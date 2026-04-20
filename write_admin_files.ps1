$enc = [System.Text.UTF8Encoding]::new($false)
$base = $PSScriptRoot

# ── AdminViewModel ──────────────────────────────────────────────────────────
$vm = @'
package com.example.servermasterncii.admin;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.servermasterncii.admin.model.AdminMission;
import com.example.servermasterncii.admin.model.AdminQuestion;
import com.example.servermasterncii.db.AppDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AdminViewModel — manages the admin mission dashboard and publish workflow.
 *
 * <h3>Local draft flow</h3>
 * <ol>
 *   <li>Admin creates a mission (Step 1 metadata form) → saved to Room as DRAFT</li>
 *   <li>Admin adds questions (Step 2 question builder) → saved to Room</li>
 *   <li>Publish button enabled only when every question passes {@link AdminQuestion#isValid()}</li>
 * </ol>
 *
 * <h3>Publish flow</h3>
 * <ol>
 *   <li>Read all questions for the mission from Room</li>
 *   <li>Write mission + questions to Firestore under {@code missions/{docId}}</li>
 *   <li>Increment {@code meta/global_version} document's {@code version_id} field</li>
 *   <li>Update Room record: syncStatus = SYNCED, firestoreDocId = docId</li>
 * </ol>
 */
public class AdminViewModel extends AndroidViewModel {

    private static final String TAG = "AdminViewModel";

    // ── Publish state ──────────────────────────────────────────────────────
    public enum PublishState { IDLE, LOADING, SUCCESS, ERROR }

    public static final class PublishResult {
        public final PublishState state;
        public final String message;
        PublishResult(PublishState s, String m) { state = s; message = m; }
        static PublishResult idle()             { return new PublishResult(PublishState.IDLE, null); }
        static PublishResult loading()          { return new PublishResult(PublishState.LOADING, null); }
        static PublishResult success(String m)  { return new PublishResult(PublishState.SUCCESS, m); }
        static PublishResult error(String m)    { return new PublishResult(PublishState.ERROR, m); }
    }

    // ── Fields ─────────────────────────────────────────────────────────────
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<PublishResult> publishResult =
            new MutableLiveData<>(PublishResult.idle());

    /** Validation state for the currently open mission editor. */
    private final MutableLiveData<Boolean> canPublish = new MutableLiveData<>(false);

    // ── Constructor ────────────────────────────────────────────────────────
    public AdminViewModel(@NonNull Application app) {
        super(app);
        db        = AppDatabase.getDatabase(app);
        firestore = FirebaseFirestore.getInstance();
    }

    // ── Exposed LiveData ───────────────────────────────────────────────────

    public LiveData<List<AdminMission>> getAllMissions() {
        return db.adminMissionDao().getAllLive();
    }

    public LiveData<List<AdminQuestion>> getQuestionsForMission(int missionId) {
        return db.adminMissionDao().getById(missionId) != null
                ? db.adminQuestionDao().getByMissionLive(missionId)
                : new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<PublishResult> getPublishResult() { return publishResult; }
    public LiveData<Boolean>       getCanPublish()    { return canPublish; }

    // ── Mission CRUD ───────────────────────────────────────────────────────

    /** Creates a new mission draft and returns its Room-generated ID via callback. */
    public void createMission(String title, String difficulty, String mechanicType,
                              OnMissionCreated callback) {
        executor.execute(() -> {
            AdminMission m = new AdminMission(title, difficulty, mechanicType);
            long newId = db.adminMissionDao().insert(m);
            if (callback != null) callback.onCreated((int) newId);
        });
    }

    public void updateMission(AdminMission mission) {
        executor.execute(() -> {
            mission.syncStatus     = AdminMission.STATUS_DRAFT;
            mission.lastModifiedMs = System.currentTimeMillis();
            db.adminMissionDao().update(mission);
        });
    }

    public void deleteMission(AdminMission mission) {
        executor.execute(() -> db.adminMissionDao().delete(mission));
    }

    // ── Question CRUD ──────────────────────────────────────────────────────

    public void addQuestion(int missionId, OnQuestionCreated callback) {
        executor.execute(() -> {
            int count = db.adminQuestionDao().countForMission(missionId);
            AdminQuestion q = new AdminQuestion(missionId, count);
            long newId = db.adminQuestionDao().insert(q);
            if (callback != null) callback.onCreated((int) newId);
            revalidate(missionId);
        });
    }

    public void saveQuestion(AdminQuestion question, int missionId) {
        executor.execute(() -> {
            db.adminQuestionDao().update(question);
            // Mark parent mission as DRAFT again
            AdminMission m = db.adminMissionDao().getById(missionId);
            if (m != null) {
                m.syncStatus     = AdminMission.STATUS_DRAFT;
                m.lastModifiedMs = System.currentTimeMillis();
                db.adminMissionDao().update(m);
            }
            revalidate(missionId);
        });
    }

    public void deleteQuestion(AdminQuestion question, int missionId) {
        executor.execute(() -> {
            db.adminQuestionDao().delete(question);
            revalidate(missionId);
        });
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Re-evaluates whether all questions in the mission are valid.
     * Posts result to {@link #canPublish} LiveData.
     */
    private void revalidate(int missionId) {
        List<AdminQuestion> questions = db.adminQuestionDao().getByMission(missionId);
        boolean valid = !questions.isEmpty();
        for (AdminQuestion q : questions) {
            if (!q.isValid()) { valid = false; break; }
        }
        canPublish.postValue(valid);
    }

    public void revalidateMission(int missionId) {
        executor.execute(() -> revalidate(missionId));
    }

    // ── Publish workflow ───────────────────────────────────────────────────

    /**
     * Publishes a mission and all its questions to Firestore.
     *
     * <ol>
     *   <li>Reads questions from Room</li>
     *   <li>Validates all questions — aborts with ERROR if any are invalid</li>
     *   <li>Writes to {@code missions/{missionId}} in Firestore</li>
     *   <li>Increments {@code meta/global_version.version_id} atomically</li>
     *   <li>Updates Room record to SYNCED</li>
     * </ol>
     */
    public void publishMission(int missionId) {
        publishResult.postValue(PublishResult.loading());

        executor.execute(() -> {
            AdminMission mission = db.adminMissionDao().getById(missionId);
            if (mission == null) {
                publishResult.postValue(PublishResult.error("Mission not found."));
                return;
            }

            List<AdminQuestion> questions = db.adminQuestionDao().getByMission(missionId);
            if (questions.isEmpty()) {
                publishResult.postValue(PublishResult.error("Add at least one question before publishing."));
                return;
            }
            for (AdminQuestion q : questions) {
                if (!q.isValid()) {
                    publishResult.postValue(PublishResult.error(
                            "All questions must have text, 4 options, and a correct answer selected."));
                    return;
                }
            }

            // Build Firestore document
            String docId = mission.firestoreDocId != null
                    ? mission.firestoreDocId
                    : "mission_" + missionId;

            Map<String, Object> missionDoc = new HashMap<>();
            missionDoc.put("title",        mission.title);
            missionDoc.put("difficulty",   mission.difficulty);
            missionDoc.put("mechanicType", mission.mechanicType);
            missionDoc.put("questionCount", questions.size());
            missionDoc.put("publishedAt",  System.currentTimeMillis());

            List<Map<String, Object>> questionDocs = new ArrayList<>();
            for (AdminQuestion q : questions) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionText",   q.questionText);
                qMap.put("optionA",        q.optionA);
                qMap.put("optionB",        q.optionB);
                qMap.put("optionC",        q.optionC);
                qMap.put("optionD",        q.optionD);
                qMap.put("correctOption",  q.correctOption);
                qMap.put("sortOrder",      q.sortOrder);
                questionDocs.add(qMap);
            }
            missionDoc.put("questions", questionDocs);

            // Write to Firestore
            firestore.collection("missions")
                    .document(docId)
                    .set(missionDoc, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        // Increment global_version_id
                        Map<String, Object> versionUpdate = new HashMap<>();
                        versionUpdate.put("version_id",
                                com.google.firebase.firestore.FieldValue.increment(1));
                        versionUpdate.put("last_updated", System.currentTimeMillis());

                        firestore.collection("meta")
                                .document("global_version")
                                .set(versionUpdate, SetOptions.merge())
                                .addOnSuccessListener(v2 -> {
                                    // Update Room to SYNCED
                                    db.adminMissionDao().updateSyncStatus(
                                            missionId,
                                            AdminMission.STATUS_SYNCED,
                                            docId,
                                            System.currentTimeMillis());
                                    canPublish.postValue(true);
                                    publishResult.postValue(
                                            PublishResult.success("Mission published successfully."));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Version increment failed", e);
                                    publishResult.postValue(PublishResult.error(
                                            "Published but version update failed: " + e.getMessage()));
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore write failed", e);
                        publishResult.postValue(PublishResult.error(
                                "Publish failed: " + e.getMessage()));
                    });
        });
    }

    public void resetPublishState() {
        publishResult.postValue(PublishResult.idle());
    }

    // ── Callbacks ──────────────────────────────────────────────────────────

    public interface OnMissionCreated  { void onCreated(int missionId); }
    public interface OnQuestionCreated { void onCreated(int questionId); }
}
'@
[System.IO.File]::WriteAllText("$base\app\src\main\java\com\example\servermasterncii\admin\AdminViewModel.java", $vm, $enc)
Write-Host "ViewModel written"