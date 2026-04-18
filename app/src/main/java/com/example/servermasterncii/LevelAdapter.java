package com.example.servermasterncii;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.databinding.ItemLevelCardBinding;
import com.example.servermasterncii.model.Level;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the Saga Map level grid.
 * <p>
 * Uses {@link ItemLevelCardBinding} (ViewBinding) and displays each level card
 * in one of three visual states:
 * <ul>
 *     <li><b>Locked</b>   — dimmed, gray stroke, no interaction</li>
 *     <li><b>Unlocked</b> — full brightness, neon green (#39FF14) glow border</li>
 *     <li><b>Completed</b> — full brightness, cyan (#00CCFF) glow border</li>
 * </ul>
 */
public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {

    /** Callback for handling level clicks. */
    public interface OnLevelClickListener {
        void onLevelClick(Level level);
    }

    private final List<Level> levels;
    private final OnLevelClickListener listener;

    public LevelAdapter(List<Level> levels, OnLevelClickListener listener) {
        this.levels = levels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLevelCardBinding binding = ItemLevelCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LevelViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        Level level = levels.get(position);
        ItemLevelCardBinding b = holder.binding;

        // --- Text fields ---
        b.tvLevelNumber.setText(
                String.format(Locale.getDefault(), "MISSION %s", level.getLevelId()));
        b.tvLevelTitle.setText(level.getTitle());
        b.tvLevelSubtitle.setText(level.getSubtitle());

        // --- Level Type tag ---
        String typeLabel = formatLevelType(level.getLevelType());
        b.tvLevelType.setText(typeLabel);

        // --- Lottie animation ---
        b.lottieIcon.setAnimation(level.getLottieAsset());
        b.lottieIcon.playAnimation();

        // --- Apply visual state ---
        if (!level.isUnlocked()) {
            // ══════ LOCKED STATE ══════
            applyLockedState(holder);
        } else if (level.isCompleted()) {
            // ══════ COMPLETED STATE (≥ 70%) ══════
            applyCompletedState(holder, level);
        } else {
            // ══════ UNLOCKED STATE ══════
            applyUnlockedState(holder, level);
        }

        // --- Click handling ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLevelClick(level);
            }
        });
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    // ─────────────────────────────────────────────────────────────
    // Visual state helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * LOCKED: dimmed out, dark stroke, lock icon, no progress.
     */
    private void applyLockedState(@NonNull LevelViewHolder holder) {
        ItemLevelCardBinding b = holder.binding;
        int ctx = holder.itemView.getContext().hashCode(); // unused

        // Card stroke → dark gray
        b.cardRoot.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_stroke_locked));
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1));
        b.cardRoot.setCardElevation(2f);

        // Lock icon
        b.ivLockIcon.setImageResource(R.drawable.ic_lock);

        // Dim all content
        b.lottieIcon.setAlpha(0.2f);
        b.tvLevelTitle.setAlpha(0.4f);
        b.tvLevelSubtitle.setAlpha(0.3f);
        b.tvLevelNumber.setAlpha(0.4f);
        b.tvLevelType.setAlpha(0.3f);

        // Progress hidden
        b.progressLevel.setProgress(0);
        b.tvProgressPercent.setText("🔒");
        b.tvProgressPercent.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_text_muted));
    }

    /**
     * UNLOCKED: neon green glow border, full opacity, green progress bar.
     */
    private void applyUnlockedState(@NonNull LevelViewHolder holder, Level level) {
        ItemLevelCardBinding b = holder.binding;

        // Card stroke → neon green glow
        b.cardRoot.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_neon_green_bright));
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1.5f));
        b.cardRoot.setCardElevation(8f);

        // Unlock icon
        b.ivLockIcon.setImageResource(R.drawable.ic_unlock);

        // Full opacity
        b.lottieIcon.setAlpha(1.0f);
        b.tvLevelTitle.setAlpha(1.0f);
        b.tvLevelSubtitle.setAlpha(1.0f);
        b.tvLevelNumber.setAlpha(1.0f);
        b.tvLevelType.setAlpha(1.0f);

        // Green progress
        b.progressLevel.setProgressDrawable(ContextCompat.getDrawable(
                holder.itemView.getContext(), R.drawable.progress_bar_neon));
        b.progressLevel.setProgress(level.getProgressPercent());
        b.tvProgressPercent.setText(
                String.format(Locale.getDefault(), "%d%%", level.getProgressPercent()));
        b.tvProgressPercent.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_neon_green_bright));
    }

    /**
     * COMPLETED: cyan glow border, full opacity, cyan progress bar.
     */
    private void applyCompletedState(@NonNull LevelViewHolder holder, Level level) {
        ItemLevelCardBinding b = holder.binding;

        // Card stroke → cyan glow
        b.cardRoot.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_blue));
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1.5f));
        b.cardRoot.setCardElevation(8f);

        // Unlock icon (tinted cyan would be nice but we keep it simple)
        b.ivLockIcon.setImageResource(R.drawable.ic_unlock);
        b.ivLockIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_blue)));

        // Full opacity
        b.lottieIcon.setAlpha(1.0f);
        b.tvLevelTitle.setAlpha(1.0f);
        b.tvLevelSubtitle.setAlpha(1.0f);
        b.tvLevelNumber.setAlpha(1.0f);
        b.tvLevelType.setAlpha(1.0f);

        // Level number badge → cyan accent
        b.tvLevelNumber.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_blue));

        // Cyan progress
        b.progressLevel.setProgressDrawable(ContextCompat.getDrawable(
                holder.itemView.getContext(), R.drawable.progress_bar_cyan));
        b.progressLevel.setProgress(level.getProgressPercent());
        b.tvProgressPercent.setText(
                String.format(Locale.getDefault(), "%d%%", level.getProgressPercent()));
        b.tvProgressPercent.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.cyber_blue));
    }

    // ─────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts a human-readable level type code to a short UI label.
     */
    private String formatLevelType(String levelType) {
        if (levelType == null) return "QUIZ";
        switch (levelType) {
            case "Interactive_CMD":  return "⌨ CMD";
            case "Interactive_UI":   return "🖥 SIM";
            case "Standard_Quiz":    return "📝 QUIZ";
            default:                 return "📝 QUIZ";
        }
    }

    /**
     * Simple dp → px conversion for setting stroke width.
     */
    private int dpToPx(@NonNull LevelViewHolder holder, float dp) {
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        final ItemLevelCardBinding binding;

        LevelViewHolder(@NonNull ItemLevelCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
