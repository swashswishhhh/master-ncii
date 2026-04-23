package com.example.servermasterncii;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.servermasterncii.databinding.ActivitySplashBinding;

/**
 * SplashActivity — cute cyberpunk robot mascot splash screen.
 *
 * Animation sequence (total ~3.2 seconds):
 *   0ms   — screen is black
 *   200ms — robot drops in with overshoot bounce
 *   600ms — robot starts floating loop + antenna blinks
 *   800ms — title slides up + glows
 *   1100ms — subtitle fades in
 *   1300ms — loading bar begins filling
 *   2800ms — loading bar reaches 100%
 *   3200ms — entire screen fades out → LoginActivity
 *
 * No Lottie required — uses AnimatedVectorDrawable + property animators only.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Total time before we navigate away (ms)
    private static final long SPLASH_DURATION = 3400L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Full-screen immersive — no status bar, no nav bar
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Start all views invisible — we'll animate them in
        resetViews();
        runAnimationSequence();
    }

    // ─────────────────────────────────────────────────────────────
    // Initial state
    // ─────────────────────────────────────────────────────────────

    private void resetViews() {
        binding.ivRobot.setAlpha(0f);
        binding.ivRobot.setTranslationY(-120f);
        binding.ivRobot.setScaleX(0.6f);
        binding.ivRobot.setScaleY(0.6f);

        binding.tvTitle.setAlpha(0f);
        binding.tvTitle.setTranslationY(30f);
        binding.loaderWrap.setAlpha(0f);
        binding.progressLoader.setProgress(0);

        binding.dotsWrap.setAlpha(0f);
    }

    // ─────────────────────────────────────────────────────────────
    // Sequence
    // ─────────────────────────────────────────────────────────────

    private void runAnimationSequence() {

        // ── 1. Robot bounces in ──────────────────────────────────
        handler.postDelayed(() -> {
            binding.ivRobot.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .withEndAction(this::startRobotFloat)
                    .start();
        }, 200);

        // ── 2. Start robot eye-blink AnimatedVectorDrawable ──────
        handler.postDelayed(() -> {
            if (binding.ivRobot.getDrawable() instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) binding.ivRobot.getDrawable()).start();
            }
        }, 600);

        // ── 3. Title slides up ───────────────────────────────────
        handler.postDelayed(() -> {
            binding.tvTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }, 800);


        // ── 5. Loader appears + fills ────────────────────────────
        handler.postDelayed(() -> {
            binding.loaderWrap.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            binding.dotsWrap.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(this::animateDots)
                    .start();
            animateProgressBar();
        }, 1300);

        // ── 6. Fade out entire screen → navigate ─────────────────
        handler.postDelayed(this::exitSplash, SPLASH_DURATION);
    }

    // ─────────────────────────────────────────────────────────────
    // Robot float loop (up/down oscillation)
    // ─────────────────────────────────────────────────────────────

    private void startRobotFloat() {
        floatRobot(true);
    }

    private boolean floatRunning = true;

    private void floatRobot(boolean up) {
        if (!floatRunning) return;
        binding.ivRobot.animate()
                .translationY(up ? -14f : 0f)
                .setDuration(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> floatRobot(!up))
                .start();
    }

    // ─────────────────────────────────────────────────────────────
    // Progress bar — smooth animated fill
    // ─────────────────────────────────────────────────────────────

    private void animateProgressBar() {
        // We animate from 0 → 100 over ~1500ms using a Handler tick
        final long fillDuration = 1500L;
        final long startTime    = System.currentTimeMillis();

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = Math.min(1f, elapsed / (float) fillDuration);

                // Ease: decelerate — fast at first, slows near end
                float eased = 1f - (1f - fraction) * (1f - fraction);
                int progress = (int) (eased * 100);

                binding.progressLoader.setProgress(progress);
                binding.tvLoadPct.setText(progress + "%");

                if (fraction < 1f) {
                    handler.postDelayed(this, 16); // ~60fps
                } else {
                    binding.tvLoadPct.setText("100%");
                    binding.tvLoadLabel.setText("SYSTEM READY");
                }
            }
        };
        handler.post(tick);
    }

    // ─────────────────────────────────────────────────────────────
    // Dots — staggered cyan pulse loop
    // ─────────────────────────────────────────────────────────────

    private void animateDots() {
        animateDot(binding.dot1, 0);
        animateDot(binding.dot2, 200);
        animateDot(binding.dot3, 400);
    }

    private void animateDot(View dot, long startDelay) {
        handler.postDelayed(() -> {
            if (!floatRunning) return; // stop if we're exiting
            dot.animate()
                    .scaleX(1.6f).scaleY(1.6f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        dot.setBackgroundResource(R.drawable.bg_dot_active);
                        dot.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(250)
                                .withEndAction(() -> {
                                    dot.setBackgroundResource(R.drawable.bg_dot);
                                    // Re-schedule for looping
                                    animateDot(dot, 1200);
                                })
                                .start();
                    })
                    .start();
        }, startDelay);
    }

    // ─────────────────────────────────────────────────────────────
    // Exit
    // ─────────────────────────────────────────────────────────────

    private void exitSplash() {
        floatRunning = false;

        // Fade everything out
        binding.getRoot().animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    // Slide transition: new screen slides up from bottom
                    overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out);
                    finish();
                })
                .start();
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        floatRunning = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
        binding = null;
    }
}