package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE                = "extra_score";
    public static final String EXTRA_TOTAL                = "extra_total";
    public static final String EXTRA_LEARNING_OUTCOME     = "extra_learning_outcome";
    public static final String EXTRA_MISSED_QUESTION_IDS  = "extra_missed_question_ids";
    public static final String EXTRA_USER_ANSWERS         = "extra_user_answers";

    private static final String PREFS_NAME = "server_master_prefs";

    private ActivityResultBinding binding;
    private SharedPreferences prefs;

    private int score;
    private int total;
    private String learningOutcome;
    private int[] missedIds;
    private int[] userAnswers;

    private ArrayList<String>  missedTexts;
    private ArrayList<String>  missedOptA;
    private ArrayList<String>  missedOptB;
    private ArrayList<String>  missedOptC;
    private ArrayList<String>  missedOptD;
    private ArrayList<Integer> missedCorrect;
    private ArrayList<Integer> missedUserAnswerOptions;

    private boolean mistakesVisible = false;
    private boolean mistakesLoaded  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Intent intent = getIntent();
        score           = intent.getIntExtra(EXTRA_SCORE, 0);
        total           = intent.getIntExtra(EXTRA_TOTAL, 0);
        learningOutcome = intent.getStringExtra(EXTRA_LEARNING_OUTCOME);
        missedIds       = intent.getIntArrayExtra(EXTRA_MISSED_QUESTION_IDS);
        userAnswers     = intent.getIntArrayExtra(EXTRA_USER_ANSWERS);

        missedTexts             = intent.getStringArrayListExtra("missed_texts");
        missedOptA              = intent.getStringArrayListExtra("missed_opt_a");
        missedOptB              = intent.getStringArrayListExtra("missed_opt_b");
        missedOptC              = intent.getStringArrayListExtra("missed_opt_c");
        missedOptD              = intent.getStringArrayListExtra("missed_opt_d");
        missedCorrect           = intent.getIntegerArrayListExtra("missed_correct");
        missedUserAnswerOptions = intent.getIntegerArrayListExtra("missed_user_answer_options");

        if (learningOutcome == null) learningOutcome = "";

        saveProgress();
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
    // Progress — delegates to ProgressManager (local + cloud)
    // =====================================================================

    private void saveProgress() {
        if (learningOutcome.isEmpty() || total == 0) return;
        ProgressManager.saveProgress(
                prefs,
                learningOutcome,
                score,
                total,
                true,
                getNextLevelId(learningOutcome)
        );
    }

    private String getNextLevelId(String id) {
        if (id.startsWith("mission_")) return null;
        switch (id) {
            case "1.1":  return "1.2";
            case "1.2":  return "1.3";
            case "1.3":  return "1.4";
            case "1.4":  return "1.5";
            case "1.5":  return "2.1";
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
            case "2.12": return "3.1";
            case "3.1":  return "3.2";
            case "3.2":  return "3.3";
            case "3.3":  return "3.4";
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
        binding.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
        binding.progressResult.setProgress(percent, true);
        binding.tvLearningOutcome.setText(
                String.format(Locale.getDefault(), "Learning Outcome: %s", learningOutcome));
    }

    // =====================================================================
    // Competency badge
    // =====================================================================

    private void populateCompetencyBadge() {
        int percent = total > 0 ? Math.round((score / (float) total) * 100) : 0;
        if (percent == 100) {
            binding.tvStatusIcon.setText("✅");
            binding.tvCompetencyStatus.setText("COMPETENT");
            binding.tvStatusDescription.setText(
                    "NYC (National Youth Commission) Ready!\nYou have demonstrated full competency in this module.");
            binding.tvCompetencyStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_competent));
            binding.cardCompetencyStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.status_competent_bg));
            binding.btnReviewMistakes.setVisibility(View.GONE);
        } else {
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
    // Buttons
    // =====================================================================

    private void setupButtons() {
        binding.btnReviewMistakes.setOnClickListener(v -> toggleMistakesSection());
        binding.btnRetry.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class)
                    .putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, learningOutcome));
            finish();
        });
        binding.btnFinish.setOnClickListener(v -> finish());

        int percent = total > 0 ? Math.round((score / (float) total) * 100) : 0;
        if (percent >= 70) {
            Intent missionIntent = getMissionIntent(learningOutcome);
            if (missionIntent != null) {
                binding.btnMission.setVisibility(View.VISIBLE);
                binding.btnMission.setOnClickListener(v -> { startActivity(missionIntent); finish(); });
            }
        } else {
            binding.btnMission.setVisibility(View.GONE);
            binding.btnReviewTheory.setVisibility(View.VISIBLE);
            binding.btnReviewTheory.setOnClickListener(v -> {
                startActivity(new Intent(this, QuizActivity.class)
                        .putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, learningOutcome));
                finish();
            });
        }
    }

    private Intent getMissionIntent(String levelId) {
        if (levelId == null) return null;
        switch (levelId) {
            case "1.5": return new Intent(this, TerminalEmulatorActivity.class);
            case "1.3": return new Intent(this, FirewallSimulatorActivity.class);
            default:    return null;
        }
    }

    // =====================================================================
    // Review Mistakes
    // =====================================================================

    private void toggleMistakesSection() {
        mistakesVisible = !mistakesVisible;
        int visibility = mistakesVisible ? View.VISIBLE : View.GONE;
        binding.tvReviewHeader.setVisibility(visibility);
        binding.containerMistakes.setVisibility(visibility);
        binding.btnReviewMistakes.setText(mistakesVisible ? "Hide Mistakes" : "📝  Review Mistakes");
        if (mistakesVisible && !mistakesLoaded) loadMistakeCards();
    }

    private void loadMistakeCards() {
        if (missedTexts == null || missedTexts.isEmpty()) {
            binding.tvReviewHeader.setText("No mistakes — well done!");
            mistakesLoaded = true;
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        binding.containerMistakes.removeAllViews();
        for (int i = 0; i < missedTexts.size(); i++) {
            ItemMissedQuestionBinding card =
                    ItemMissedQuestionBinding.inflate(inflater, binding.containerMistakes, false);
            card.tvMissedQuestionNumber.setText(
                    String.format(Locale.getDefault(), "Question %d", i + 1));
            card.tvMissedQuestionText.setText(missedTexts.get(i));
            int userOpt = (missedUserAnswerOptions != null && i < missedUserAnswerOptions.size())
                    ? missedUserAnswerOptions.get(i) : 0;
            card.tvYourAnswer.setText(formatOption(userOpt, i));
            int correctOpt = (missedCorrect != null && i < missedCorrect.size())
                    ? missedCorrect.get(i) : 1;
            card.tvCorrectAnswer.setText(formatOption(correctOpt, i));
            binding.containerMistakes.addView(card.getRoot());
        }
        mistakesLoaded = true;
    }

    private String formatOption(int option, int index) {
        String text;
        switch (option) {
            case 1:
                text = (missedOptA != null && index < missedOptA.size()) ? missedOptA.get(index) : "—";
                return "A.  " + text;
            case 2:
                text = (missedOptB != null && index < missedOptB.size()) ? missedOptB.get(index) : "—";
                return "B.  " + text;
            case 3:
                text = (missedOptC != null && index < missedOptC.size()) ? missedOptC.get(index) : "—";
                return "C.  " + text;
            case 4:
                text = (missedOptD != null && index < missedOptD.size()) ? missedOptD.get(index) : "—";
                return "D.  " + text;
            default:
                return "—  (Not answered / Timed out)";
        }
    }
}