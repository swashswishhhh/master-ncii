package com.example.servermasterncii;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityWorkgroupConfigBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * WorkgroupConfigActivity simulates a Windows "System Properties → Computer Name"
 * dialog.  The student must change the Workgroup field to {@code WORKGROUP_B}
 * and click <b>Apply</b>.
 * <p>
 * On success a Lottie celebration plays with a system chime, mimicking the
 * real Windows "Welcome to the workgroup" experience.
 */
public class WorkgroupConfigActivity extends AppCompatActivity {

    /** Intent extra key: set to {@code true} when the mission is completed. */
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    /** The correct workgroup answer. */
    private static final String CORRECT_WORKGROUP = "WORKGROUP_B";

    private ActivityWorkgroupConfigBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate()
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityWorkgroupConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootFrame, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // =====================================================================
    // Button wiring
    // =====================================================================

    private void setupButtons() {
        // Apply — validate the workgroup and trigger celebration if correct
        binding.btnApply.setOnClickListener(v -> onApplyClicked());

        // OK — same as Apply but also finishes the activity on success
        binding.btnOk.setOnClickListener(v -> {
            String workgroup = binding.etWorkgroup.getText().toString().trim();
            if (CORRECT_WORKGROUP.equalsIgnoreCase(workgroup)) {
                onApplyClicked();
            } else {
                showError("Please change the Workgroup to the correct name first.");
            }
        });

        // Cancel — go back
        binding.btnCancel.setOnClickListener(v -> finish());

        // Decorative close button on the title bar
        binding.btnWinClose.setOnClickListener(v -> finish());

        // Dismiss overlay — mark mission as completed and return result
        binding.btnDismissOverlay.setOnClickListener(v -> {
            binding.overlayContainer.setVisibility(View.GONE);
            binding.lottieAnimation.cancelAnimation();
            returnTaskCompleted();
        });
    }

    // =====================================================================
    // Validation & celebration
    // =====================================================================

    /**
     * Validates the Workgroup field. If it matches {@link #CORRECT_WORKGROUP},
     * plays the Lottie animation and a sound chime. Otherwise shows an error.
     */
    private void onApplyClicked() {
        String computerName = binding.etComputerName.getText().toString().trim();
        String workgroup = binding.etWorkgroup.getText().toString().trim();

        // Basic computer-name validation
        if (computerName.isEmpty()) {
            showError("Computer name cannot be empty.");
            binding.etComputerName.requestFocus();
            return;
        }

        // Workgroup validation
        if (workgroup.isEmpty()) {
            showError("Workgroup name cannot be empty.");
            binding.etWorkgroup.requestFocus();
            return;
        }

        if (CORRECT_WORKGROUP.equalsIgnoreCase(workgroup)) {
            playCelebration();
        } else {
            showError("Incorrect workgroup name. Please try again.\n"
                    + "Hint: The target workgroup ends with \"_B\".");
            binding.etWorkgroup.requestFocus();
            binding.etWorkgroup.selectAll();
        }
    }

    // =====================================================================
    // Celebration: Lottie + Sound
    // =====================================================================

    /**
     * Shows the dark overlay, plays the Lottie checkmark/confetti animation,
     * and triggers a system notification chime as a "welcome" sound effect.
     */
    private void playCelebration() {
        // Show the overlay
        binding.overlayContainer.setVisibility(View.VISIBLE);
        binding.overlayContainer.setAlpha(0f);
        binding.overlayContainer.animate()
                .alpha(1f)
                .setDuration(400)
                .start();

        // Reset and play Lottie
        binding.lottieAnimation.setProgress(0f);
        binding.lottieAnimation.playAnimation();

        // Play system notification sound as a "welcome" chime
        playSuccessSound();

        // After the animation progresses a bit, reveal the welcome text
        binding.lottieAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showWelcomeText();
            }
        });

        // Fallback: show text after 2.5 s even if animation listener doesn't fire
        handler.postDelayed(this::showWelcomeText, 2500);
    }

    /**
     * Fades in the "Welcome to the Workgroup!" text and the Continue button
     * with a pleasant overshoot scale animation.
     */
    private void showWelcomeText() {
        if (binding == null) return;

        // Guard against double-firing (listener + handler)
        if (binding.tvWelcomeTitle.getVisibility() == View.VISIBLE) return;

        binding.tvWelcomeTitle.setVisibility(View.VISIBLE);
        binding.tvWelcomeMessage.setVisibility(View.VISIBLE);
        binding.btnDismissOverlay.setVisibility(View.VISIBLE);

        // Scale-up entrance
        View[] views = {binding.tvWelcomeTitle, binding.tvWelcomeMessage, binding.btnDismissOverlay};
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setScaleX(0.6f);
            view.setScaleY(0.6f);
            view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(i * 150L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    /**
     * Plays the system default notification sound as a success chime.
     * Falls back silently if no sound is available.
     */
    private void playSuccessSound() {
        try {
            releaseMediaPlayer();
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(this, notification);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Non-critical — the animation alone is fine
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void showError(String message) {
        Snackbar.make(binding.rootFrame, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.quiz_wrong, getTheme()))
                .setTextColor(getResources().getColor(android.R.color.white, getTheme()))
                .show();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
    }

    /**
     * Sets RESULT_OK with EXTRA_TASK_COMPLETED and finishes the activity.
     * The QuizActivity GameRouter uses this to award credit.
     */
    private void returnTaskCompleted() {
        Intent result = new Intent();
        result.putExtra(EXTRA_TASK_COMPLETED, true);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
