package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityPlayerDetailBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlayerDetailActivity extends AppCompatActivity {

    private ActivityPlayerDetailBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private String displayName;

    // Chapter data
    private static final String[] CHAPTER_IDS = {
            "chapter_1", "chapter_2", "chapter_3"
    };
    private static final String[] CHAPTER_TITLES = {
            "Chapter 1 — User Access",
            "Chapter 2 — Server Setup",
            "Chapter 3 — Maintenance"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityPlayerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db  = FirebaseFirestore.getInstance();
        uid = getIntent().getStringExtra("uid");
        displayName = getIntent().getStringExtra("displayName");

        if (displayName != null) {
            binding.tvPlayerName.setText(displayName.toUpperCase());
        }

        loadPlayerOverview();
        setupChapterCards();
    }

    // ── Load overall stats for header ─────────────────────────────
    private void loadPlayerOverview() {
        if (uid == null) return;

        db.collection("progress").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    int skillPoints = doc.getLong("totalSkillPoints") != null
                            ? doc.getLong("totalSkillPoints").intValue() : 0;
                    int completed   = doc.getLong("totalMissionsCompleted") != null
                            ? doc.getLong("totalMissionsCompleted").intValue() : 0;
                    int played      = doc.getLong("totalMissionsPlayed") != null
                            ? doc.getLong("totalMissionsPlayed").intValue() : 0;

                    // Overall mastery = completed / total missions (21)
                    int totalMissions = 21;
                    int mastery = (int) ((completed / (float) totalMissions) * 100);

                    binding.tvSkillPoints.setText(String.valueOf(skillPoints));
                    binding.tvMissionsComplete.setText(
                            completed + " of " + totalMissions + " missions complete");
                    binding.progressMastery.setProgress(mastery);
                    binding.tvMasteryPercent.setText(mastery + "%");
                });
    }

    // ── Chapter cards ─────────────────────────────────────────────
    private void setupChapterCards() {
        binding.cardChapter1.setOnClickListener(v ->
                launchChapterMissions(0));
        binding.cardChapter2.setOnClickListener(v ->
                launchChapterMissions(1));
        binding.cardChapter3.setOnClickListener(v ->
                launchChapterMissions(2));
    }

    private void launchChapterMissions(int chapterIndex) {
        Intent intent = new Intent(this,
                com.example.servermasterncii.admin.ChapterMissionsActivity.class);
        intent.putExtra("uid",         uid);
        intent.putExtra("chapterId",   CHAPTER_IDS[chapterIndex]);
        intent.putExtra("chapterTitle",CHAPTER_TITLES[chapterIndex]);
        intent.putExtra("displayName", displayName);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // ── MissionStat model ─────────────────────────────────────────
    public static class MissionStat {
        public String missionId, chapterId;
        public int score, total, percentage;
        public boolean completed;
    }
}