package com.example.servermasterncii;

import android.content.Context;
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

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {

    public interface OnLevelClickListener {
        void onLevelClick(Level level);
    }

    private final List<Level> levels;
    private final OnLevelClickListener listener;

    public LevelAdapter(List<Level> levels, OnLevelClickListener listener) {
        this.levels   = levels;
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
        Context ctx = holder.itemView.getContext();

        b.tvLevelNumber.setText(formatMissionLabel(level.getLevelId()));
        b.tvLevelTitle.setText(level.getTitle());
        b.tvLevelSubtitle.setText(level.getSubtitle());
        b.tvLevelType.setText(formatLevelType(level.getLevelType()));

        b.lottieIcon.setAnimation(level.getLottieAsset());
        b.lottieIcon.playAnimation();

        if (!level.isUnlocked()) {
            applyLockedState(holder, ctx);
        } else if (level.isCompleted()) {
            applyCompletedState(holder, level, ctx);
        } else {
            applyUnlockedState(holder, level, ctx);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLevelClick(level);
        });
    }

    @Override
    public int getItemCount() { return levels.size(); }

    // ── Visual states ─────────────────────────────────────────────

    private void applyLockedState(@NonNull LevelViewHolder holder, Context ctx) {
        ItemLevelCardBinding b = holder.binding;

        b.cardRoot.setStrokeColor(ThemeColors.strokeLocked(ctx));
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1));
        b.cardRoot.setCardElevation(2f);
        b.cardRoot.setCardBackgroundColor(ThemeColors.bgCard(ctx));

        b.ivLockIcon.setImageResource(R.drawable.ic_lock);
        b.ivLockIcon.setImageTintList(
                ColorStateList.valueOf(ThemeColors.textMuted(ctx)));

        // Dim lottie container + text
        b.lottieBg.setAlpha(0.25f);
        b.tvLevelTitle.setAlpha(0.4f);
        b.tvLevelSubtitle.setAlpha(0.3f);
        b.tvLevelNumber.setAlpha(0.4f);
        b.tvLevelType.setAlpha(0.3f);

        b.progressLevel.setProgress(0);
        b.tvProgressPercent.setText("🔒");
        b.tvProgressPercent.setTextColor(ThemeColors.textMuted(ctx));
    }

    private void applyUnlockedState(@NonNull LevelViewHolder holder,
                                    Level level, Context ctx) {
        ItemLevelCardBinding b = holder.binding;

        b.cardRoot.setStrokeColor(ThemeColors.accent(ctx));
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1.5f));
        b.cardRoot.setCardElevation(8f);
        b.cardRoot.setCardBackgroundColor(ThemeColors.bgCard(ctx));

        b.ivLockIcon.setImageResource(R.drawable.ic_unlock);
        b.ivLockIcon.setImageTintList(
                ColorStateList.valueOf(ThemeColors.accent(ctx)));

        b.lottieBg.setAlpha(1.0f);
        b.tvLevelTitle.setAlpha(1.0f);
        b.tvLevelSubtitle.setAlpha(1.0f);
        b.tvLevelNumber.setAlpha(1.0f);
        b.tvLevelType.setAlpha(1.0f);

        b.tvLevelTitle.setTextColor(ThemeColors.textPrimary(ctx));
        b.tvLevelSubtitle.setTextColor(ThemeColors.textSecondary(ctx));
        b.tvLevelNumber.setTextColor(ThemeColors.accent(ctx));

        b.progressLevel.setProgressDrawable(
                ContextCompat.getDrawable(ctx, R.drawable.progress_bar_neon));
        b.progressLevel.setProgress(level.getProgressPercent());
        b.tvProgressPercent.setText(
                String.format(Locale.getDefault(), "%d%%", level.getProgressPercent()));
        b.tvProgressPercent.setTextColor(ThemeColors.accent(ctx));
    }

    private void applyCompletedState(@NonNull LevelViewHolder holder,
                                     Level level, Context ctx) {
        ItemLevelCardBinding b = holder.binding;

        int cyan = ContextCompat.getColor(ctx, R.color.cyber_blue);

        b.cardRoot.setStrokeColor(cyan);
        b.cardRoot.setStrokeWidth(dpToPx(holder, 1.5f));
        b.cardRoot.setCardElevation(8f);
        b.cardRoot.setCardBackgroundColor(ThemeColors.bgCard(ctx));

        b.ivLockIcon.setImageResource(R.drawable.ic_unlock);
        b.ivLockIcon.setImageTintList(ColorStateList.valueOf(cyan));

        b.lottieBg.setAlpha(1.0f);
        b.tvLevelTitle.setAlpha(1.0f);
        b.tvLevelSubtitle.setAlpha(1.0f);
        b.tvLevelNumber.setAlpha(1.0f);
        b.tvLevelType.setAlpha(1.0f);

        b.tvLevelTitle.setTextColor(ThemeColors.textPrimary(ctx));
        b.tvLevelSubtitle.setTextColor(ThemeColors.textSecondary(ctx));
        b.tvLevelNumber.setTextColor(cyan);

        b.progressLevel.setProgressDrawable(
                ContextCompat.getDrawable(ctx, R.drawable.progress_bar_cyan));
        b.progressLevel.setProgress(level.getProgressPercent());
        b.tvProgressPercent.setText(
                String.format(Locale.getDefault(), "%d%%", level.getProgressPercent()));
        b.tvProgressPercent.setTextColor(cyan);
    }

    // ── Utilities ─────────────────────────────────────────────────

    private String formatMissionLabel(String levelId) {
        if (levelId == null) return "MISSION";
        if (levelId.startsWith("mission_")) {
            return "MISSION " + levelId.substring(8).replace("_", ".");
        }
        return "MISSION " + levelId;
    }

    private String formatLevelType(String levelType) {
        if (levelType == null) return "📝 QUIZ";
        switch (levelType) {
            case "Interactive_CMD": return "⌨ CMD";
            case "Interactive_UI":  return "🖥 SIM";
            default:                return "📝 QUIZ";
        }
    }

    private int dpToPx(@NonNull LevelViewHolder holder, float dp) {
        float density = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        final ItemLevelCardBinding binding;
        LevelViewHolder(@NonNull ItemLevelCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}