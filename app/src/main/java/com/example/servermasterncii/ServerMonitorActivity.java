package com.example.servermasterncii;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityServerMonitorBinding;

import java.util.Locale;

/**
 * ServerMonitorActivity — Server threat identification mini-game.
 *
 * <p>Displays a fake server dashboard with three server panels. SERVER-02 is
 * the critical one (CPU 92%, RAM 88%, DISK 95%). The player must tap it within
 * 30 seconds. Correct tap → "THREAT NEUTRALIZED" overlay → RESULT_OK.
 * Wrong tap → -20 integrity, shake, "WRONG SERVER — SYSTEMS EXPOSED".</p>
 */
public class ServerMonitorActivity extends AppCompatActivity {

    // ── Intent extra ───────────────────────────────────────────────────────
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    // ── Timer constants (identical to QuizActivity) ────────────────────────
    private static final long TIMER_DURATION_MS = 30_000L;
    private static final long TIMER_INTERVAL_MS =  1_000L;

    // ── Integrity ──────────────────────────────────────────────────────────
    private static final int MAX_INTEGRITY     = 100;
    private static final int INTEGRITY_PENALTY =  20;
    private int currentIntegrity               = MAX_INTEGRITY;

    // ── UI ─────────────────────────────────────────────────────────────────
    private ActivityServerMonitorBinding binding;

    // ── State ──────────────────────────────────────────────────────────────
    private CountDownTimer countDownTimer;
    private ObjectAnimator cpuPulseAnimator;
    private boolean answered    = false;
    private boolean resultDone  = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityServerMonitorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        setupCardClicks();
        startCriticalPulse();
        startTimer();
        updateIntegrityBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        stopCriticalPulse();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Card click handlers
    // ══════════════════════════════════════════════════════════════════════

    private void setupCardClicks() {
        binding.cardServer01.setOnClickListener(v -> onServerTapped(false));
        binding.cardServer02.setOnClickListener(v -> onServerTapped(true));
        binding.cardServer03.setOnClickListener(v -> onServerTapped(false));
    }

    /**
     * @param isCorrect true only when SERVER-02 is tapped
     */
    private void onServerTapped(boolean isCorrect) {
        if (answered) return;
        answered = true;
        cancelTimer();

        if (isCorrect) {
            handleCorrect();
        } else {
            handleWrong();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Correct answer
    // ══════════════════════════════════════════════════════════════════════

    private void handleCorrect() {
        stopCriticalPulse();

        // Green flash on SERVER-02 card
        flashCard(binding.cardServer02, true);

        // Brief delay then show overlay
        handler.postDelayed(this::showSuccessOverlay, 600);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Wrong answer
    // ══════════════════════════════════════════════════════════════════════

    private void handleWrong() {
        // Integrity penalty
        currentIntegrity = Math.max(0, currentIntegrity - INTEGRITY_PENALTY);
        updateIntegrityBar();

        // Red flash on the tapped card (we flash all non-critical cards red)
        flashCard(binding.cardServer01, false);
        flashCard(binding.cardServer03, false);

        showStatusMessage("⚠  WRONG SERVER — SYSTEMS EXPOSED");

        // Allow retry — reset answered so player can try again
        handler.postDelayed(() -> {
            answered = false;
            startTimer();   // restart timer for remaining attempts
        }, 1500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Timer — identical pattern to QuizActivity
    // ══════════════════════════════════════════════════════════════════════

    private void startTimer() {
        cancelTimer();
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, TIMER_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                long seconds = millisUntilFinished / 1000;
                binding.tvTimer.setText(
                        String.format(Locale.getDefault(),
                                "%02d:%02d", seconds / 60, seconds % 60));

                if (seconds <= 10) {
                    binding.tvTimer.setTextColor(
                            ContextCompat.getColor(ServerMonitorActivity.this,
                                    R.color.quiz_wrong));
                } else {
                    binding.tvTimer.setTextColor(
                            ContextCompat.getColor(ServerMonitorActivity.this,
                                    R.color.cyber_neon_green_bright));
                }
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
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
        stopCriticalPulse();
        showStatusMessage("⏱  TIME EXPIRED — THREAT UNCONTAINED");
        // Auto-fail: return without RESULT_OK
        handler.postDelayed(this::finish, 2500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Critical server pulse animation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Pulses the SERVER-02 CPU progress bar alpha between 0.5 and 1.0
     * at 500 ms per cycle to draw the player's eye to the critical server.
     */
    private void startCriticalPulse() {
        cpuPulseAnimator = ObjectAnimator.ofFloat(
                binding.progressS02Cpu, "alpha", 0.5f, 1.0f);
        cpuPulseAnimator.setDuration(500);
        cpuPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        cpuPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cpuPulseAnimator.start();
    }

    private void stopCriticalPulse() {
        if (cpuPulseAnimator != null) {
            cpuPulseAnimator.cancel();
            cpuPulseAnimator = null;
            if (binding != null) {
                binding.progressS02Cpu.setAlpha(1f);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Success overlay
    // ══════════════════════════════════════════════════════════════════════

    private void showSuccessOverlay() {
        if (binding == null) return;
        binding.overlaySuccess.setVisibility(View.VISIBLE);
        binding.overlaySuccess.setAlpha(0f);
        binding.overlaySuccess.animate()
                .alpha(1f)
                .setDuration(400)
                .start();

        View[] views = {
                binding.tvSuccessIcon,
                binding.tvSuccessTitle,
                binding.tvSuccessSubtitle,
                binding.btnContinue
        };
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            v.setAlpha(0f);
            v.setScaleX(0.5f);
            v.setScaleY(0.5f);
            v.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(200L + i * 120L)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }

        binding.btnContinue.setOnClickListener(v -> returnSuccess());
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
    // Integrity bar — identical logic to QuizActivity
    // ══════════════════════════════════════════════════════════════════════

    private void updateIntegrityBar() {
        if (binding == null) return;
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

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private void showStatusMessage(String message) {
        if (binding == null) return;
        binding.tvStatusMessage.setText(message);
        if (binding.tvStatusMessage.getVisibility() != View.VISIBLE) {
            binding.tvStatusMessage.setVisibility(View.VISIBLE);
            binding.tvStatusMessage.setAlpha(0f);
            binding.tvStatusMessage.animate().alpha(1f).setDuration(250).start();
        }
    }

    /**
     * Briefly flashes a card green (correct) or red (wrong) then restores it.
     *
     * @param card      the MaterialCardView to flash
     * @param isCorrect true = green flash, false = red flash
     */
    private void flashCard(com.google.android.material.card.MaterialCardView card,
                           boolean isCorrect) {
        if (card == null) return;
        int flashColor = isCorrect
                ? ContextCompat.getColor(this, R.color.monitor_ok_green)
                : ContextCompat.getColor(this, R.color.monitor_critical_red);
        int originalStroke = isCorrect
                ? ContextCompat.getColor(this, R.color.monitor_critical_red)
                : ContextCompat.getColor(this, R.color.monitor_ok_green);

        card.setStrokeColor(flashColor);
        card.setStrokeWidth(dpToPx(3));

        handler.postDelayed(() -> {
            if (binding == null) return;
            card.setStrokeColor(originalStroke);
            card.setStrokeWidth(dpToPx(isCorrect ? 2 : 1));
        }, 600);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}