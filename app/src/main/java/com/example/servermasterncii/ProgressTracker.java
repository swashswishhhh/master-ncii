package com.example.servermasterncii;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * ProgressTracker — syncs per-mission results to Firestore progress/{uid}
 * which is what AnalyticsActivity reads for the admin leaderboard.
 *
 * Skill points = sum of best scores across all missions.
 * On each sync we recompute totalSkillPoints by querying all mission docs,
 * so the number always matches what the student sees locally.
 */
public class ProgressTracker {

    private static final String TAG = "ProgressTracker";

    public static void syncMissionResult(String missionId,
                                         String chapterId,
                                         int score,
                                         int total) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        int percentage = total > 0 ? (int) ((score / (float) total) * 100) : 0;
        boolean completed = percentage >= 70;

        DocumentReference missionRef = db.collection("progress")
                .document(uid)
                .collection("missions")
                .document(missionId);

        missionRef.get().addOnSuccessListener(existingDoc -> {

            // ── Read previous best ────────────────────────────────
            int previousBestScore = 0;
            int previousBestPct   = 0;
            boolean wasCompleted  = false;

            if (existingDoc.exists()) {
                if (existingDoc.getLong("score") != null)
                    previousBestScore = existingDoc.getLong("score").intValue();
                if (existingDoc.getLong("percentage") != null)
                    previousBestPct = existingDoc.getLong("percentage").intValue();
                if (existingDoc.getBoolean("completed") != null)
                    wasCompleted = Boolean.TRUE.equals(existingDoc.getBoolean("completed"));
            }

            boolean isNewMission    = !existingDoc.exists();
            boolean isNewCompletion = completed && !wasCompleted;
            boolean isNewBest       = score > previousBestScore;

            int bestScore = Math.max(score, previousBestScore);
            int bestPct   = Math.max(percentage, previousBestPct);

            // ── Step 1: Write mission subcollection doc ───────────
            Map<String, Object> missionDoc = new HashMap<>();
            missionDoc.put("missionId",  missionId);
            missionDoc.put("chapterId",  chapterId);
            missionDoc.put("score",      bestScore);
            missionDoc.put("total",      total);
            missionDoc.put("percentage", bestPct);
            missionDoc.put("completed",  wasCompleted || completed);
            missionDoc.put("lastPlayed", Timestamp.now());

            missionRef.set(missionDoc, SetOptions.merge())
                    .addOnSuccessListener(v -> {

                        // ── Step 2: Recount totalSkillPoints from ALL missions ──
                        // This guarantees analytics matches student's local score
                        recomputeAndSaveRootDoc(uid, db, user,
                                isNewMission, isNewCompletion);

                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "❌ Mission doc write failed", e));

        }).addOnFailureListener(e ->
                Log.e(TAG, "❌ Failed to read mission doc", e));
    }

    // ─────────────────────────────────────────────────────────────
    // Recount by summing ALL mission best scores — matches local prefs
    // ─────────────────────────────────────────────────────────────

    private static void recomputeAndSaveRootDoc(String uid,
                                                FirebaseFirestore db,
                                                FirebaseUser user,
                                                boolean isNewMission,
                                                boolean isNewCompletion) {
        db.collection("progress")
                .document(uid)
                .collection("missions")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int totalSkillPoints    = 0;
                    int totalMissionsPlayed = 0;
                    int totalCompleted      = 0;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        Long s = doc.getLong("score");
                        if (s != null) totalSkillPoints += s.intValue();

                        totalMissionsPlayed++;

                        Boolean comp = doc.getBoolean("completed");
                        if (Boolean.TRUE.equals(comp)) totalCompleted++;
                    }

                    // ── Write root progress doc ───────────────────
                    Map<String, Object> rootDoc = new HashMap<>();
                    rootDoc.put("displayName", user.getDisplayName() != null
                            ? user.getDisplayName() : "Unknown");
                    rootDoc.put("email",                 user.getEmail());
                    rootDoc.put("lastActive",            Timestamp.now());
                    rootDoc.put("totalSkillPoints",      totalSkillPoints);
                    rootDoc.put("totalMissionsPlayed",   totalMissionsPlayed);
                    rootDoc.put("totalMissionsCompleted",totalCompleted);

                    int finalTotalSkillPoints = totalSkillPoints;
                    int finalTotalCompleted = totalCompleted;
                    db.collection("progress")
                            .document(uid)
                            .set(rootDoc, SetOptions.merge())
                            .addOnSuccessListener(v ->
                                    Log.d(TAG, "✅ Root doc updated: "
                                            + finalTotalSkillPoints + " pts | "
                                            + finalTotalCompleted + " completed"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "❌ Root doc write failed", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Failed to recount missions", e));
    }
}