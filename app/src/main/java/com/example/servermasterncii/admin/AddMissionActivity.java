package com.example.servermasterncii.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityAddMissionBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMissionActivity extends AppCompatActivity {

    private ActivityAddMissionBinding binding;
    private FirebaseFirestore db;

    // Your 3 chapters
    private final String[] chapterIds    = {"chapter_1", "chapter_2", "chapter_3"};
    private final String[] chapterTitles = {
            "Chapter 1 — User Access",
            "Chapter 2 — Server Setup",
            "Chapter 3 — Maintenance"
    };
    private final String[] difficulties  = {"beginner", "intermediate", "advanced"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityAddMissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();

        setupSpinners();
        setupButtons();
    }

    private void setupSpinners() {
        // Chapter spinner
        ArrayAdapter<String> chapterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, chapterTitles);
        chapterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerChapter.setAdapter(chapterAdapter);

        // Difficulty spinner
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, difficulties);
        diffAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(diffAdapter);
    }

    private void setupButtons() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnPublishMission.setOnClickListener(v -> {
            if (validateInputs()) saveMission(true);
        });

        binding.btnSaveDraft.setOnClickListener(v -> {
            if (validateInputs()) saveMission(false);
        });
    }

    private boolean validateInputs() {
        boolean valid = true;

        String title = binding.etMissionTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.etMissionTitle.setError("Mission title is required");
            valid = false;
        }

        String missionId = binding.etMissionId.getText().toString().trim();
        if (TextUtils.isEmpty(missionId)) {
            binding.etMissionId.setError("Mission ID is required (e.g. mission_1_6)");
            valid = false;
        }

        return valid;
    }

    private void saveMission(boolean published) {
        setLoading(true);

        int chapterIndex   = binding.spinnerChapter.getSelectedItemPosition();
        String chapterId   = chapterIds[chapterIndex];
        String title       = binding.etMissionTitle.getText().toString().trim();
        String missionId   = binding.etMissionId.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String difficulty  = difficulties[binding.spinnerDifficulty
                .getSelectedItemPosition()];
        String adminEmail  = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Data for top-level missions collection (saga map)
        Map<String, Object> missionData = new HashMap<>();
        missionData.put("title",       title);
        missionData.put("missionId",   missionId);
        missionData.put("chapterId",   chapterId);
        missionData.put("description", description);
        missionData.put("difficulty",  difficulty);
        missionData.put("published",   published);
        missionData.put("isFirestore", true);
        missionData.put("isLocal",     false);
        missionData.put("createdBy",   adminEmail);
        missionData.put("createdAt",   Timestamp.now());

        // Data for chapters subcollection (question form dropdown)
        Map<String, Object> subcollectionData = new HashMap<>();
        subcollectionData.put("id",        missionId);
        subcollectionData.put("title",     title);
        subcollectionData.put("order",     999);      // admin missions go at end
        subcollectionData.put("isLocal",   false);
        subcollectionData.put("isPublished", published);

        // Write to top-level missions collection
        db.collection("missions")
                .add(missionData)
                .addOnSuccessListener(ref -> {

                    // Also write to chapters subcollection
                    db.collection("chapters")
                            .document(chapterId)
                            .collection("missions")
                            .document(missionId)
                            .set(subcollectionData)
                            .addOnSuccessListener(v -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        published ? "✅ Mission published!" : "📝 Draft saved!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "❌ Failed to save to chapter: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "❌ Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPublishMission.setEnabled(!loading);
        binding.btnSaveDraft.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}