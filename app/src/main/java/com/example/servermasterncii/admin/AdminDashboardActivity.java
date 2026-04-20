package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.R;
import com.example.servermasterncii.admin.model.AdminMission;
import com.example.servermasterncii.databinding.ActivityAdminDashboardBinding;
import com.example.servermasterncii.db.AppDatabase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminDashboardActivity â€” Mission management hub.
 *
 * Shows all admin-authored missions as cards with sync status badges.
 * FAB opens the MissionEditorActivity for creating a new mission.
 * Each card has Edit, Publish, and Delete action buttons.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_MISSION_ID = "extra_mission_id";

    private ActivityAdminDashboardBinding binding;
    private AdminViewModel viewModel;
    private MissionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        adapter = new MissionAdapter();
        binding.recyclerMissions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMissions.setAdapter(adapter);

        viewModel.getAllMissions().observe(this, missions -> {
            adapter.setMissions(missions);
            binding.emptyState.setVisibility(missions.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerMissions.setVisibility(missions.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getPublishResult().observe(this, result -> {
            if (result.state == AdminViewModel.PublishState.SUCCESS) {
                showSnackbar(result.message, false);
                viewModel.resetPublishState();
            } else if (result.state == AdminViewModel.PublishState.ERROR) {
                showSnackbar(result.message, true);
                viewModel.resetPublishState();
            }
        });

        binding.fabAddMission.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMissionEditorActivity.class)));

        binding.btnPublishAll.setOnClickListener(v -> {
            List<AdminMission> missions = adapter.getMissions();
            for (AdminMission m : missions) {
                if (AdminMission.STATUS_DRAFT.equals(m.syncStatus)) {
                    viewModel.publishMission(m.id);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void showSnackbar(String message, boolean isError) {
        if (binding == null) return;
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(isError ? 0xFFF85149 : 0xFF3FB950)
                .setTextColor(0xFFFFFFFF)
                .show();
    }

    // â”€â”€ Mission RecyclerView Adapter â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.VH> {

        private final List<AdminMission> missions = new ArrayList<>();

        void setMissions(List<AdminMission> list) {
            missions.clear();
            missions.addAll(list);
            notifyDataSetChanged();
        }

        List<AdminMission> getMissions() { return missions; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_mission, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AdminMission m = missions.get(position);
            h.tvTitle.setText(m.title != null ? m.title : "Untitled");
            h.tvDifficulty.setText(m.difficulty != null ? m.difficulty : "");
            h.tvMechanic.setText(m.mechanicType != null ? m.mechanicType : "");

            AppDatabase.databaseWriteExecutor.execute(() -> {
                int count = AppDatabase.getDatabase(AdminDashboardActivity.this)
                        .adminQuestionDao().countForMission(m.id);
                runOnUiThread(() -> h.tvCount.setText(
                        count + " question" + (count == 1 ? "" : "s")));
            });

            boolean synced = AdminMission.STATUS_SYNCED.equals(m.syncStatus);
            h.tvSync.setText(synced ? "SYNCED" : "DRAFT");
            h.tvSync.setTextColor(synced ? 0xFF3FB950 : 0xFFE3B341);
            h.tvSync.setBackground(ContextCompat.getDrawable(AdminDashboardActivity.this,
                    synced ? R.drawable.bg_sync_badge_synced : R.drawable.bg_sync_badge_draft));
            h.statusBar.setBackgroundColor(synced ? 0xFF3FB950 : 0xFF58A6FF);

            h.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this,
                        AdminMissionEditorActivity.class);
                intent.putExtra(EXTRA_MISSION_ID, m.id);
                startActivity(intent);
            });

            h.btnPublish.setOnClickListener(v -> viewModel.publishMission(m.id));

            h.btnDelete.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(AdminDashboardActivity.this)
                            .setTitle("Delete Mission")
                            .setMessage("Delete \"" + m.title + "\"? This cannot be undone.")
                            .setPositiveButton("DELETE", (d, w) -> viewModel.deleteMission(m))
                            .setNegativeButton("CANCEL", null)
                            .show());
        }

        @Override
        public int getItemCount() { return missions.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCount, tvDifficulty, tvMechanic, tvSync;
            View statusBar;
            ImageButton btnEdit, btnPublish, btnDelete;

            VH(@NonNull View v) {
                super(v);
                tvTitle      = v.findViewById(R.id.tvMissionTitle);
                tvCount      = v.findViewById(R.id.tvQuestionCount);
                tvDifficulty = v.findViewById(R.id.tvDifficulty);
                tvMechanic   = v.findViewById(R.id.tvMechanicType);
                tvSync       = v.findViewById(R.id.tvSyncStatus);
                statusBar    = v.findViewById(R.id.statusBar);
                btnEdit      = v.findViewById(R.id.btnEdit);
                btnPublish   = v.findViewById(R.id.btnPublish);
                btnDelete    = v.findViewById(R.id.btnDelete);
            }
        }
    }
}