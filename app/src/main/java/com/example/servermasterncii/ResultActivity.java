package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityResultBinding;
import com.example.servermasterncii.databinding.ItemMissedQuestionBinding;
import com.example.servermasterncii.db.AppDatabase;
import com.example.servermasterncii.db.QuestionDao;
import com.example.servermasterncii.model.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultActivity displays the final quiz score, a competency status badge,
 * and an expandable "Review Mistakes" section that shows every question the
 * user answered incorrectly together with the correct answer.
 *
 * <h3>Intent Extras</h3>
 * <ul>
 *     <li>{@link #EXTRA_SCORE}              — int, number of correct answers</li>
 *     <li>{@link #EXTRA_TOTAL}              — int, total number of questions</li>
 *     <li>{@link #EXTRA_LEARNING_OUTCOME}   — String, LO identifier</li>
 *     <li>{@link #EXTRA_MISSED_QUESTION_IDS} — int[], Room IDs of missed questions</li>
 *     <li>{@link #EXTRA_USER_ANSWERS}        — int[], user's selected options for missed questions (parallel to IDs)</li>
 * </ul>
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_LEARNING_OUTCOME = "extra_learning_outcome";
    public static final String EXTRA_MISSED_QUESTION_IDS = "extra_missed_question_ids";
    public static final String EXTRA_USER_ANSWERS = "extra_user_answers";

    /** Must match the prefs name used by MainActivity for level progress. */
    private static final String PREFS_NAME = "server_master_prefs";

    private ActivityResultBinding binding;

    private int score;
    private int total;
    private String learningOutcome;
    private int[] missedIds;
    private int[] userAnswers;

    private boolean mistakesVisible = false;
    private boolean mistakesLoaded = false;

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Unpack intent extras
        Intent intent = getIntent();
        score = intent.getIntExtra(EXTRA_SCORE, 0);
        total = intent.getIntExtra(EXTRA_TOTAL, 0);
        learningOutcome = intent.getStringExtra(EXTRA_LEARNING_OUTCOME);
        missedIds = intent.getIntArrayExtra(EXTRA_MISSED_QUESTION_IDS);
        userAnswers = intent.getIntArrayExtra(EXTRA_USER_ANSWERS);

        if (learningOutcome == null) learningOutcome = "";

        // ── Persist score to SharedPreferences for the Level Selection Hub ──
        saveScoreToPrefs();

        populateScoreCard();
        populateCompetencyBadge();
        setupButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // =====================================================================
    // Score persistence — Level Selection Hub integration
    // =====================================================================

    /**
     * Saves the quiz result to SharedPreferences so the Level Selection Hub
     * can display progress and unlock subsequent levels.
     * <p>
     * Only updates if the new score is better than the previously saved best.
     * If the score exceeds 70% of total, the next level is auto-unlocked.
     */
    private void saveScoreToPrefs() {
        if (learningOutcome.isEmpty() || total == 0) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int previousBest = prefs.getInt("score_" + learningOutcome, 0);

        // Only save if this attempt is the new personal best
        if (score > previousBest) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("score_" + learningOutcome, score);
            editor.putInt("total_" + learningOutcome, total);

            // Auto-unlock the next level if score ≥ 70%
            double pct = score / (double) total;
            if (pct >= 0.70) {
                String nextLevel = getNextLevelId(learningOutcome);
                if (nextLevel != null) {
                    editor.putBoolean("unlocked_" + nextLevel, true);
                }
            }

            editor.apply();
        }
    }

    /**
     * Maps a learning outcome to the next one in sequence.
     * Covers all 21 levels across 3 chapters, including cross-chapter gateways.
     */
    private String getNextLevelId(String currentLevelId) {
        switch (currentLevelId) {
            // Chapter 1
            case "1.1":  return "1.2";
            case "1.2":  return "1.3";
            case "1.3":  return "1.4";
            case "1.4":  return "1.5";
            case "1.5":  return "2.1";   // cross-chapter gate

            // Chapter 2
            case "2.1":  return "2.2";
            case "2.2":  return "2.3";
            case "2.3":  return "2.4";
            case "2.4":  return "2.5";
            case "2.5":  return "2.6";
            case "2.6":  return "2.7";
            case "2.7":  return "2.8";
            case "2.8":  return "2.9";
            case "2.9":  return "2.10";
            case "2.10": return "2.11";
            case "2.11": return "2.12";
            case "2.12": return "3.1";   // cross-chapter gate

            // Chapter 3
            case "3.1":  return "3.2";
            case "3.2":  return "3.3";
            case "3.3":  return "3.4";
            case "3.4":  return null;    // final level

            default:     return null;
        }
    }

    // =====================================================================
    // Score card
    // =====================================================================

    private void populateScoreCard() {
        binding.tvScoreValue.setText(
                String.format(Locale.getDefault(), "%d / %d", score, total));

        int percent = total > 0 ? Math.round((score / (float) total) * 100) : 0;
        binding.tvPercentage.setText(
                String.format(Locale.getDefault(), "%d%%", percent));
        binding.progressResult.setProgress(percent, true);

        binding.tvLearningOutcome.setText(
                String.format(Locale.getDefault(), "Learning Outcome: %s", learningOutcome));
    }

    // =====================================================================
    // Competency status
    // =====================================================================

    /**
     * Determines the competency status:
     * <ul>
     *     <li><b>100 %</b> → "COMPETENT" / "NYC Ready" (green badge)</li>
     *     <li><b>&lt; 100 %</b> → "NOT YET COMPETENT" (amber badge)</li>
     * </ul>
     */
    private void populateCompetencyBadge() {
        int percent = total > 0 ? Math.round((score / (float) total) * 100) : 0;

        if (percent == 100) {
            // Perfect score — Competent / NYC Ready
            binding.tvStatusIcon.setText("✅");
            binding.tvCompetencyStatus.setText("COMPETENT");
            binding.tvStatusDescription.setText(
                    "NYC (National Youth Commission) Ready!\nYou have demonstrated full competency in this module.");

            binding.tvCompetencyStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_competent));
            binding.cardCompetencyStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.status_competent_bg));

            // Hide the review button — nothing to review
            binding.btnReviewMistakes.setVisibility(View.GONE);
        } else {
            // Below perfect — Not Yet Competent
            binding.tvStatusIcon.setText("📋");
            binding.tvCompetencyStatus.setText("NOT YET COMPETENT");
            binding.tvStatusDescription.setText(
                    String.format(Locale.getDefault(),
                            "You need 100%% to achieve Competent status.\nReview the questions you missed and try again!"));

            binding.tvCompetencyStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_nyc));
            binding.cardCompetencyStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.status_nyc_bg));
        }
    }

    // =====================================================================
    // Button wiring
    // =====================================================================

    private void setupButtons() {
        // Review Mistakes — toggle visibility
        binding.btnReviewMistakes.setOnClickListener(v -> toggleMistakesSection());

        // Retry — relaunch QuizActivity with the same LO
        binding.btnRetry.setOnClickListener(v -> {
            Intent quizIntent = new Intent(this, QuizActivity.class);
            quizIntent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, learningOutcome);
            startActivity(quizIntent);
            finish();
        });

        // Finish — pop back to home
        binding.btnFinish.setOnClickListener(v -> finish());

        // ── Mission / Review Theory visibility based on score ──
        int percent = total > 0 ? Math.round((score / (float) total) * 100) : 0;

        if (percent >= 70) {
            // Score qualifies — show Mission button (only for levels that have one)
            Intent missionIntent = getMissionIntent(learningOutcome);

            if (missionIntent != null) {
                binding.btnMission.setVisibility(View.VISIBLE);
                binding.btnMission.setOnClickListener(v -> {
                    startActivity(missionIntent);
                    finish();
                });
            }
            // If no mission is mapped for this level, both buttons stay hidden
        } else {
            // Score below 70 % — hide Mission, show Review Theory
            binding.btnMission.setVisibility(View.GONE);
            binding.btnReviewTheory.setVisibility(View.VISIBLE);

            binding.btnReviewTheory.setOnClickListener(v -> {
                // TODO: launch your TheoryActivity / review screen here
                // For now, re-open the same quiz so the user can study
                Intent retryIntent = new Intent(this, QuizActivity.class);
                retryIntent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, learningOutcome);
                startActivity(retryIntent);
                finish();
            });
        }
    }

    // =====================================================================
    // Mission routing — maps levelId → interactive Activity
    // =====================================================================

    /**
     * Returns an Intent for the interactive "Mission" activity that matches
     * the given level ID, or {@code null} if no mission is assigned.
     *
     * @param levelId the current learning-outcome / level identifier
     * @return a configured Intent, or null
     */
    private Intent getMissionIntent(String levelId) {
        if (levelId == null) return null;

        switch (levelId) {
            case "1.5":
                return new Intent(this, TerminalEmulatorActivity.class);

            case "1.3":
                return new Intent(this, FirewallSimulatorActivity.class);

            // ── Add future mission mappings here ──
            // case "2.1":
            //     return new Intent(this, IpConfigActivity.class);
            // case "2.5":
            //     return new Intent(this, WorkgroupActivity.class);

            default:
                return null;   // no interactive mission for this level
        }
    }

    // =====================================================================
    // Review Mistakes
    // =====================================================================

    /**
     * Toggles the review-mistakes section. The first time it is expanded,
     * questions are fetched from Room on a background thread and inflated
     * into the container.
     */
    private void toggleMistakesSection() {
        mistakesVisible = !mistakesVisible;

        int visibility = mistakesVisible ? View.VISIBLE : View.GONE;
        binding.tvReviewHeader.setVisibility(visibility);
        binding.containerMistakes.setVisibility(visibility);

        binding.btnReviewMistakes.setText(
                mistakesVisible ? "Hide Mistakes" : "📝  Review Mistakes");

        // Lazy-load mistake cards only once
        if (mistakesVisible && !mistakesLoaded) {
            loadMistakeCards();
        }
    }

    /**
     * Fetches the missed questions from Room by their IDs, then inflates an
     * {@code item_missed_question} card for each one.
     */
    private void loadMistakeCards() {
        if (missedIds == null || missedIds.length == 0) {
            binding.tvReviewHeader.setText("No mistakes — well done!");
            mistakesLoaded = true;
            return;
        }

        // Convert int[] → List<Integer> for the DAO
        List<Integer> idList = new ArrayList<>(missedIds.length);
        for (int id : missedIds) {
            idList.add(id);
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            QuestionDao dao = AppDatabase.getDatabase(getApplicationContext()).questionDao();
            List<Question> missed = dao.getQuestionsByIds(idList);

            runOnUiThread(() -> {
                LayoutInflater inflater = getLayoutInflater();

                for (int i = 0; i < missed.size(); i++) {
                    Question q = missed.get(i);

                    // Find the parallel user-answer for this question ID
                    int userAnswer = findUserAnswer(q.getId());

                    ItemMissedQuestionBinding card =
                            ItemMissedQuestionBinding.inflate(inflater, binding.containerMistakes, false);

                    // Question header & text
                    card.tvMissedQuestionNumber.setText(
                            String.format(Locale.getDefault(), "Question %d", i + 1));
                    card.tvMissedQuestionText.setText(q.getQuestionText());

                    // User's (wrong) answer
                    card.tvYourAnswer.setText(formatOption(userAnswer, q));

                    // Correct answer
                    card.tvCorrectAnswer.setText(formatOption(q.getCorrectOption(), q));

                    binding.containerMistakes.addView(card.getRoot());
                }

                mistakesLoaded = true;
            });
        });
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Looks up the user's selected option for a given question ID from the
     * parallel arrays passed via Intent.
     *
     * @param questionId the Room ID of the question
     * @return the user's selected option (1–4), or 0 if not found (e.g. timed out)
     */
    private int findUserAnswer(int questionId) {
        if (missedIds == null || userAnswers == null) return 0;
        for (int i = 0; i < missedIds.length; i++) {
            if (missedIds[i] == questionId) {
                return userAnswers[i];
            }
        }
        return 0;
    }

    /**
     * Returns a human-readable label for the given option number.
     *
     * @param option 1 = A, 2 = B, 3 = C, 4 = D, 0 = unanswered
     * @param q      the Question to pull option text from
     */
    private String formatOption(int option, Question q) {
        switch (option) {
            case 1:  return "A.  " + q.getOptionA();
            case 2:  return "B.  " + q.getOptionB();
            case 3:  return "C.  " + q.getOptionC();
            case 4:  return "D.  " + q.getOptionD();
            default: return "—  (Not answered)";
        }
    }
}
