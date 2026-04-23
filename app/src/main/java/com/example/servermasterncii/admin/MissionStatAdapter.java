package com.example.servermasterncii.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.databinding.ItemMissionStatBinding;

import java.util.List;

public class MissionStatAdapter extends
        RecyclerView.Adapter<MissionStatAdapter.MissionStatViewHolder> {

    private final List<PlayerDetailActivity.MissionStat> missions;

    public MissionStatAdapter(List<PlayerDetailActivity.MissionStat> missions) {
        this.missions = missions;
    }

    @NonNull
    @Override
    public MissionStatViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                    int viewType) {
        ItemMissionStatBinding binding = ItemMissionStatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MissionStatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionStatViewHolder holder,
                                 int position) {
        holder.bind(missions.get(position));
    }

    @Override
    public int getItemCount() { return missions.size(); }

    static class MissionStatViewHolder extends RecyclerView.ViewHolder {
        private final ItemMissionStatBinding binding;

        MissionStatViewHolder(ItemMissionStatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PlayerDetailActivity.MissionStat stat) {
            // Format mission ID for display
            String display = stat.missionId != null
                    ? stat.missionId.replace("mission_", "Mission ")
                    .replace("_", ".")
                    : "Unknown";
            binding.tvMissionName.setText(display);
            binding.tvScore.setText(stat.score + " / " + stat.total);
            binding.tvPercentage.setText(stat.percentage + "%");
            binding.progressMission.setProgress(stat.percentage);

            // Color code by completion
            if (stat.completed) {
                binding.tvStatus.setText("✓ PASSED");
                binding.tvStatus.setTextColor(0xFF00FF9F);
                binding.tvPercentage.setTextColor(0xFF00FF9F);
            } else {
                binding.tvStatus.setText("✗ FAILED");
                binding.tvStatus.setTextColor(0xFFFF2D78);
                binding.tvPercentage.setTextColor(0xFFFF2D78);
            }
        }
    }
}