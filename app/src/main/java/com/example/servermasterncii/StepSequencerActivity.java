package com.example.servermasterncii;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.databinding.ActivityStepSequencerBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * StepSequencerActivity — Drag-to-reorder puzzle for RDP configuration steps.
 *
 * <p>The player must arrange 6 shuffled step cards into the correct sequence.
 * Correct order is validated on VERIFY; wrong cards are highlighted in red.
 * On success a full-screen neon overlay is shown and RESULT_OK is returned.</p>
 *
 * <p>Returns {@link Activity#RESULT_OK} with
 * {@code extra_task_completed = true} when the player submits the correct order.</p>
 */
public class StepSequencerActivity extends AppCompatActivity {

    // ── Intent extras ──────────────────────────────────────────────────────
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    // ── Correct RDP configuration sequence (display strings) ──────────────
    private static final String[] CORRECT_STEPS = {
            "Open Server Manager",
            "Add Remote Desktop Services Role",
            "Configure Network Level Authentication (NLA)",
            "Open Windows Firewall and allow port 3389",
            "Add users to Remote Desktop Users group",
            "Test connection using mstsc.exe"
    };

    /**
     * The expected order after sorting: index i of this array holds the
     * position (0-based) that CORRECT_STEPS[i] should occupy.
     * Since the correct sequence IS the natural order, this is simply {0,1,2,3,4,5}.
     */
    private static final int[] CORRECT_ORDER = {0, 1, 2, 3, 4, 5};

    // ── UI ─────────────────────────────────────────────────────────────────
    private ActivityStepSequencerBinding binding;

    // ── Data ───────────────────────────────────────────────────────────────
    private StepSequencerAdapter adapter;
    private List<String> shuffledSteps;

    // ── State ──────────────────────────────────────────────────────────────
    private int attemptCount = 0;
    private boolean resultReturned = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityStepSequencerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        buildShuffledList();
        setupRecyclerView();
        setupButtons();
        updateAttemptLabel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Setup
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a shuffled copy of the correct steps, ensuring it is never
     * accidentally in the correct order on first display.
     */
    private void buildShuffledList() {
        shuffledSteps = new ArrayList<>(Arrays.asList(CORRECT_STEPS));
        // Shuffle until the order differs from the correct sequence
        do {
            Collections.shuffle(shuffledSteps);
        } while (isCurrentOrderCorrect());
    }

    private void setupRecyclerView() {
        adapter = new StepSequencerAdapter(shuffledSteps);

        // ItemTouchHelper — enables long-press drag and drop reordering
        ItemTouchHelper touchHelper = new ItemTouchHelper(new DragCallback());
        touchHelper.attachToRecyclerView(binding.recyclerSteps);
        adapter.attachTouchHelper(touchHelper);

        binding.recyclerSteps.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSteps.setAdapter(adapter);
        // Disable default change animation so badge numbers update instantly
        RecyclerView.ItemAnimator animator = binding.recyclerSteps.getItemAnimator();
        if (animator != null) {
            animator.setChangeDuration(0);
        }
    }

    private void setupButtons() {
        binding.btnVerify.setOnClickListener(v -> onVerifyClicked());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnContinue.setOnClickListener(v -> returnSuccess());
    }

    // ══════════════════════════════════════════════════════════════════════
    // Verification logic
    // ══════════════════════════════════════════════════════════════════════

    private void onVerifyClicked() {
        attemptCount++;
        updateAttemptLabel();

        if (isCurrentOrderCorrect()) {
            adapter.clearErrors();
            hideStatusMessage();
            showSuccessOverlay();
        } else {
            showErrors();
        }
    }

    /**
     * Checks whether the current RecyclerView order matches the correct sequence.
     *
     * <p>Strategy: the correct sequence is CORRECT_STEPS[0..5] in order.
     * We compare each position in the adapter's list to the expected string.</p>
     */
    private boolean isCurrentOrderCorrect() {
        List<String> current = adapter.getSteps();
        for (int i = 0; i < CORRECT_ORDER.length; i++) {
            // CORRECT_ORDER[i] == i always (natural order), so we just compare
            // the string at position i to CORRECT_STEPS[i]
            if (!current.get(i).equals(CORRECT_STEPS[CORRECT_ORDER[i]])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Highlights cards that are in the wrong position and shows the
     * "RECONFIGURE" status message.
     */
    private void showErrors() {
        List<String> current = adapter.getSteps();
        boolean[] flags = new boolean[current.size()];
        for (int i = 0; i < current.size(); i++) {
            flags[i] = !current.get(i).equals(CORRECT_STEPS[CORRECT_ORDER[i]]);
        }
        adapter.setErrorFlags(flags);

        // Count wrong positions for the message
        int wrongCount = 0;
        for (boolean f : flags) if (f) wrongCount++;

        showStatusMessage(
                "⚠  RECONFIGURE — " + wrongCount + " STEP(S) OUT OF SEQUENCE");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Success overlay
    // ══════════════════════════════════════════════════════════════════════

    private void showSuccessOverlay() {
        binding.overlaySuccess.setVisibility(View.VISIBLE);
        binding.overlaySuccess.setAlpha(0f);
        binding.overlaySuccess.animate()
                .alpha(1f)
                .setDuration(400)
                .start();

        // Animate children in with overshoot
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
    }

    // ══════════════════════════════════════════════════════════════════════
    // Result
    // ══════════════════════════════════════════════════════════════════════

    private void returnSuccess() {
        if (resultReturned) return;
        resultReturned = true;

        Intent result = new Intent();
        result.putExtra(EXTRA_TASK_COMPLETED, true);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private void updateAttemptLabel() {
        binding.tvAttempts.setText("ATTEMPTS: " + attemptCount);
    }

    private void showStatusMessage(String message) {
        binding.tvStatusMessage.setText(message);
        if (binding.tvStatusMessage.getVisibility() != View.VISIBLE) {
            binding.tvStatusMessage.setVisibility(View.VISIBLE);
            binding.tvStatusMessage.setAlpha(0f);
            binding.tvStatusMessage.animate().alpha(1f).setDuration(250).start();
        }
    }

    private void hideStatusMessage() {
        binding.tvStatusMessage.setVisibility(View.GONE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ItemTouchHelper.Callback — drag reorder
    // ══════════════════════════════════════════════════════════════════════

    private class DragCallback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh) {
            // Allow dragging up and down; no swipe
            return makeMovementFlags(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv,
                              @NonNull RecyclerView.ViewHolder source,
                              @NonNull RecyclerView.ViewHolder target) {
            adapter.moveItem(source.getAdapterPosition(),
                             target.getAdapterPosition());
            // Clear errors while the user is actively reordering
            adapter.clearErrors();
            hideStatusMessage();
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            // Swipe disabled — no-op
        }

        // ── Visual feedback during drag ──────────────────────────────────

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder vh, int actionState) {
            super.onSelectedChanged(vh, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) {
                // Lift the card: scale up + dim background
                vh.itemView.animate()
                        .scaleX(1.04f)
                        .scaleY(1.04f)
                        .alpha(0.92f)
                        .setDuration(150)
                        .start();
                vh.itemView.setElevation(16f);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView rv,
                              @NonNull RecyclerView.ViewHolder vh) {
            super.clearView(rv, vh);
            // Return card to normal state
            vh.itemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(150)
                    .start();
            vh.itemView.setElevation(4f);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // We handle long-press ourselves in the adapter so the whole card
            // acts as a drag trigger; the handle also triggers it via touch.
            return true;
        }
    }
}
