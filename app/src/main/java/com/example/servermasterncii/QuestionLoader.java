package com.example.servermasterncii;

import android.content.Context;

import com.example.servermasterncii.model.Question;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QuestionLoader {

    /**
     * Reads questions.json from assets and returns only questions
     * whose "id" starts with "SC_{levelId}_" (e.g. "SC_1.1_").
     */
    public static List<Question> loadForLevel(Context context, String levelId) {
        List<Question> result = new ArrayList<>();
        String prefix = "SC_" + levelId + "_";

        try {
            InputStream is = context.getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id", "");

                // Only load questions belonging to this level
                if (!id.startsWith(prefix)) continue;

                Question q = new Question();

                q.setType(obj.optString("type", Question.TYPE_STATIC));
                q.setCategory(obj.optString("category", ""));
                q.setExplanation(obj.optString("explanation", ""));

                // "question" field for static/true_false, "task" for interactive
                String questionText = obj.has("question")
                        ? obj.optString("question", "")
                        : obj.optString("task", "");
                q.setQuestionText(questionText);

                // answer_index in JSON is 0-based; correctOption in model is 1-based
                q.setCorrectOption(obj.optInt("answer_index", 0) + 1);

                // Map learningOutcome from the levelId (e.g. "1.1")
                q.setLearningOutcome(levelId);

                // Options array
                JSONArray options = obj.optJSONArray("options");
                if (options != null) {
                    q.setOptionA(options.optString(0, ""));
                    q.setOptionB(options.optString(1, ""));
                    q.setOptionC(options.optString(2, ""));
                    q.setOptionD(options.optString(3, ""));
                }

                result.add(q);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}