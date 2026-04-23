package com.example.servermasterncii.admin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityChapterMissionsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChapterMissionsActivity extends AppCompatActivity {

    private ActivityChapterMissionsBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityChapterMissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        String uid          = getIntent().getStringExtra("uid");
        String chapterId    = getIntent().getStringExtra("chapterId");
        String chapterTitle = getIntent().getStringExtra("chapterTitle");

        if (chapterTitle != null) {
            binding.toolbar.setTitle(chapterTitle.toUpperCase());
        }

        if (uid != null && chapterId != null) {
            loadMissions(uid, chapterId);
        }
    }

    private void loadMissions(String uid, String chapterId) {
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        db.collection("progress")
                .document(uid)
                .collection("missions")
                .whereEqualTo("chapterId", chapterId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    binding.loadingOverlay.setVisibility(View.GONE);

                    List<PlayerDetailActivity.MissionStat> missions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        PlayerDetailActivity.MissionStat stat =
                                new PlayerDetailActivity.MissionStat();
                        stat.missionId  = doc.getString("missionId");
                        stat.chapterId  = doc.getString("chapterId");
                        stat.score      = doc.getLong("score") != null
                                ? doc.getLong("score").intValue() : 0;
                        stat.total      = doc.getLong("total") != null
                                ? doc.getLong("total").intValue() : 0;
                        stat.percentage = doc.getLong("percentage") != null
                                ? doc.getLong("percentage").intValue() : 0;
                        stat.completed  = Boolean.TRUE.equals(
                                doc.getBoolean("completed"));
                        missions.add(stat);
                    }

                    if (missions.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.recyclerMissions.setVisibility(View.GONE);
                        return;
                    }

                    MissionStatAdapter adapter = new MissionStatAdapter(missions);
                    binding.recyclerMissions.setLayoutManager(
                            new LinearLayoutManager(this));
                    binding.recyclerMissions.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    android.widget.Toast.makeText(this,
                            "Failed to load: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}