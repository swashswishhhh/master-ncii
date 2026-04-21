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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityIpConfigSimulatorBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * IPConfigSimulatorActivity simulates the Windows "Internet Protocol Version 4
 * (TCP/IPv4) Properties" dialog.
 * <p>
 * The student must enter the first valid IP address of the 192.168.1.0/24
 * network:
 * <ul>
 *     <li>IP Address: <b>192.168.1.1</b></li>
 *     <li>Subnet Mask: <b>255.255.255.0</b></li>
 * </ul>
 * On success a Lottie animation of a computer connecting to a router plays
 * along with a "Connection Established" sound.
 */
public class IPConfigSimulatorActivity extends AppCompatActivity {

    /** Intent extra key: set to {@code true} when the mission is completed. */
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    // Correct answers
    private static final int[] CORRECT_IP   = {192, 168, 1, 1};
    private static final int[] CORRECT_MASK = {255, 255, 255, 0};

    private ActivityIpConfigSimulatorBinding binding;
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

        binding = ActivityIpConfigSimulatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootFrame, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupAutoAdvance();
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
    // Auto-advance: jump to the next octet box after 3 digits
    // =====================================================================

    /**
     * Wires up auto-advance so that typing 3 digits in an octet field
     * automatically moves focus to the next field.
     */
    private void setupAutoAdvance() {
        // IP row
        wireAutoAdvance(binding.etIp1, binding.etIp2);
        wireAutoAdvance(binding.etIp2, binding.etIp3);
        wireAutoAdvance(binding.etIp3, binding.etIp4);
        wireAutoAdvance(binding.etIp4, binding.etSub1);

        // Subnet row
        wireAutoAdvance(binding.etSub1, binding.etSub2);
        wireAutoAdvance(binding.etSub2, binding.etSub3);
        wireAutoAdvance(binding.etSub3, binding.etSub4);
    }

    /**
     * When {@code current} reaches 3 characters, focus moves to {@code next}.
     */
    private void wireAutoAdvance(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 3) {
                    next.requestFocus();
                    next.selectAll();
                }
            }
        });
    }

    // =====================================================================
    // Button wiring
    // =====================================================================

    private void setupButtons() {
        binding.btnOk.setOnClickListener(v -> onOkClicked());
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnWinClose.setOnClickListener(v -> finish());

        binding.btnDismissOverlay.setOnClickListener(v -> {
            binding.overlayContainer.setVisibility(View.GONE);
            binding.lottieAnimation.cancelAnimation();
            returnTaskCompleted();
        });
    }

    // =====================================================================
    // Validation & celebration
    // =====================================================================

    private void onOkClicked() {
        // Read IP octets
        int[] ip = readOctets(binding.etIp1, binding.etIp2, binding.etIp3, binding.etIp4);
        if (ip == null) {
            showError("Please fill in all four IP address octets (0-255).");
            return;
        }

        // Read Subnet octets
        int[] mask = readOctets(binding.etSub1, binding.etSub2, binding.etSub3, binding.etSub4);
        if (mask == null) {
            showError("Please fill in all four Subnet mask octets (0-255).");
            return;
        }

        // Check IP
        boolean ipCorrect = matches(ip, CORRECT_IP);
        boolean maskCorrect = matches(mask, CORRECT_MASK);

        if (ipCorrect && maskCorrect) {
            playCelebration();
        } else if (!ipCorrect && !maskCorrect) {
            showError("Both IP and Subnet are incorrect.\n"
                    + "Hint: The first valid host in 192.168.1.0/24 is 192.168.1.1 "
                    + "with mask 255.255.255.0.");
        } else if (!ipCorrect) {
            showError("Subnet mask is correct, but the IP address is wrong.\n"
                    + "Hint: The network is 192.168.1.0 — what's the first usable host?");
        } else {
            showError("IP address is correct, but the Subnet mask is wrong.\n"
                    + "Hint: /24 means the first three octets are 255.");
        }
    }

    /**
     * Reads four octet values from four EditTexts.
     * Returns {@code null} if any field is empty or contains an invalid value.
     */
    private int[] readOctets(EditText e1, EditText e2, EditText e3, EditText e4) {
        EditText[] fields = {e1, e2, e3, e4};
        int[] values = new int[4];
        for (int i = 0; i < 4; i++) {
            String text = fields[i].getText().toString().trim();
            if (text.isEmpty()) return null;
            try {
                int val = Integer.parseInt(text);
                if (val < 0 || val > 255) return null;
                values[i] = val;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return values;
    }

    private boolean matches(int[] entered, int[] correct) {
        for (int i = 0; i < 4; i++) {
            if (entered[i] != correct[i]) return false;
        }
        return true;
    }

    // =====================================================================
    // Celebration: Lottie + Sound
    // =====================================================================

    private void playCelebration() {
        // Show overlay
        binding.overlayContainer.setVisibility(View.VISIBLE);
        binding.overlayContainer.setAlpha(0f);
        binding.overlayContainer.animate()
                .alpha(1f)
                .setDuration(400)
                .start();

        // Reset and play Lottie
        binding.lottieAnimation.setProgress(0f);
        binding.lottieAnimation.playAnimation();

        // Play success sound
        playSuccessSound();

        // After animation ends, reveal text
        binding.lottieAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showSuccessText();
            }
        });

        // Fallback
        handler.postDelayed(this::showSuccessText, 3000);
    }

    private void showSuccessText() {
        if (binding == null) return;
        if (binding.tvSuccessTitle.getVisibility() == View.VISIBLE) return;

        binding.tvSuccessTitle.setVisibility(View.VISIBLE);
        binding.tvSuccessMessage.setVisibility(View.VISIBLE);
        binding.btnDismissOverlay.setVisibility(View.VISIBLE);

        View[] views = {binding.tvSuccessTitle, binding.tvSuccessMessage, binding.btnDismissOverlay};
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
            // Non-critical
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
