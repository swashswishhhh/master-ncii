package com.example.servermasterncii.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.R;
import com.example.servermasterncii.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * MissionManagerActivity — Admin screen to view, and delete missions.
 *
 * Deleting a mission:
 *  1. Deletes all questions in Firestore where missionId == mission.missionId
 *  2. Deletes the mission document itself from Firestore
 *  3. Removes the card from the student's main screen immediately
 *     (MainActivity.loadFirestoreMissions() re-queries on next launch/resume)
 */
public class MissionManagerActivity extends AppCompatActivity
        implements MissionManagerAdapter.OnMissionActionListener {

    private RecyclerView recyclerView;
    private View loadingOverlay;
    private View emptyState;
    private MissionManagerAdapter adapter;
    private final List<MissionItem> missionList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_manager);

        db = FirebaseFirestore.getInstance();

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("MANAGE MISSIONS");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView  = findViewById(R.id.recyclerMissions);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        emptyState    = findViewById(R.id.emptyState);

        adapter = new MissionManagerAdapter(missionList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadMissions();
    }

    // =====================================================================
    // Load all missions from Firestore
    // =====================================================================

    private void loadMissions() {
        setLoading(true);

        db.collection("missions")
                .orderBy("chapterId")
                .get()
                .addOnSuccessListener(snapshot -> {
                    missionList.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        MissionItem item = new MissionItem();
                        item.firestoreDocId = doc.getId();
                        item.missionId      = doc.getString("missionId");
                        item.title          = doc.getString("title");
                        item.chapterId      = doc.getString("chapterId");
                        item.description    = doc.getString("description");
                        item.difficulty     = doc.getString("difficulty");
                        Boolean pub         = doc.getBoolean("published");
                        item.published      = Boolean.TRUE.equals(pub);

                        if (item.missionId != null && item.title != null) {
                            missionList.add(item);
                        }
                    }

                    setLoading(false);
                    adapter.notifyDataSetChanged();
                    emptyState.setVisibility(missionList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(missionList.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to load missions: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // =====================================================================
    // Adapter callbacks
    // =====================================================================

    @Override
    public void onDeleteMission(MissionItem mission, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("⚠ DELETE MISSION")
                .setMessage("Delete \"" + mission.title + "\"?\n\n"
                        + "This will permanently remove:\n"
                        + "• The mission card from the student map\n"
                        + "• All questions belonging to this mission\n\n"
                        + "This action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) ->
                        performDelete(mission, position))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onTogglePublish(MissionItem mission, int position) {
        boolean newState = !mission.published;
        setLoading(true);

        db.collection("missions")
                .document(mission.firestoreDocId)
                .update("published", newState)
                .addOnSuccessListener(unused -> {
                    mission.published = newState;
                    adapter.notifyItemChanged(position);
                    setLoading(false);
                    Toast.makeText(this,
                            newState ? "✅ Mission published" : "Mission unpublished",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // =====================================================================
    // Delete logic — questions first, then mission doc
    // =====================================================================

    private void performDelete(MissionItem mission, int position) {
        setLoading(true);

        // Step 1: delete all questions where missionId == mission.missionId
        db.collection("questions")
                .whereEqualTo("missionId", mission.missionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Batch delete all matching questions
                    if (snapshot.isEmpty()) {
                        // No questions — skip straight to deleting the mission
                        deleteMissionDoc(mission, position);
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                // Step 2: delete the mission document
                                deleteMissionDoc(mission, position);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "Failed to delete questions: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to query questions: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void deleteMissionDoc(MissionItem mission, int position) {
        db.collection("missions")
                .document(mission.firestoreDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    // Remove from list immediately — student map updates on next resume
                    missionList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, missionList.size());

                    if (missionList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }

                    Toast.makeText(this,
                            "✅ Mission and all questions deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to delete mission: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // =====================================================================
    // UI helpers
    // =====================================================================

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    // =====================================================================
    // Mission data model (Firestore-only, no Room needed here)
    // =====================================================================

    public static class MissionItem {
        public String firestoreDocId;
        public String missionId;
        public String title;
        public String chapterId;
        public String description;
        public String difficulty;
        public boolean published;
    }
}