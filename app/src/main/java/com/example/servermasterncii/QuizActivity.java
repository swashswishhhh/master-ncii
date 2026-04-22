package com.example.servermasterncii;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityQuizBinding;
import com.example.servermasterncii.db.AppDatabase;
import com.example.servermasterncii.model.Question;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    public static final String EXTRA_LEARNING_OUTCOME = "extra_learning_outcome";

    private static final long TIMER_DURATION_MS = 30_000L;
    private static final long TIMER_INTERVAL_MS = 1_000L;
    private static final long RESULT_DELAY_MS   = 2_000L;

    // ---------- Integrity System ----------
    private static final int MAX_INTEGRITY      = 100;
    private static final int INTEGRITY_PENALTY  = 20;
    private int currentIntegrity                = MAX_INTEGRITY;

    // ---------- UI ----------
    private ActivityQuizBinding binding;
    private MaterialButton[] optionButtons;

    // ---------- Data ----------
    private List<Question> questionList;
    private int currentIndex = 0;
    private int score        = 0;
    private String learningOutcome;

    // ---------- Mistake tracking ----------
    private final ArrayList<Integer> missedQuestionIds = new ArrayList<>();
    private final ArrayList<Integer> missedUserAnswers  = new ArrayList<>();

    // ---------- Timer ----------
    private CountDownTimer countDownTimer;
    private final Handler autoAdvanceHandler = new Handler(Looper.getMainLooper());

    // ---------- State ----------
    private boolean answered = false;

    // =====================================================================
    // ActivityResultLaunchers  (must be fields — registered before onStart)
    // =====================================================================

    /**
     * Used by the GameRouter for mid-quiz interactive questions
     * (e.g. an interactive_cmd or interactive_ui inside the question list).
     */
    private final ActivityResultLauncher<Intent> interactiveLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        boolean completed = result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getBooleanExtra(
                                "extra_task_completed", false);
                        onInteractiveResult(completed);
                    });

    /**
     * Used after the quiz is finished for levels 1.3 / 1.4 / 1.5.
     * Launches the simulator as a gate; when it returns, proceeds to
     * ResultActivity with the combined score.
     */
    private final ActivityResultLauncher<Intent> simulatorForResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        boolean simCompleted = result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getBooleanExtra(
                                "extra_task_completed", false);

                        // Simulator counts as 1 bonus question
                        int finalScore = simCompleted ? score + 1 : score;
                        int finalTotal = (questionList != null)
                                ? questionList.size() + 1 : 1;

                        launchResultActivity(finalScore, finalTotal);
                    });

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate()
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.quizRoot, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        optionButtons = new MaterialButton[]{
                binding.btnOptionA,
                binding.btnOptionB,
                binding.btnOptionC,
                binding.btnOptionD
        };

        learningOutcome = getIntent().getStringExtra(EXTRA_LEARNING_OUTCOME);
        if (learningOutcome == null || learningOutcome.isEmpty()) {
            learningOutcome = "1.1";
        }

        binding.btnOptionA.setOnClickListener(v -> checkAnswer(1));
        binding.btnOptionB.setOnClickListener(v -> checkAnswer(2));
        binding.btnOptionC.setOnClickListener(v -> checkAnswer(3));
        binding.btnOptionD.setOnClickListener(v -> checkAnswer(4));

        binding.btnSkip.setOnClickListener(v -> {
            if (!answered) {
                answered = true;
                cancelTimer();
                Question skipped = questionList.get(currentIndex);
                missedQuestionIds.add(skipped.getId());
                missedUserAnswers.add(0);

                // Skipping costs integrity too
                currentIntegrity = Math.max(0, currentIntegrity - INTEGRITY_PENALTY);
                updateIntegrityBar();

                moveToNextQuestion();
            }
        });

        loadQuestions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        autoAdvanceHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // =====================================================================
    // Data loading
    // =====================================================================

    private void loadQuestions() {
        // If learningOutcome is already "mission_1_1" format, use it directly.
        // Otherwise convert "1.1" → "mission_1_1"
        String missionId = learningOutcome.startsWith("mission_")
                ? learningOutcome
                : "mission_" + learningOutcome.replace(".", "_");

        // Extract chapter number from missionId → "mission_1_1" → "chapter_1"
        String chapterNum = missionId.replace("mission_", "").split("_")[0];
        String chapterId = "chapter_" + chapterNum;

        QuestionLoader.loadForMission(getApplicationContext(), chapterId, missionId,
                new QuestionLoader.OnQuestionsLoadedListener() {
                    @Override
                    public void onLoaded(List<QuestionLoader.Question> questions) {
                        runOnUiThread(() -> {
                            if (questions.isEmpty()) {
                                showNoQuestionsDialog();
                                return;
                            }
                            questionList = convertQuestions(questions);
                            currentIntegrity = MAX_INTEGRITY;
                            updateIntegrityBar();
                            routeCurrentQuestion();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> showNoQuestionsDialog());
                    }
                });
    }

    private List<Question> convertQuestions(List<QuestionLoader.Question> raw) {
        List<Question> result = new ArrayList<>();
        int idCounter = 1;

        for (QuestionLoader.Question q : raw) {
            if (q.choices == null || q.choices.size() < 2) continue;

            Question mapped = new Question();
            mapped.setId(idCounter++);
            mapped.setQuestionText(q.questionText);
            mapped.setType(Question.TYPE_STATIC);
            mapped.setLearningOutcome(learningOutcome);
            mapped.setExplanation(q.explanation);

            mapped.setOptionA(q.choices.size() > 0 ? q.choices.get(0) : "");
            mapped.setOptionB(q.choices.size() > 1 ? q.choices.get(1) : "");
            mapped.setOptionC(q.choices.size() > 2 ? q.choices.get(2) : "");
            mapped.setOptionD(q.choices.size() > 3 ? q.choices.get(3) : "");

            // Match correctAnswer string → correctOption (1-based)
            int correctOption = 1;
            for (int i = 0; i < q.choices.size(); i++) {
                if (q.choices.get(i).equals(q.correctAnswer)) {
                    correctOption = i + 1;
                    break;
                }
            }
            mapped.setCorrectOption(correctOption);

            result.add(mapped);
        }

        return result;
    }

    // =====================================================================
    // GameRouter
    // =====================================================================

    private void routeCurrentQuestion() {
        if (questionList == null || currentIndex >= questionList.size()) {
            finishQuiz();   // ← replaces old showFinalScoreDialog()
            return;
        }

        Question q    = questionList.get(currentIndex);
        String type   = q.getType() != null ? q.getType() : Question.TYPE_STATIC;

        Log.d(TAG, String.format("GameRouter → index=%d, type=%s, category=%s",
                currentIndex, type, q.getCategory()));

        switch (type) {
            case Question.TYPE_INTERACTIVE_CMD:
                launchInteractiveCmd();
                break;
            case Question.TYPE_INTERACTIVE_UI:
                launchInteractiveUi(q);
                break;
            case Question.TYPE_STATIC:
            case Question.TYPE_TRUE_FALSE:
            default:
                displayQuestion();
                break;
        }
    }

    // =====================================================================
    // GameRouter — interactive launchers (mid-quiz)
    // =====================================================================

    private void launchInteractiveCmd() {
        interactiveLauncher.launch(
                new Intent(this, TerminalEmulatorActivity.class));
    }

    private void launchInteractiveUi(Question q) {
        String category = q.getCategory() != null ? q.getCategory() : "";
        Class<?> target;

        switch (category) {
            case Question.CATEGORY_FIREWALL:
                target = FirewallSimulatorActivity.class;
                break;
            case Question.CATEGORY_IP_CONFIG:
                target = IPConfigSimulatorActivity.class;
                break;
            case Question.CATEGORY_WORKGROUP:
                target = WorkgroupConfigActivity.class;
                break;
            default:
                Log.w(TAG, "Unknown interactive_ui category: " + category
                        + " — falling back to static display.");
                displayQuestion();
                return;
        }

        interactiveLauncher.launch(new Intent(this, target));
    }

    private void onInteractiveResult(boolean completed) {
        if (questionList == null || currentIndex >= questionList.size()) return;

        Question q = questionList.get(currentIndex);
        if (completed) {
            score++;
            Log.d(TAG, "Interactive mission COMPLETED — score now " + score);
        } else {
            missedQuestionIds.add(q.getId());
            missedUserAnswers.add(0);

            // Failed interactive = integrity hit
            currentIntegrity = Math.max(0, currentIntegrity - INTEGRITY_PENALTY);
            updateIntegrityBar();

            Log.d(TAG, "Interactive mission SKIPPED/CANCELLED");
        }

        moveToNextQuestion();
    }

    // =====================================================================
    // Quiz completion — finishQuiz() replaces showFinalScoreDialog()
    // =====================================================================

    /**
     * Called when all questions are exhausted.
     *
     * Flow:
     *   Score >= 70% AND level has a simulator gate
     *       → launch simulator via simulatorForResultLauncher
     *       → simulator result callback → ResultActivity
     *   Otherwise
     *       → ResultActivity directly
     */
    private void finishQuiz() {
        double pct    = (questionList == null || questionList.isEmpty())
                ? 0 : score / (double) questionList.size();
        boolean passed = pct >= 0.70;

        Class<?> simulatorClass = getSimulatorForLevel(learningOutcome);

        if (passed && simulatorClass != null) {
            // Gate: student must also complete the simulator
            Intent simIntent = new Intent(this, simulatorClass);
            // Pass quiz context so simulator knows where it came from
            simIntent.putExtra(EXTRA_LEARNING_OUTCOME, learningOutcome);
            simulatorForResultLauncher.launch(simIntent);
        } else {
            // Go straight to results (failed quiz or non-gated level)
            int total = (questionList != null) ? questionList.size() : 0;
            launchResultActivity(score, total);
        }
    }

    /**
     * Maps a levelId to its post-quiz simulator gate class.
     * Returns null for levels that have no simulator gate.
     */
    private Class<?> getSimulatorForLevel(String levelId) {
        switch (levelId) {
            case "1.3": return FirewallSimulatorActivity.class;
            case "1.4": return IPConfigSimulatorActivity.class;
            case "1.5": return TerminalEmulatorActivity.class;
            default:    return null;
        }
    }

    /**
     * Builds the ResultActivity Intent and launches it, then finishes
     * QuizActivity so the back stack is clean.
     */
    private void launchResultActivity(int finalScore, int finalTotal) {
        int[] missedIdsArray   = new int[missedQuestionIds.size()];
        int[] userAnswersArray = new int[missedUserAnswers.size()];

        // Full question data for the review section
        ArrayList<String>  missedTexts            = new ArrayList<>();
        ArrayList<String>  missedOptA             = new ArrayList<>();
        ArrayList<String>  missedOptB             = new ArrayList<>();
        ArrayList<String>  missedOptC             = new ArrayList<>();
        ArrayList<String>  missedOptD             = new ArrayList<>();
        ArrayList<Integer> missedCorrect           = new ArrayList<>();
        ArrayList<Integer> missedUserAnswerOptions = new ArrayList<>();

        for (int i = 0; i < missedQuestionIds.size(); i++) {
            int qId = missedQuestionIds.get(i);
            missedIdsArray[i]   = qId;
            userAnswersArray[i] = missedUserAnswers.get(i);

            // Find the full Question object by id
            if (questionList != null) {
                for (Question q : questionList) {
                    if (q.getId() == qId) {
                        missedTexts.add(q.getQuestionText() != null ? q.getQuestionText() : "");
                        missedOptA.add(q.getOptionA() != null ? q.getOptionA() : "");
                        missedOptB.add(q.getOptionB() != null ? q.getOptionB() : "");
                        missedOptC.add(q.getOptionC() != null ? q.getOptionC() : "");
                        missedOptD.add(q.getOptionD() != null ? q.getOptionD() : "");
                        missedCorrect.add(q.getCorrectOption());
                        missedUserAnswerOptions.add(missedUserAnswers.get(i));
                        break;
                    }
                }
            }
        }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_SCORE,               finalScore);
        intent.putExtra(ResultActivity.EXTRA_TOTAL,               finalTotal);
        intent.putExtra(ResultActivity.EXTRA_LEARNING_OUTCOME,    learningOutcome);
        intent.putExtra(ResultActivity.EXTRA_MISSED_QUESTION_IDS, missedIdsArray);
        intent.putExtra(ResultActivity.EXTRA_USER_ANSWERS,        userAnswersArray);

        // Full question data for review mistakes section
        intent.putStringArrayListExtra("missed_texts",             missedTexts);
        intent.putStringArrayListExtra("missed_opt_a",             missedOptA);
        intent.putStringArrayListExtra("missed_opt_b",             missedOptB);
        intent.putStringArrayListExtra("missed_opt_c",             missedOptC);
        intent.putStringArrayListExtra("missed_opt_d",             missedOptD);
        intent.putIntegerArrayListExtra("missed_correct",          missedCorrect);
        intent.putIntegerArrayListExtra("missed_user_answer_options", missedUserAnswerOptions);

        startActivity(intent);
        finish();
    }

    // =====================================================================
    // Question display
    // =====================================================================

    private void displayQuestion() {
        if (questionList == null || currentIndex >= questionList.size()) return;

        answered = false;
        Question q = questionList.get(currentIndex);

        // Reset integrity only on the very first question
        if (currentIndex == 0) {
            currentIntegrity = MAX_INTEGRITY;
            updateIntegrityBar();
        }

        binding.tvLearningOutcomeLabel.setText(
                String.format(Locale.getDefault(),
                        "LEARNING OUTCOME: %s", learningOutcome));

        int progressPercent =
                (int) ((currentIndex / (float) questionList.size()) * 100);
        binding.progressQuiz.setProgress(progressPercent, true);

        binding.tvQuestionNumber.setText(
                String.format(Locale.getDefault(),
                        "Q %d of %d", currentIndex + 1, questionList.size()));

        binding.tvQuestionChip.setText(
                String.format(Locale.getDefault(),
                        "QUESTION %d OF %d", currentIndex + 1, questionList.size()));

        binding.tvQuestionText.setText(q.getQuestionText());

        updateScoreLabel();

        boolean isTrueFalse = Question.TYPE_TRUE_FALSE.equals(q.getType());

        for (MaterialButton btn : optionButtons) {
            btn.setEnabled(true);
            resetButtonStyle(btn);
        }

        binding.btnOptionA.setText(String.format("A.  %s", q.getOptionA()));
        binding.btnOptionB.setText(String.format("B.  %s", q.getOptionB()));

        if (isTrueFalse) {
            binding.btnOptionC.setVisibility(View.GONE);
            binding.btnOptionD.setVisibility(View.GONE);
        } else {
            binding.btnOptionC.setVisibility(View.VISIBLE);
            binding.btnOptionD.setVisibility(View.VISIBLE);
            binding.btnOptionC.setText(String.format("C.  %s", q.getOptionC()));
            binding.btnOptionD.setText(String.format("D.  %s", q.getOptionD()));
        }

        binding.gridChoices.setVisibility(View.VISIBLE);
        binding.tvTimer.setVisibility(View.VISIBLE);
        binding.cardQuestion.setVisibility(View.VISIBLE);
        binding.btnSkip.setVisibility(View.VISIBLE);

        startTimer();
    }

    // =====================================================================
    // Answer checking
    // =====================================================================

    public void checkAnswer(int selectedOption) {
        if (answered) return;
        answered = true;

        cancelTimer();
        binding.btnSkip.setVisibility(View.INVISIBLE);

        Question q         = questionList.get(currentIndex);
        int correctOpt     = q.getCorrectOption();

        for (MaterialButton btn : optionButtons) btn.setEnabled(false);

        MaterialButton selectedButton = optionButtons[selectedOption - 1];
        MaterialButton correctButton  = optionButtons[correctOpt  - 1];

        if (selectedOption == correctOpt) {
            score++;
            setButtonColor(selectedButton, R.color.quiz_correct);
        } else {
            setButtonColor(selectedButton, R.color.quiz_wrong);
            setButtonColor(correctButton,  R.color.quiz_correct);

            missedQuestionIds.add(q.getId());
            missedUserAnswers.add(selectedOption);

            // Degrade system integrity on wrong answer
            currentIntegrity = Math.max(0, currentIntegrity - INTEGRITY_PENALTY);
            updateIntegrityBar();
        }

        updateScoreLabel();
        autoAdvanceHandler.postDelayed(this::moveToNextQuestion, RESULT_DELAY_MS);
    }

    // =====================================================================
    // Navigation
    // =====================================================================

    private void moveToNextQuestion() {
        currentIndex++;
        routeCurrentQuestion();
    }

    // =====================================================================
    // Timer
    // =====================================================================

    private void startTimer() {
        cancelTimer();
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, TIMER_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                binding.tvTimer.setText(
                        String.format(Locale.getDefault(),
                                "%02d:%02d", seconds / 60, seconds % 60));

                if (seconds <= 10) {
                    binding.tvTimer.setTextColor(
                            ContextCompat.getColor(QuizActivity.this, R.color.quiz_wrong));
                } else {
                    binding.tvTimer.setTextColor(
                            ContextCompat.getColor(QuizActivity.this,
                                    R.color.cyber_neon_green_bright));
                }
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("00:00");
                onTimeUp();
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void onTimeUp() {
        if (answered) return;
        answered = true;
        binding.btnSkip.setVisibility(View.INVISIBLE);

        for (MaterialButton btn : optionButtons) btn.setEnabled(false);

        Question q = questionList.get(currentIndex);
        setButtonColor(optionButtons[q.getCorrectOption() - 1], R.color.quiz_correct);

        missedQuestionIds.add(q.getId());
        missedUserAnswers.add(0);

        // Time-out also costs integrity
        currentIntegrity = Math.max(0, currentIntegrity - INTEGRITY_PENALTY);
        updateIntegrityBar();

        autoAdvanceHandler.postDelayed(this::moveToNextQuestion, RESULT_DELAY_MS);
    }

    // =====================================================================
    // Integrity bar
    // =====================================================================

    /**
     * Updates the INTEGRITY progress bar and status label.
     *
     * 100–80  → OPTIMAL  (neon green)
     * 79–50   → STABLE   (green)
     * 49–20   → DEGRADED (amber)
     * 19–0    → CRITICAL (red)
     */
    private void updateIntegrityBar() {
        binding.progressIntegrity.setProgress(currentIntegrity, true);

        final String status;
        final int color;

        if (currentIntegrity >= 80) {
            status = "OPTIMAL";
            color  = ContextCompat.getColor(this, R.color.cyber_neon_green_bright);
        } else if (currentIntegrity >= 50) {
            status = "STABLE";
            color  = ContextCompat.getColor(this, R.color.cyber_neon_green);
        } else if (currentIntegrity >= 20) {
            status = "DEGRADED";
            color  = android.graphics.Color.parseColor("#FFA500");
        } else {
            status = "CRITICAL";
            color  = ContextCompat.getColor(this, R.color.quiz_wrong);
        }

        binding.tvIntegrityStatus.setText(status);
        binding.tvIntegrityStatus.setTextColor(color);
        binding.progressIntegrity.setIndicatorColor(color);
    }

    // =====================================================================
    // UI helpers
    // =====================================================================

    private void updateScoreLabel() {
        binding.tvScore.setText(
                String.format(Locale.getDefault(),
                        "Score: %d / %d", score, questionList.size()));
    }

    private void setButtonColor(MaterialButton button, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void resetButtonStyle(MaterialButton button) {
        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#0F2A1A")));
        button.setTextColor(
                ContextCompat.getColor(this, android.R.color.white));
        button.setStrokeColor(
                ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.cyber_neon_green)));
        button.setStrokeWidth(3);
    }

    // =====================================================================
    // No questions fallback
    // =====================================================================

    private void showNoQuestionsDialog() {
        com.google.android.material.snackbar.Snackbar
                .make(binding.getRoot(),
                        "Mission Data Not Found — Syncing...",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .show();
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2500);
    }
}