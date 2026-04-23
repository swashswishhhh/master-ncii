package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.servermasterncii.PlayerAdapter;
import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityAnalyticsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;
    private FirebaseFirestore db;
    private PlayerAdapter playerAdapter;
    private final List<PlayerStat> playerStats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        loadAnalytics();
    }

    private void setupRecyclerView() {
        playerAdapter = new PlayerAdapter(playerStats, player -> {
            // Drill into individual player
            Intent intent = new Intent(this, com.example.servermasterncii.admin.PlayerDetailActivity.class);
            intent.putExtra("uid",         player.uid);
            intent.putExtra("displayName", player.displayName);
            startActivity(intent);
        });
        binding.recyclerPlayers.setLayoutManager(
                new LinearLayoutManager(this));
        binding.recyclerPlayers.setAdapter(playerAdapter);
    }

    private void loadAnalytics() {
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        db.collection("progress")
                .orderBy("totalSkillPoints",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    playerStats.clear();

                    Map<String, Integer> missionFailCounts = new HashMap<>();
                    int[] totalCompleted = {0};
                    int[] pending        = {snapshot.size()};

                    if (snapshot.isEmpty()) {
                        binding.loadingOverlay.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot userDoc : snapshot) {
                        String uid         = userDoc.getId();
                        String displayName = userDoc.getString("displayName");
                        String email       = userDoc.getString("email");
                        int skillPoints    = userDoc.getLong("totalSkillPoints") != null
                                ? userDoc.getLong("totalSkillPoints").intValue() : 0;
                        int missionsPlayed = userDoc.getLong("totalMissionsPlayed") != null
                                ? userDoc.getLong("totalMissionsPlayed").intValue() : 0;
                        int missionsCompleted = userDoc.getLong("totalMissionsCompleted") != null
                                ? userDoc.getLong("totalMissionsCompleted").intValue() : 0;

                        totalCompleted[0] += missionsCompleted;

                        db.collection("progress")
                                .document(uid)
                                .collection("missions")
                                .get()
                                .addOnSuccessListener(missionSnap -> {
                                    int userAvgScore = 0;

                                    for (QueryDocumentSnapshot missionDoc : missionSnap) {
                                        String missionId = missionDoc.getString("missionId");
                                        Boolean completed = missionDoc.getBoolean("completed");
                                        Long percentage   = missionDoc.getLong("percentage");

                                        if (missionId != null && !Boolean.TRUE.equals(completed)) {
                                            missionFailCounts.merge(missionId, 1, Integer::sum);
                                        }
                                        if (percentage != null) {
                                            userAvgScore += percentage.intValue();
                                        }
                                    }

                                    int avgScore = missionSnap.size() > 0
                                            ? userAvgScore / missionSnap.size() : 0;

                                    // With:
                                    PlayerStat stat = new PlayerStat(
                                            uid,
                                            displayName != null ? displayName : "Unknown",
                                            email != null ? email : "",
                                            missionsCompleted,
                                            21,               // ← always 21 base missions
                                            avgScore,
                                            skillPoints
                                    );
                                    playerStats.add(stat);

                                    pending[0]--;
                                    if (pending[0] == 0) {
                                        runOnUiThread(() -> {
                                            binding.loadingOverlay.setVisibility(View.GONE);

                                            // Sort by skill points (leaderboard)
                                            playerStats.sort((a, b) ->
                                                    b.skillPoints - a.skillPoints);

                                            playerAdapter.notifyDataSetChanged();
                                            updateOverviewStats(
                                                    snapshot.size(),
                                                    totalCompleted[0],
                                                    missionFailCounts
                                            );
                                        });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    android.widget.Toast.makeText(this,
                            "Failed to load analytics: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
    }

    private void updateOverviewStats(int totalUsers,
                                     int totalCompleted,
                                     Map<String, Integer> failCounts) {
        binding.tvTotalPlayers.setText(String.valueOf(totalUsers));
        binding.tvTotalCompleted.setText(String.valueOf(totalCompleted));

        // Find most failed mission
        String hardestMission = "None";
        int maxFails = 0;
        for (Map.Entry<String, Integer> entry : failCounts.entrySet()) {
            if (entry.getValue() > maxFails) {
                maxFails       = entry.getValue();
                hardestMission = entry.getKey();
            }
        }
        binding.tvHardestMission.setText(
                hardestMission.replace("mission_", "Mission ")
                        .replace("_", "."));
        binding.tvHardestMissionFails.setText(maxFails + " fails");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // ── PlayerStat model ──────────────────────────────────────────
    public static class PlayerStat {
        public String uid, displayName, email;
        public int completedMissions, totalMissions, avgScore, skillPoints;

        public PlayerStat(String uid, String displayName, String email,
                          int completedMissions, int totalMissions,
                          int avgScore, int skillPoints) {
            this.uid               = uid;
            this.displayName       = displayName;
            this.email             = email;
            this.completedMissions = completedMissions;
            this.totalMissions     = totalMissions;
            this.avgScore          = avgScore;
            this.skillPoints       = skillPoints;  // ← new
        }
    }
}