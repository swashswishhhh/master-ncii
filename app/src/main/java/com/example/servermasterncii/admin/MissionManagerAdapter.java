package com.example.servermasterncii.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * RecyclerView adapter for MissionManagerActivity.
 * Each card shows mission title, chapter, status, and
 * Delete / Publish-toggle action buttons.
 */
public class MissionManagerAdapter
        extends RecyclerView.Adapter<MissionManagerAdapter.MissionViewHolder> {

    public interface OnMissionActionListener {
        void onDeleteMission(MissionManagerActivity.MissionItem mission, int position);
        void onTogglePublish(MissionManagerActivity.MissionItem mission, int position);
    }

    private final List<MissionManagerActivity.MissionItem> items;
    private final OnMissionActionListener listener;

    public MissionManagerAdapter(List<MissionManagerActivity.MissionItem> items,
                                 OnMissionActionListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mission_card, parent, false);
        return new MissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionViewHolder holder, int position) {
        MissionManagerActivity.MissionItem item = items.get(position);

        holder.tvTitle.setText(item.title != null ? item.title : "Untitled");
        holder.tvMissionId.setText(item.missionId != null ? item.missionId : "—");
        holder.tvChapter.setText(item.chapterId != null
                ? item.chapterId.replace("_", " ").toUpperCase() : "—");
        holder.tvDifficulty.setText(item.difficulty != null
                ? item.difficulty.toUpperCase() : "—");

        // Published status badge
        if (item.published) {
            holder.tvStatus.setText("● LIVE");
            holder.tvStatus.setTextColor(0xFF00FF9F);
            holder.btnTogglePublish.setText("UNPUBLISH");
            holder.btnTogglePublish.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(0xFF888888));
            holder.btnTogglePublish.setTextColor(0xFF888888);
        } else {
            holder.tvStatus.setText("○ DRAFT");
            holder.tvStatus.setTextColor(0xFFFFE600);
            holder.btnTogglePublish.setText("PUBLISH");
            holder.btnTogglePublish.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(0xFF00FF9F));
            holder.btnTogglePublish.setTextColor(0xFF00FF9F);
        }

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null) {
                listener.onDeleteMission(items.get(pos), pos);
            }
        });

        holder.btnTogglePublish.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null) {
                listener.onTogglePublish(items.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MissionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMissionId, tvChapter, tvDifficulty, tvStatus;
        MaterialButton btnDelete, btnTogglePublish;

        MissionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle          = itemView.findViewById(R.id.tvMissionTitle);
            tvMissionId      = itemView.findViewById(R.id.tvMissionId);
            tvChapter        = itemView.findViewById(R.id.tvMissionChapter);
            tvDifficulty     = itemView.findViewById(R.id.tvMissionDifficulty);
            tvStatus         = itemView.findViewById(R.id.tvMissionStatus);
            btnDelete        = itemView.findViewById(R.id.btnDeleteMission);
            btnTogglePublish = itemView.findViewById(R.id.btnTogglePublish);
        }
    }
}