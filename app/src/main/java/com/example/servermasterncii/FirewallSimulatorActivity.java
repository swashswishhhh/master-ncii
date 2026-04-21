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

import com.example.servermasterncii.databinding.ActivityFirewallSimulatorBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * FirewallSimulatorActivity simulates the Windows "Allow an app through
 * Windows Defender Firewall" dialog.
 * <p>
 * The student must check <b>both</b> the Private and Public boxes for
 * "File and Printer Sharing" and click <b>OK</b>.
 * <p>
 * On success a Lottie shield-glow animation plays with a system chime,
 * indicating correct firewall configuration.
 */
public class FirewallSimulatorActivity extends AppCompatActivity {

    /** Intent extra key: set to {@code true} when the mission is completed. */
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    private ActivityFirewallSimulatorBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Tracks whether the "Change settings" button has been clicked. */
    private boolean settingsUnlocked = false;

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate()
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFirewallSimulatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootFrame, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initially lock the target checkboxes
        setFileSharingCheckboxesEnabled(false);

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
        // "Change settings" — unlocks the File and Printer Sharing checkboxes
        binding.btnChangeSettings.setOnClickListener(v -> {
            settingsUnlocked = true;
            setFileSharingCheckboxesEnabled(true);
            binding.btnChangeSettings.setEnabled(false);
            binding.btnChangeSettings.setAlpha(0.5f);

            Snackbar.make(binding.rootFrame,
                            "Settings unlocked — you can now change firewall rules.",
                            Snackbar.LENGTH_SHORT)
                    .show();
        });

        // OK — validate the configuration
        binding.btnOk.setOnClickListener(v -> onOkClicked());

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
    // Enable / disable the target row
    // =====================================================================

    /**
     * Enables or disables the interactive checkboxes for "File and Printer Sharing".
     * All other rows remain decoratively locked.
     */
    private void setFileSharingCheckboxesEnabled(boolean enabled) {
        binding.cbFileSharingPrivate.setEnabled(enabled);
        binding.cbFileSharingPublic.setEnabled(enabled);
        binding.cbFileSharingPrivate.setAlpha(enabled ? 1f : 0.5f);
        binding.cbFileSharingPublic.setAlpha(enabled ? 1f : 0.5f);
    }

    // =====================================================================
    // Validation & celebration
    // =====================================================================

    /**
     * Validates the firewall configuration. The correct answer is both
     * Private and Public checked for "File and Printer Sharing".
     */
    private void onOkClicked() {
        if (!settingsUnlocked) {
            showError("Click \"Change settings\" first to unlock the firewall rules.");
            return;
        }

        boolean privateChecked = binding.cbFileSharingPrivate.isChecked();
        boolean publicChecked = binding.cbFileSharingPublic.isChecked();

        if (privateChecked && publicChecked) {
            // Correct!
            playCelebration();
        } else if (!privateChecked && !publicChecked) {
            showError("You haven't checked anything yet.\n" +
                    "Hint: Enable both Private and Public for File and Printer Sharing.");
        } else if (privateChecked) {
            showError("Almost there! You also need to check the Public column.\n" +
                    "File sharing should be allowed on both network types.");
        } else {
            showError("Almost there! You also need to check the Private column.\n" +
                    "File sharing should be allowed on both network types.");
        }
    }

    // =====================================================================
    // Celebration: Lottie + Sound
    // =====================================================================

    /**
     * Shows the dark overlay, plays the Lottie shield-glow animation,
     * and triggers a system notification chime as a success sound effect.
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

        // Play system notification sound as a success chime
        playSuccessSound();

        // After the animation progresses, reveal the success text
        binding.lottieAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showSuccessText();
            }
        });

        // Fallback: show text after 2.5 s even if animation listener doesn't fire
        handler.postDelayed(this::showSuccessText, 2500);
    }

    /**
     * Fades in the "Firewall Configured!" text and the Continue button
     * with a pleasant overshoot scale animation.
     */
    private void showSuccessText() {
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
