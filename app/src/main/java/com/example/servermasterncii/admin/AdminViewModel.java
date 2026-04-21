package com.example.servermasterncii.admin;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminViewModel extends ViewModel {

    private static final String TAG = "AdminViewModel";
    private static final String COLLECTION_QUESTIONS = "questions";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ── LiveData fields ────────────────────────────────────────────────────
    private final MutableLiveData<List<AdminQuestion>> questions      = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>             isLoading      = new MutableLiveData<>(false);
    private final MutableLiveData<String>              error          = new MutableLiveData<>("");
    private final MutableLiveData<String>              successMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer>             totalQuestions = new MutableLiveData<>(0);
    private final MutableLiveData<Integer>             totalUsers     = new MutableLiveData<>(0);

    // ═══════════════════════════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════════════════════════

    public LiveData<List<AdminQuestion>> getQuestions()     { return questions; }
    public LiveData<Boolean>            getIsLoading()      { return isLoading; }
    public LiveData<String>             getError()          { return error; }
    public LiveData<String>             getSuccessMessage() { return successMessage; }
    public LiveData<Integer>            getTotalQuestions() { return totalQuestions; }
    public LiveData<Integer>            getTotalUsers()     { return totalUsers; }

    public void clearError()          { error.setValue(""); }
    public void clearSuccessMessage() { successMessage.setValue(""); }

    // ═══════════════════════════════════════════════════════════════
    // Dashboard Stats
    // ═══════════════════════════════════════════════════════════════

    /**
     * Loads total question count and total user count.
     * Called by AdminDashboardActivity on create and resume.
     */
    public void loadStats() {
        isLoading.setValue(true);

        // Total questions
        db.collection(COLLECTION_QUESTIONS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    totalQuestions.setValue(snapshot.size());
                    isLoading.setValue(false);
                    Log.d(TAG, "Total questions: " + snapshot.size());
                })
                .addOnFailureListener(e -> {
                    error.setValue("Failed to load question stats: " + e.getMessage());
                    isLoading.setValue(false);
                });

        // Total users
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    totalUsers.setValue(snapshot.size());
                    Log.d(TAG, "Total users: " + snapshot.size());
                })
                .addOnFailureListener(e ->
                        error.setValue("Failed to load user stats: " + e.getMessage()));
    }

    // ═══════════════════════════════════════════════════════════════
    // Question CRUD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Publishes a new question to Firestore.
     * Called by AddQuestionActivity when creating a new question.
     */
    public void publishQuestion(AdminQuestion q) {
        isLoading.setValue(true);
        error.setValue("");
        successMessage.setValue("");

        db.collection(COLLECTION_QUESTIONS)
                .add(toMap(q))
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Question published: " + ref.getId());
                    successMessage.setValue(q.isPublished()
                            ? "✅ Question published successfully!"
                            : "📝 Draft saved successfully!");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error publishing question", e);
                    error.setValue("❌ Failed to save question: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * Updates an existing question in Firestore.
     * Called by AddQuestionActivity when editing an existing question.
     */
    public void updateQuestion(AdminQuestion q) {
        if (q.getId() == null || q.getId().isEmpty()) {
            error.setValue("❌ Cannot update: question ID is missing");
            return;
        }

        isLoading.setValue(true);
        error.setValue("");
        successMessage.setValue("");

        Map<String, Object> data = toMap(q);
        data.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_QUESTIONS)
                .document(q.getId())
                .set(data)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Question updated: " + q.getId());
                    successMessage.setValue("✅ Question updated successfully!");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating question", e);
                    error.setValue("❌ Failed to update: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * Loads all questions from Firestore ordered by chapterId.
     * Called by QuestionManagerActivity.
     */
    public void loadQuestions() {
        isLoading.setValue(true);
        error.setValue("");

        db.collection(COLLECTION_QUESTIONS)
                .orderBy("chapterId")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminQuestion> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AdminQuestion q = fromDoc(doc);
                        if (q != null) list.add(q);
                    }
                    questions.setValue(list);
                    isLoading.setValue(false);
                    Log.d(TAG, "Loaded " + list.size() + " questions");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading questions", e);
                    error.setValue("❌ Failed to load questions: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * Loads questions filtered by chapter and mission.
     * Called by AdminMissionEditorActivity.
     */
    public void loadQuestionsByMission(String chapterId, String missionId) {
        isLoading.setValue(true);
        error.setValue("");

        db.collection(COLLECTION_QUESTIONS)
                .whereEqualTo("chapterId", chapterId)
                .whereEqualTo("missionId", missionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminQuestion> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AdminQuestion q = fromDoc(doc);
                        if (q != null) list.add(q);
                    }
                    questions.setValue(list);
                    isLoading.setValue(false);
                    Log.d(TAG, "Loaded " + list.size() + " questions for " + missionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading by mission", e);
                    error.setValue("❌ Failed to load mission questions: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * Deletes a question from Firestore and refreshes the list.
     * Called by QuestionManagerActivity.
     */
    public void deleteQuestion(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            error.setValue("❌ Cannot delete: question ID is missing");
            return;
        }

        isLoading.setValue(true);
        error.setValue("");
        successMessage.setValue("");

        db.collection(COLLECTION_QUESTIONS)
                .document(questionId)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Question deleted: " + questionId);
                    successMessage.setValue("🗑️ Question deleted.");
                    loadQuestions(); // refresh list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting question", e);
                    error.setValue("❌ Failed to delete: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * Toggles the published status of a question.
     * Called by QuestionManagerActivity.
     */
    public void togglePublish(String questionId, boolean publish) {
        if (questionId == null || questionId.isEmpty()) {
            error.setValue("❌ Cannot update: question ID is missing");
            return;
        }

        isLoading.setValue(true);
        error.setValue("");
        successMessage.setValue("");

        Map<String, Object> update = new HashMap<>();
        update.put("published", publish);
        update.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_QUESTIONS)
                .document(questionId)
                .update(update)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Publish toggled: " + questionId + " → " + publish);
                    successMessage.setValue(publish ? "✅ Published!" : "⛔ Unpublished.");
                    loadQuestions(); // refresh list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error toggling publish", e);
                    error.setValue("❌ Failed to update publish status: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // ═══════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════

    /** Converts AdminQuestion → Firestore Map */
    private Map<String, Object> toMap(AdminQuestion q) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionText",  q.getQuestionText());
        data.put("choices",       q.getChoices());
        data.put("correctAnswer", q.getCorrectAnswer());
        data.put("chapterId",     q.getChapterId());
        data.put("missionId",     q.getMissionId());
        data.put("difficulty",    q.getDifficulty());
        data.put("explanation",   q.getExplanation() != null ? q.getExplanation() : "");
        data.put("published",     q.isPublished());
        data.put("createdBy",     q.getCreatedBy() != null ? q.getCreatedBy() : "");
        data.put("createdAt",     Timestamp.now());
        return data;
    }

    /** Converts Firestore document → AdminQuestion, returns null on parse error */
    private AdminQuestion fromDoc(QueryDocumentSnapshot doc) {
        try {
            AdminQuestion q = new AdminQuestion();
            q.setId(doc.getId());
            q.setQuestionText(doc.getString("questionText"));
            q.setChoices((List<String>) doc.get("choices"));
            q.setCorrectAnswer(doc.getString("correctAnswer"));
            q.setChapterId(doc.getString("chapterId"));
            q.setMissionId(doc.getString("missionId"));
            q.setDifficulty(doc.getString("difficulty"));
            q.setExplanation(doc.getString("explanation"));
            q.setPublished(Boolean.TRUE.equals(doc.getBoolean("published")));
            q.setCreatedBy(doc.getString("createdBy"));
            return q;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing doc: " + doc.getId(), e);
            return null;
        }
    }
}