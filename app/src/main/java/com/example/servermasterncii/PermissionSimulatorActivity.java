package com.example.servermasterncii;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityPermissionSimulatorBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

/**
 * PermissionSimulatorActivity — NTFS permission drag-and-drop puzzle.
 *
 * <p>The player drags permission badges (Full Control / Change / Read) onto
 * four user-role cards (IT Admin / Accountant / Intern / HR Manager).
 * The SUBMIT button activates once all four cards have a badge assigned.
 * Correct mapping returns RESULT_OK; wrong cards shake and show "RECONFIGURE".</p>
 *
 * <h3>Correct mapping</h3>
 * <ul>
 *   <li>IT Admin     → Full Control</li>
 *   <li>Accountant   → Change</li>
 *   <li>Intern       → Read</li>
 *   <li>HR Manager   → Read</li>
 * </ul>
 *
 * <p>Drag is implemented with the platform {@link View#startDragAndDrop} API
 * (API 24+) and {@link View.OnDragListener} — no deprecated helpers.</p>
 */
public class PermissionSimulatorActivity extends AppCompatActivity {

    // ── Intent extra ───────────────────────────────────────────────────────
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    // ── Badge label constants (also used as ClipData MIME labels) ─────────
    private static final String BADGE_FULL_CONTROL = "Full Control";
    private static final String BADGE_CHANGE       = "Change";
    private static final String BADGE_READ         = "Read";

    // ── User role keys (used as map keys) ─────────────────────────────────
    private static final String ROLE_IT_ADMIN    = "IT Admin";
    private static final String ROLE_ACCOUNTANT  = "Accountant";
    private static final String ROLE_INTERN      = "Intern";
    private static final String ROLE_HR_MANAGER  = "HR Manager";

    // ── Correct answer map ────────────────────────────────────────────────
    private static final Map<String, String> CORRECT_MAP = new HashMap<>();
    static {
        CORRECT_MAP.put(ROLE_IT_ADMIN,   BADGE_FULL_CONTROL);
        CORRECT_MAP.put(ROLE_ACCOUNTANT, BADGE_CHANGE);
        CORRECT_MAP.put(ROLE_INTERN,     BADGE_READ);
        CORRECT_MAP.put(ROLE_HR_MANAGER, BADGE_READ);
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    private ActivityPermissionSimulatorBinding binding;

    // ── State ──────────────────────────────────────────────────────────────
    /** Maps role key → currently assigned badge label (null = unassigned). */
    private final Map<String, String> assignments = new HashMap<>();

    private int attemptCount   = 0;
    private boolean resultDone = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPermissionSimulatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Initialise all roles as unassigned
        assignments.put(ROLE_IT_ADMIN,   null);
        assignments.put(ROLE_ACCOUNTANT, null);
        assignments.put(ROLE_INTERN,     null);
        assignments.put(ROLE_HR_MANAGER, null);

        setupDragSources();
        setupDropTargets();
        setupButtons();
        refreshSubmitButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Drag sources — the three badge TextViews
    // ══════════════════════════════════════════════════════════════════════

    private void setupDragSources() {
        setupBadgeDrag(binding.badgeFullControl, BADGE_FULL_CONTROL);
        setupBadgeDrag(binding.badgeChange,      BADGE_CHANGE);
        setupBadgeDrag(binding.badgeRead,        BADGE_READ);
    }

    /**
     * Attaches a long-click listener that starts a drag-and-drop operation.
     * The badge label is encoded in a {@link ClipData} item so drop targets
     * can read it without needing a reference to the source view.
     */
    private void setupBadgeDrag(View badge, String label) {
        badge.setOnLongClickListener(v -> {
            ClipData clip = ClipData.newPlainText(label, label);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(clip, shadow, label, 0);
            return true;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Drop targets — the four drop-zone TextViews inside each user card
    // ══════════════════════════════════════════════════════════════════════

    private void setupDropTargets() {
        setupDropZone(binding.dropZoneItAdmin,    ROLE_IT_ADMIN,    binding.cardItAdmin);
        setupDropZone(binding.dropZoneAccountant, ROLE_ACCOUNTANT,  binding.cardAccountant);
        setupDropZone(binding.dropZoneIntern,     ROLE_INTERN,      binding.cardIntern);
        setupDropZone(binding.dropZoneHrManager,  ROLE_HR_MANAGER,  binding.cardHrManager);
    }

    /**
     * Wires a drop-zone view to accept badge drags.
     *
     * <ul>
     *   <li>DRAG_STARTED  — highlight the zone if the drag carries text</li>
     *   <li>DRAG_ENTERED  — stronger hover highlight</li>
     *   <li>DRAG_EXITED   — revert to idle or filled state</li>
     *   <li>DROP          — assign the badge; update UI</li>
     *   <li>DRAG_ENDED    — clean up all highlights</li>
     * </ul>
     */
    private void setupDropZone(View dropZone, String roleKey, MaterialCardView parentCard) {
        dropZone.setOnDragListener((v, event) -> {
            switch (event.getAction()) {

                case DragEvent.ACTION_DRAG_STARTED:
                    // Accept any plain-text drag (our badges)
                    return event.getClipDescription()
                            .hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackground(ContextCompat.getDrawable(
                            this, R.drawable.bg_drop_zone_hover));
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // Revert to filled or idle depending on current assignment
                    refreshDropZoneBackground(v, roleKey);
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DROP:
                    // Read the badge label from the clip
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    String badgeLabel = item.getText().toString();

                    // Assign and refresh
                    assignments.put(roleKey, badgeLabel);
                    applyBadgeToDropZone(v, badgeLabel);
                    refreshSubmitButton();
                    hideStatusMessage();

                    // Clear any error state on this card
                    resetCardStroke(parentCard);
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // If the drop was NOT on this zone, revert its highlight
                    if (!event.getResult()) {
                        refreshDropZoneBackground(v, roleKey);
                    }
                    v.invalidate();
                    return true;

                default:
                    return false;
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Drop-zone visual helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Updates the drop-zone text and background to reflect the assigned badge.
     */
    private void applyBadgeToDropZone(View dropZone, String badgeLabel) {
        if (!(dropZone instanceof android.widget.TextView)) return;
        android.widget.TextView tv = (android.widget.TextView) dropZone;

        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_drop_zone_filled));

        switch (badgeLabel) {
            case BADGE_FULL_CONTROL:
                tv.setText(badgeLabel);
                tv.setTextColor(ContextCompat.getColor(this, R.color.cyber_neon_green));
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                break;
            case BADGE_CHANGE:
                tv.setText(badgeLabel);
                tv.setTextColor(ContextCompat.getColor(this, R.color.cyber_blue));
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                break;
            case BADGE_READ:
                tv.setText(badgeLabel);
                tv.setTextColor(ContextCompat.getColor(this, R.color.cyber_gold));
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                break;
            default:
                tv.setText("DROP HERE");
                tv.setTextColor(ContextCompat.getColor(this, R.color.cyber_text_muted));
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                break;
        }
    }

    /**
     * Sets the drop-zone background to filled (if assigned) or idle (if empty).
     */
    private void refreshDropZoneBackground(View dropZone, String roleKey) {
        String assigned = assignments.get(roleKey);
        int bgRes = (assigned != null)
                ? R.drawable.bg_drop_zone_filled
                : R.drawable.bg_drop_zone_idle;
        dropZone.setBackground(ContextCompat.getDrawable(this, bgRes));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Submit button
    // ══════════════════════════════════════════════════════════════════════

    private void setupButtons() {
        binding.btnSubmit.setOnClickListener(v -> onSubmitClicked());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnContinue.setOnClickListener(v -> returnSuccess());
    }

    /**
     * Enables the SUBMIT button only when all four roles have an assignment.
     */
    private void refreshSubmitButton() {
        boolean allAssigned = true;
        for (String val : assignments.values()) {
            if (val == null) { allAssigned = false; break; }
        }

        binding.btnSubmit.setEnabled(allAssigned);
        binding.btnSubmit.setBackgroundTintList(ColorStateList.valueOf(
                allAssigned
                        ? ContextCompat.getColor(this, R.color.cyber_neon_green)
                        : ContextCompat.getColor(this, R.color.cyber_text_muted)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════

    private void onSubmitClicked() {
        attemptCount++;
        binding.tvAttempts.setText("ATTEMPTS: " + attemptCount);

        boolean allCorrect = true;
        for (Map.Entry<String, String> entry : CORRECT_MAP.entrySet()) {
            String role    = entry.getKey();
            String correct = entry.getValue();
            String actual  = assignments.get(role);

            if (!correct.equals(actual)) {
                allCorrect = false;
                shakeCard(getCardForRole(role));
                highlightCardError(getCardForRole(role));
            } else {
                resetCardStroke(getCardForRole(role));
            }
        }

        if (allCorrect) {
            hideStatusMessage();
            showSuccessOverlay();
        } else {
            showStatusMessage("⚠  RECONFIGURE — INCORRECT PERMISSION MAPPING");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Card helpers
    // ══════════════════════════════════════════════════════════════════════

    private MaterialCardView getCardForRole(String role) {
        switch (role) {
            case ROLE_IT_ADMIN:   return binding.cardItAdmin;
            case ROLE_ACCOUNTANT: return binding.cardAccountant;
            case ROLE_INTERN:     return binding.cardIntern;
            case ROLE_HR_MANAGER: return binding.cardHrManager;
            default:              return null;
        }
    }

    private void highlightCardError(MaterialCardView card) {
        if (card == null) return;
        card.setStrokeColor(ContextCompat.getColor(this, R.color.quiz_wrong));
        card.setStrokeWidth(dpToPx(2));
        card.setCardBackgroundColor(Color.parseColor("#1AF44336"));
    }

    private void resetCardStroke(MaterialCardView card) {
        if (card == null) return;
        card.setStrokeColor(ContextCompat.getColor(this, R.color.cyber_neon_green));
        card.setStrokeWidth(dpToPx(1));
        card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.cyber_card_surface));
    }

    /**
     * Horizontal shake animation — 3 cycles, 400 ms total.
     */
    private void shakeCard(View view) {
        if (view == null) return;
        ObjectAnimator shaker = ObjectAnimator.ofFloat(view, "translationX", 0f, 14f);
        shaker.setDuration(400);
        shaker.setInterpolator(new CycleInterpolator(3));
        shaker.start();
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

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
