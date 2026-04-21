package com.example.servermasterncii;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityDecisionTreeBinding;

/**
 * DecisionTreeActivity — 3-level server decommission decision tree.
 *
 * <p>Each level presents a scenario card with two choices. Choosing correctly
 * flashes the card green and slides in the next level. Choosing wrongly shows
 * a consequence card with a RETRY LEVEL button that resets only the current
 * level. Completing all 3 levels correctly returns RESULT_OK.</p>
 *
 * <h3>Correct path</h3>
 * <ol>
 *   <li>Level 1 — Option A: Back up all data</li>
 *   <li>Level 2 — Option A: Remove from Active Directory</li>
 *   <li>Level 3 — Option A: Document and update asset records</li>
 * </ol>
 */
public class DecisionTreeActivity extends AppCompatActivity {

    // ── Intent extra ───────────────────────────────────────────────────────
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    // ── Level data ─────────────────────────────────────────────────────────

    private static final String[] SCENARIO_TEXTS = {
            "SERVER-07 is being retired.\nIt holds 500 GB of active company data.",
            "Backup complete.\nActive Directory still shows SERVER-07 as a domain member.",
            "All data migrated.\nSERVER-07 is removed from Active Directory."
    };

    private static final String[] QUESTIONS = {
            "What is your FIRST action?",
            "What do you do next?",
            "What is the FINAL step?"
    };

    private static final String[] OPTION_A = {
            "A)  Back up all data to another server",
            "A)  Remove SERVER-07 from Active Directory",
            "A)  Document the decommission process and update asset records"
    };

    private static final String[] OPTION_B = {
            "B)  Power off the server immediately",
            "B)  Skip this step — it will auto-remove",
            "B)  Immediately reformat and sell the hardware"
    };

    /** Index 0 = correct option for level 1, etc.  1 = A, 2 = B */
    private static final int[] CORRECT_OPTION = {1, 1, 1};   // all A

    private static final String[] CONSEQUENCE_TEXTS = {
            "Powering off without a backup causes permanent data loss.\n\n"
                    + "In real life this violates data retention policies and can result in "
                    + "legal liability. Always back up before decommissioning.",
            "Skipping AD removal leaves a ghost computer account.\n\n"
                    + "This can cause authentication errors, security audit failures, "
                    + "and orphaned Group Policy objects that affect other machines.",
            "Reformatting without documentation breaks the asset lifecycle.\n\n"
                    + "Auditors require a decommission record. Selling hardware without "
                    + "wiping and documenting can expose sensitive data and violate compliance."
    };

    // ── UI ─────────────────────────────────────────────────────────────────
    private ActivityDecisionTreeBinding binding;

    // ── State ──────────────────────────────────────────────────────────────
    private int currentLevel   = 1;   // 1-based
    private boolean resultDone = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate()
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDecisionTreeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnContinue.setOnClickListener(v -> returnSuccess());
        binding.btnRetry.setOnClickListener(v -> retryCurrentLevel());

        loadLevel(currentLevel, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Level loading
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Populates the scenario card for the given level and wires the option buttons.
     *
     * @param level    1-based level number
     * @param animate  true = slide in from the right
     */
    private void loadLevel(int level, boolean animate) {
        int idx = level - 1;

        // Update header
        binding.tvLevelLabel.setText("LEVEL " + level + " / 3");
        binding.tvScenarioChip.setText("SCENARIO — LEVEL " + level);
        binding.progressLevel.setProgress(level, true);

        // Update pips
        updatePips(level);

        // Populate text
        binding.tvScenarioText.setText(SCENARIO_TEXTS[idx]);
        binding.tvQuestion.setText(QUESTIONS[idx]);
        binding.btnOptionA.setText(OPTION_A[idx]);
        binding.btnOptionB.setText(OPTION_B[idx]);

        // Reset button styles
        resetOptionButton(binding.btnOptionA);
        resetOptionButton(binding.btnOptionB);
        binding.btnOptionA.setEnabled(true);
        binding.btnOptionB.setEnabled(true);

        // Show scenario, hide consequence
        showScenario(animate);

        // Wire clicks
        binding.btnOptionA.setOnClickListener(v -> onOptionChosen(1));
        binding.btnOptionB.setOnClickListener(v -> onOptionChosen(2));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Choice handling
    // ══════════════════════════════════════════════════════════════════════

    private void onOptionChosen(int chosen) {
        // Disable both buttons immediately to prevent double-tap
        binding.btnOptionA.setEnabled(false);
        binding.btnOptionB.setEnabled(false);

        int idx = currentLevel - 1;
        boolean correct = (chosen == CORRECT_OPTION[idx]);

        if (correct) {
            handleCorrect(chosen);
        } else {
            handleWrong(idx);
        }
    }

    private void handleCorrect(int chosen) {
        // Flash the chosen button green
        com.google.android.material.button.MaterialButton btn =
                (chosen == 1) ? binding.btnOptionA : binding.btnOptionB;
        flashButtonGreen(btn);

        handler.postDelayed(() -> {
            if (binding == null) return;
            if (currentLevel < 3) {
                currentLevel++;
                loadLevel(currentLevel, true);
            } else {
                showSuccessOverlay();
            }
        }, 700);
    }

    private void handleWrong(int idx) {
        // Flash the chosen button red briefly, then show consequence
        handler.postDelayed(() -> {
            if (binding == null) return;
            binding.tvConsequenceText.setText(CONSEQUENCE_TEXTS[idx]);
            showConsequence();
        }, 400);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Retry — resets only the current level
    // ══════════════════════════════════════════════════════════════════════

    private void retryCurrentLevel() {
        loadLevel(currentLevel, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    // View transitions
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Shows the scenario scroll view, hides consequence.
     * If animate=true, slides in from the right using ViewPropertyAnimator.
     */
    private void showScenario(boolean animate) {
        binding.scrollConsequence.setVisibility(View.GONE);
        binding.overlaySuccess.setVisibility(View.GONE);
        binding.scrollScenario.setVisibility(View.VISIBLE);

        if (animate) {
            float startX = binding.contentFrame.getWidth();
            if (startX == 0) startX = 800f;   // fallback before layout pass
            binding.scrollScenario.setTranslationX(startX);
            binding.scrollScenario.animate()
                    .translationX(0f)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();
        } else {
            binding.scrollScenario.setTranslationX(0f);
        }
    }

    /** Slides in the consequence card from the right. */
    private void showConsequence() {
        binding.scrollScenario.setVisibility(View.GONE);
        binding.overlaySuccess.setVisibility(View.GONE);
        binding.scrollConsequence.setVisibility(View.VISIBLE);

        float startX = binding.contentFrame.getWidth();
        if (startX == 0) startX = 800f;
        binding.scrollConsequence.setTranslationX(startX);
        binding.scrollConsequence.animate()
                .translationX(0f)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Success overlay
    // ══════════════════════════════════════════════════════════════════════

    private void showSuccessOverlay() {
        binding.scrollScenario.setVisibility(View.GONE);
        binding.scrollConsequence.setVisibility(View.GONE);
        binding.overlaySuccess.setVisibility(View.VISIBLE);
        binding.overlaySuccess.setAlpha(0f);
        binding.overlaySuccess.animate().alpha(1f).setDuration(400).start();

        View[] views = {
                binding.tvSuccessIcon,
                binding.tvSuccessTitle,
                binding.tvSuccessTitle2,
                binding.tvSuccessSubtitle,
                binding.btnContinue
        };
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            v.setAlpha(0f);
            v.setScaleX(0.5f);
            v.setScaleY(0.5f);
            v.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(200L + i * 110L)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Result
    // ══════════════════════════════════════════════════════════════════════

    private void returnSuccess() {
        if (resultDone) return;
        resultDone = true;
        Intent result = new Intent();
        result.putExtra(EXTRA_TASK_COMPLETED, true);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    /** Updates the three progress pips based on the current level. */
    private void updatePips(int level) {
        View[] pips = {binding.pip1, binding.pip2, binding.pip3};
        for (int i = 0; i < pips.length; i++) {
            int pipLevel = i + 1;
            if (pipLevel < level) {
                pips[i].setBackground(
                        ContextCompat.getDrawable(this, R.drawable.bg_level_pip_done));
            } else if (pipLevel == level) {
                pips[i].setBackground(
                        ContextCompat.getDrawable(this, R.drawable.bg_level_pip_active));
            } else {
                pips[i].setBackground(
                        ContextCompat.getDrawable(this, R.drawable.bg_level_pip_idle));
            }
        }
    }

    /** Flashes a button neon green for 600 ms then restores it. */
    private void flashButtonGreen(com.google.android.material.button.MaterialButton btn) {
        int green = ContextCompat.getColor(this, R.color.cyber_neon_green);
        btn.setBackgroundTintList(ColorStateList.valueOf(green));
        btn.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    /** Resets an option button to the default cyber style. */
    private void resetOptionButton(com.google.android.material.button.MaterialButton btn) {
        btn.setBackgroundTintList(
                ColorStateList.valueOf(android.graphics.Color.parseColor("#FF0A1A0A")));
        btn.setTextColor(
                ContextCompat.getColor(this, R.color.cyber_text_primary));
        btn.setStrokeColor(
                ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.cyber_neon_green)));
        btn.setStrokeWidth(dpToPx(1));
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}