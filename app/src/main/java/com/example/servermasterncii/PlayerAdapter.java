package com.example.servermasterncii;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.admin.AnalyticsActivity;
import com.example.servermasterncii.databinding.ItemPlayerStatBinding;

import java.util.List;

public class PlayerAdapter extends
        RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(AnalyticsActivity.PlayerStat player);
    }

    private final List<AnalyticsActivity.PlayerStat> players;
    private final OnPlayerClickListener listener;

    public PlayerAdapter(List<AnalyticsActivity.PlayerStat> players,
                         OnPlayerClickListener listener) {
        this.players  = players;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlayerStatBinding binding = ItemPlayerStatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PlayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        holder.bind(players.get(position), position + 1, listener);
    }

    @Override
    public int getItemCount() { return players.size(); }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlayerStatBinding b;

        PlayerViewHolder(ItemPlayerStatBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(AnalyticsActivity.PlayerStat player, int rank,
                  OnPlayerClickListener listener) {

            // ── Rank badge ────────────────────────────────────────
            b.tvRank.setText("#" + rank);
            if      (rank == 1) b.tvRank.setTextColor(0xFFFFE600);
            else if (rank == 2) b.tvRank.setTextColor(0xFF00F5FF);
            else if (rank == 3) b.tvRank.setTextColor(0xFFFF2D78);
            else                b.tvRank.setTextColor(0xFF6B6B99);

            // ── Player identity ───────────────────────────────────
            b.tvPlayerName.setText(player.displayName.toUpperCase());
            b.tvPlayerEmail.setText(player.email);

            // ── Mission count ─────────────────────────────────────
            int totalMissions = player.totalMissions > 0 ? player.totalMissions : 21;
            b.tvMissionsCompleted.setText(
                    player.completedMissions + " of " + totalMissions
                            + " missions complete");

            // ── Skill points ──────────────────────────────────────
            b.tvAvgScore.setText(player.skillPoints + " pts");
            if      (player.skillPoints >= 100) b.tvAvgScore.setTextColor(0xFFFFE600);
            else if (player.skillPoints >= 50)  b.tvAvgScore.setTextColor(0xFF00F5FF);
            else                                b.tvAvgScore.setTextColor(0xFF00FF9F);

            // ── Mastery % ─────────────────────────────────────────
            int mastery = totalMissions > 0
                    ? Math.min(100, (int) ((player.completedMissions
                    / (float) totalMissions) * 100))
                    : 0;

            // Set progress bar — must use setProgress() AND tint programmatically
            // because android:progressTint is unreliable pre-API 23
            b.progressPlayer.setMax(100);
            b.progressPlayer.setProgress(mastery);
            b.progressPlayer.getProgressDrawable()
                    .setColorFilter(0xFF00FF9F, PorterDuff.Mode.SRC_IN);

            b.tvMasteryPercent.setText(mastery + "%");

            // ── Click ──────────────────────────────────────────────
            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onPlayerClick(player);
            });
        }
    }
}