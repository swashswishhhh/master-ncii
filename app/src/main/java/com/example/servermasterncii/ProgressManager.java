package com.example.servermasterncii;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * ProgressManager — handles syncing quiz progress between
 * SharedPreferences (local) and Firestore (cloud).
 *
 * Rules:
 * - Guest users: SharedPreferences only, no Firestore
 * - Logged-in users: both SharedPreferences + Firestore
 * - Higher score always wins on conflict
 *
 * Firestore path: users/{uid}/progress/{levelId}
 * Fields: score (int), total (int), unlocked (boolean)
 */
public class ProgressManager {

    private static final String TAG = "ProgressManager";

    // Firestore collection path
    private static final String COLLECTION_USERS    = "users";
    private static final String COLLECTION_PROGRESS = "progress";

    // SharedPreferences keys
    private static final String KEY_SCORE    = "score_";
    private static final String KEY_TOTAL    = "total_";
    private static final String KEY_UNLOCKED = "unlocked_";

    public interface OnSyncCompleteListener {
        void onComplete();
        void onError(String message);
    }

    // =====================================================================
    // Save progress (local + cloud)
    // =====================================================================

    /**
     * Saves progress for a single level.
     * Always writes to SharedPreferences.
     * Only writes to Firestore if user is logged in (not guest).
     * Higher score always wins.
     */
    public static void saveProgress(SharedPreferences prefs,
                                    String levelId,
                                    int score,
                                    int total,
                                    boolean unlockNext,
                                    String nextLevelId) {
        if (levelId == null || levelId.isEmpty() || total == 0) return;

        int previousBest = prefs.getInt(KEY_SCORE + levelId, 0);

        // Only update if new score is better
        if (score <= previousBest) return;

        // ── Write to SharedPreferences ────────────────────────────
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_SCORE + levelId, score);
        editor.putInt(KEY_TOTAL + levelId, total);

        double pct = score / (double) total;
        if (pct >= 0.70) {
            editor.putBoolean("completed_" + levelId, true);
            if (unlockNext && nextLevelId != null) {
                editor.putBoolean(KEY_UNLOCKED + nextLevelId, true);
            }
        }
        editor.apply();

        // ── Write to Firestore (logged-in users only) ─────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("score",    score);
        data.put("total",    total);
        data.put("unlocked", pct >= 0.70);
        if (unlockNext && nextLevelId != null && pct >= 0.70) {
            data.put("nextLevelUnlocked", nextLevelId);
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(user.getUid())
                .collection(COLLECTION_PROGRESS)
                .document(levelId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "✅ Progress synced to Firestore for " + levelId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Firestore sync failed for " + levelId + ": " + e.getMessage()));
    }

    /**
     * Saves simulator completion (score=1, total=1).
     */
    public static void saveSimulatorProgress(SharedPreferences prefs,
                                             String levelId,
                                             String nextLevelId) {
        saveProgress(prefs, levelId, 1, 1, true, nextLevelId);
    }

    // =====================================================================
    // Pull progress from Firestore → SharedPreferences
    // =====================================================================

    /**
     * Pulls all progress from Firestore and writes it to SharedPreferences.
     * Called once on MainActivity.onCreate() for logged-in users.
     * Higher score always wins.
     */
    public static void pullFromFirestore(SharedPreferences prefs,
                                         OnSyncCompleteListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Guest or not logged in — nothing to pull
        if (user == null || user.isAnonymous()) {
            if (listener != null) listener.onComplete();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(user.getUid())
                .collection(COLLECTION_PROGRESS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    SharedPreferences.Editor editor = prefs.edit();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String levelId = doc.getId();

                        Long firestoreScore = doc.getLong("score");
                        Long firestoreTotal = doc.getLong("total");
                        Boolean firestoreUnlocked = doc.getBoolean("unlocked");
                        String nextLevelUnlocked = doc.getString("nextLevelUnlocked");

                        int localScore = prefs.getInt(KEY_SCORE + levelId, 0);
                        int fsScore    = firestoreScore != null ? firestoreScore.intValue() : 0;
                        int fsTotal    = firestoreTotal != null ? firestoreTotal.intValue() : 0;

                        // Higher score wins
                        if (fsScore > localScore && fsTotal > 0) {
                            editor.putInt(KEY_SCORE + levelId, fsScore);
                            editor.putInt(KEY_TOTAL + levelId, fsTotal);
                            Log.d(TAG, "Pulled better score for " + levelId
                                    + ": " + fsScore + "/" + fsTotal);
                        }

                        // Unlock state — if Firestore says unlocked, trust it
                        if (Boolean.TRUE.equals(firestoreUnlocked)) {
                            editor.putBoolean("completed_" + levelId, true);
                        }

                        // Unlock next level if Firestore recorded it
                        if (nextLevelUnlocked != null && !nextLevelUnlocked.isEmpty()) {
                            editor.putBoolean(KEY_UNLOCKED + nextLevelUnlocked, true);
                        }
                    }

                    editor.apply();
                    Log.d(TAG, "✅ Pulled " + snapshot.size() + " progress records from Firestore");

                    if (listener != null) listener.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to pull progress from Firestore: " + e.getMessage());
                    // Still complete — fall back to local data
                    if (listener != null) listener.onComplete();
                });
    }

    // =====================================================================
    // Push all local progress to Firestore
    // =====================================================================

    /**
     * Pushes all local SharedPreferences progress to Firestore.
     * Called after login if Firestore has no data yet.
     */
    public static void pushAllToFirestore(SharedPreferences prefs, String[] allLevelIds) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String levelId : allLevelIds) {
            int score = prefs.getInt(KEY_SCORE + levelId, 0);
            int total = prefs.getInt(KEY_TOTAL + levelId, 0);
            if (total == 0) continue; // never played, skip

            boolean unlocked = prefs.getBoolean("completed_" + levelId, false);

            Map<String, Object> data = new HashMap<>();
            data.put("score",    score);
            data.put("total",    total);
            data.put("unlocked", unlocked);

            db.collection(COLLECTION_USERS)
                    .document(user.getUid())
                    .collection(COLLECTION_PROGRESS)
                    .document(levelId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v ->
                            Log.d(TAG, "Pushed local progress for " + levelId))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to push " + levelId + ": " + e.getMessage()));
        }
    }
}