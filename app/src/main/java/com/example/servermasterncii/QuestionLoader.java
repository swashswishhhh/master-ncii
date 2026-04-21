package com.example.servermasterncii;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * QuestionLoader — Hybrid question loading system for students.
 * 
 * Merges questions from two sources:
 * 1. Local JSON (assets/questions.json) — existing read-only questions
 * 2. Firestore (questions collection) — admin-created published questions
 * 
 * Mission ID mapping:
 * - mission_1_1 → SC_1.1_* (local JSON pattern)
 * - mission_1_2 → SC_1.2_* (local JSON pattern)
 * - etc.
 * 
 * Students only see published questions from Firestore (published == true).
 */
public class QuestionLoader {

    private static final String TAG = "QuestionLoader";

    /**
     * Loads questions for a specific mission from both local JSON and Firestore.
     * Results are merged, shuffled, and returned via callback.
     * 
     * @param ctx Application context
     * @param chapterId Chapter ID (e.g., "chapter_1")
     * @param missionId Mission ID (e.g., "mission_1_1")
     * @param listener Callback for results
     */
    public static void loadForMission(Context ctx, String chapterId, String missionId, 
                                     OnQuestionsLoadedListener listener) {
        if (listener == null) {
            Log.e(TAG, "Listener is null");
            return;
        }
        
        List<Question> allQuestions = new ArrayList<>();
        
        // Step 1: Load local JSON questions
        try {
            List<Question> localQuestions = loadLocalQuestionsForMission(ctx, missionId);
            allQuestions.addAll(localQuestions);
            Log.d(TAG, "Loaded " + localQuestions.size() + " local questions for " + missionId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading local questions", e);
            listener.onError("Failed to load local questions: " + e.getMessage());
            return;
        }
        
        // Step 2: Load Firestore questions
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("questions")
                .whereEqualTo("chapterId", chapterId)
                .whereEqualTo("missionId", missionId)
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Question> firestoreQuestions = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Question q = convertFirestoreToQuestion(doc);
                            if (q != null) {
                                firestoreQuestions.add(q);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Firestore question: " + doc.getId(), e);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + firestoreQuestions.size() + " Firestore questions for " + missionId);
                    allQuestions.addAll(firestoreQuestions);
                    
                    // Step 3: Shuffle and return
                    Collections.shuffle(allQuestions);
                    listener.onLoaded(allQuestions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading Firestore questions", e);
                    
                    // Return local questions only if Firestore fails
                    if (!allQuestions.isEmpty()) {
                        Collections.shuffle(allQuestions);
                        listener.onLoaded(allQuestions);
                    } else {
                        listener.onError("Failed to load questions: " + e.getMessage());
                    }
                });
    }

    /**
     * Loads questions from local JSON for a specific mission.
     * Maps mission IDs to JSON ID prefixes:
     * - mission_1_1 → SC_1.1_*
     * - mission_1_2 → SC_1.2_*
     * - mission_2_1 → SC_2.1_*
     * 
     * @param ctx Application context
     * @param missionId Mission ID (e.g., "mission_1_1")
     * @return List of Question objects from local JSON
     */
    private static List<Question> loadLocalQuestionsForMission(Context ctx, String missionId) {
        List<Question> result = new ArrayList<>();
        
        // Extract level ID from mission ID (e.g., "mission_1_1" → "1.1")
        String levelId = extractLevelId(missionId);
        if (levelId == null) {
            Log.w(TAG, "Could not extract level ID from mission: " + missionId);
            return result;
        }
        
        String prefix = "SC_" + levelId + "_";

        try {
            InputStream is = ctx.getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id", "");

                // Only load questions belonging to this mission
                if (!id.startsWith(prefix)) continue;

                Question q = new Question();
                q.questionText = obj.optString("question", "");
                q.explanation = obj.optString("explanation", "");
                q.source = "local";

                // Get options array
                JSONArray options = obj.optJSONArray("options");
                if (options != null && options.length() >= 4) {
                    List<String> choicesList = new ArrayList<>();
                    for (int j = 0; j < options.length(); j++) {
                        choicesList.add(options.optString(j, ""));
                    }
                    q.choices = choicesList;
                    
                    // Convert answer_index (0-based) to correctAnswer (string)
                    int answerIndex = obj.optInt("answer_index", 0);
                    if (answerIndex >= 0 && answerIndex < choicesList.size()) {
                        q.correctAnswer = choicesList.get(answerIndex);
                    }
                }

                result.add(q);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading local questions", e);
        }

        return result;
    }

    /**
     * Converts a Firestore document to a Question object.
     * 
     * @param doc Firestore QueryDocumentSnapshot
     * @return Question object or null if conversion fails
     */
    private static Question convertFirestoreToQuestion(QueryDocumentSnapshot doc) {
        try {
            Question q = new Question();
            
            q.questionText = doc.getString("questionText");
            q.explanation = doc.getString("explanation");
            q.source = "firestore";
            
            // Get choices array
            List<String> choices = (List<String>) doc.get("choices");
            if (choices != null && choices.size() >= 4) {
                q.choices = choices;
            }
            
            // Get correct answer (already a string in Firestore)
            q.correctAnswer = doc.getString("correctAnswer");
            
            return q;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting Firestore document to Question", e);
            return null;
        }
    }

    /**
     * Extracts level ID from mission ID.
     * Examples:
     * - "mission_1_1" → "1.1"
     * - "mission_2_3" → "2.3"
     * 
     * @param missionId Mission ID
     * @return Level ID or null if invalid format
     */
    private static String extractLevelId(String missionId) {
        if (missionId == null || !missionId.startsWith("mission_")) {
            return null;
        }
        
        // Remove "mission_" prefix
        String remainder = missionId.substring(8); // "1_1"
        
        // Replace first underscore with dot
        return remainder.replace("_", ".");
    }

    // ═══════════════════════════════════════════════════════════════
    // Callback Interface
    // ═══════════════════════════════════════════════════════════════

    /**
     * Callback interface for async question loading.
     */
    public interface OnQuestionsLoadedListener {
        /**
         * Called when questions are successfully loaded.
         * 
         * @param questions List of merged and shuffled questions
         */
        void onLoaded(List<Question> questions);
        
        /**
         * Called when an error occurs during loading.
         * 
         * @param errorMessage Error description
         */
        void onError(String errorMessage);
    }

    // ═══════════════════════════════════════════════════════════════
    // Question Model (Student-side)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Question — Simple model for student quiz questions.
     * 
     * This is a lightweight model used by QuizActivity.
     * It represents questions from both local JSON and Firestore.
     */
    public static class Question {
        public String questionText;
        public List<String> choices;       // 4 choices
        public String correctAnswer;       // The correct answer string
        public String explanation;         // Optional explanation
        public String source;              // "local" or "firestore"

        public Question() {
        }

        public Question(String questionText, List<String> choices, String correctAnswer, 
                       String explanation, String source) {
            this.questionText = questionText;
            this.choices = choices;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
            this.source = source;
        }
    }
}
