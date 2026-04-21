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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QuestionLoader - Loads questions from both local JSON assets and Firestore.
 * Combines questions from both sources for a complete quiz experience.
 */
public class QuestionLoader {

    private static final String TAG = "QuestionLoader";

    public interface OnQuestionsLoadedListener {
        void onLoaded(List<Question> questions);
        void onError(String errorMessage);
    }

    public static class Question {
        public String questionText;
        public List<String> choices;
        public String correctAnswer;
        public String explanation;
        public String chapterId;
        public String missionId;
        public String difficulty;
        public boolean published;
        public String source; // "json" or "firestore"

        public Question() {
            choices = new ArrayList<>();
        }
    }

    /**
     * Load questions for a specific mission from both JSON and Firestore.
     * Combines results and returns them sorted by difficulty or creation order.
     */
    public static void loadForMission(Context context, String chapterId, String missionId,
                                      OnQuestionsLoadedListener listener) {
        List<Question> allQuestions = new ArrayList<>();

        // Track loading completion
        final int[] pendingSources = {2}; // JSON + Firestore

        // Load from JSON
        loadFromJson(context, chapterId, missionId, new OnQuestionsLoadedListener() {
            @Override
            public void onLoaded(List<Question> jsonQuestions) {
                synchronized (allQuestions) {
                    allQuestions.addAll(jsonQuestions);
                }
                checkComplete();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "JSON load error: " + errorMessage);
                checkComplete();
            }

            private void checkComplete() {
                pendingSources[0]--;
                if (pendingSources[0] == 0 && listener != null) {
                    // Sort questions: Firestore first, then JSON (or by difficulty)
                    sortAndDeliver(allQuestions, listener);
                }
            }
        });

        // Load from Firestore
        loadFromFirestore(missionId, new OnQuestionsLoadedListener() {
            @Override
            public void onLoaded(List<Question> firestoreQuestions) {
                synchronized (allQuestions) {
                    allQuestions.addAll(firestoreQuestions);
                }
                checkComplete();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Firestore load error: " + errorMessage);
                checkComplete();
            }

            private void checkComplete() {
                pendingSources[0]--;
                if (pendingSources[0] == 0 && listener != null) {
                    sortAndDeliver(allQuestions, listener);
                }
            }
        });
    }

    private static void sortAndDeliver(List<Question> questions, OnQuestionsLoadedListener listener) {
        // Sort: Firestore questions first, then JSON (or by difficulty order)
        questions.sort((a, b) -> {
            // Firestore questions (admin created) come first
            if ("firestore".equals(a.source) && !"firestore".equals(b.source)) return -1;
            if (!"firestore".equals(a.source) && "firestore".equals(b.source)) return 1;

            // Then sort by difficulty: easy, medium, hard
            String[] difficultyOrder = {"easy", "medium", "hard"};
            int aDiffIndex = getDifficultyIndex(a.difficulty, difficultyOrder);
            int bDiffIndex = getDifficultyIndex(b.difficulty, difficultyOrder);
            return Integer.compare(aDiffIndex, bDiffIndex);
        });

        listener.onLoaded(questions);
    }

    private static int getDifficultyIndex(String difficulty, String[] order) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equalsIgnoreCase(difficulty)) return i;
        }
        return 1; // default medium
    }

    /**
     * Load questions from local chapters.json asset
     */
    private static void loadFromJson(Context context, String chapterId, String missionId,
                                     OnQuestionsLoadedListener listener) {
        List<Question> questions = new ArrayList<>();

        try {
            String jsonString = loadJsonFromAsset(context, "chapters.json");
            JSONObject root = new JSONObject(jsonString);
            JSONArray chapters = root.getJSONArray("chapters");

            for (int i = 0; i < chapters.length(); i++) {
                JSONObject chapter = chapters.getJSONObject(i);
                String chapterIdFromJson = chapter.getString("id");

                if (chapterIdFromJson.equals(chapterId)) {
                    JSONArray missions = chapter.getJSONArray("missions");

                    for (int j = 0; j < missions.length(); j++) {
                        JSONObject mission = missions.getJSONObject(j);
                        String missionIdFromJson = mission.getString("id");

                        if (missionIdFromJson.equals(missionId)) {
                            JSONArray missionQuestions = mission.getJSONArray("questions");

                            for (int k = 0; k < missionQuestions.length(); k++) {
                                JSONObject qJson = missionQuestions.getJSONObject(k);
                                Question q = new Question();
                                q.questionText = qJson.getString("question");
                                q.source = "json";

                                JSONArray choices = qJson.getJSONArray("choices");
                                q.choices = new ArrayList<>();
                                for (int c = 0; c < choices.length(); c++) {
                                    q.choices.add(choices.getString(c));
                                }

                                q.correctAnswer = qJson.getString("correct");
                                q.explanation = qJson.optString("explanation", "");
                                q.chapterId = chapterId;
                                q.missionId = missionId;
                                q.difficulty = qJson.optString("difficulty", "medium");
                                q.published = true;

                                questions.add(q);
                            }
                            break;
                        }
                    }
                    break;
                }
            }

            listener.onLoaded(questions);

        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON questions", e);
            listener.onError(e.getMessage());
        }
    }

    /**
     * Load admin-created questions from Firestore for a specific mission
     */
    private static void loadFromFirestore(String missionId, OnQuestionsLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Question> questions = new ArrayList<>();

        // Query questions collection where missionId matches and published = true
        db.collection("questions")
                .whereEqualTo("missionId", missionId)
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Question q = new Question();
                        q.questionText = doc.getString("questionText");
                        q.source = "firestore";

                        // Get choices list
                        List<String> choicesList = (List<String>) doc.get("choices");
                        if (choicesList != null) {
                            q.choices = new ArrayList<>(choicesList);
                        } else {
                            // Fallback for older data structure
                            q.choices = new ArrayList<>();
                            q.choices.add(doc.getString("choiceA"));
                            q.choices.add(doc.getString("choiceB"));
                            q.choices.add(doc.getString("choiceC"));
                            q.choices.add(doc.getString("choiceD"));
                        }

                        q.correctAnswer = doc.getString("correctAnswer");
                        q.explanation = doc.getString("explanation");
                        q.chapterId = doc.getString("chapterId");
                        q.missionId = doc.getString("missionId");
                        q.difficulty = doc.getString("difficulty");
                        Boolean published = doc.getBoolean("published");
                        q.published = published != null && published;

                        // Only add if all required fields exist
                        if (q.questionText != null && !q.questionText.isEmpty() &&
                                q.choices != null && !q.choices.isEmpty() &&
                                q.correctAnswer != null && !q.correctAnswer.isEmpty()) {
                            questions.add(q);
                        }
                    }

                    Log.d(TAG, "Loaded " + questions.size() + " questions from Firestore for mission " + missionId);
                    listener.onLoaded(questions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore query FAILED: " + e.getMessage()); // ← check this in Logcat
                    listener.onError(e.getMessage());
                });
    }

    private static String loadJsonFromAsset(Context context, String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON from assets", e);
            return "{}";
        }
    }
}