package com.example.servermasterncii.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.servermasterncii.admin.db.AdminMissionDao;
import com.example.servermasterncii.admin.db.AdminQuestionDao;
import com.example.servermasterncii.admin.model.AdminMission;
import com.example.servermasterncii.admin.model.AdminQuestion;
import com.example.servermasterncii.model.Question;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Question.class, AdminMission.class, AdminQuestion.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract QuestionDao questionDao();
    public abstract AdminMissionDao adminMissionDao();
    public abstract AdminQuestionDao adminQuestionDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "quiz_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Initialize database from JSON upon creation
                                    databaseWriteExecutor.execute(() -> {
                                        QuestionDao dao = INSTANCE.questionDao();
                                        List<Question> questions = parseQuestionsFromJson(context);
                                        if (!questions.isEmpty()) {
                                            dao.insertAll(questions);
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Parses questions from the {@code questions.json} asset file.
     * <p>
     * Handles the actual JSON schema which uses:
     * <ul>
     *     <li>{@code id} — String identifier (e.g. "SC_1.1_Q1"), stored as hash for Room PK</li>
     *     <li>{@code type} — "static", "true_false", "interactive_cmd", "interactive_ui"</li>
     *     <li>{@code category} — topic category</li>
     *     <li>{@code question} — the question text</li>
     *     <li>{@code options} — String array of choices</li>
     *     <li>{@code answer_index} — 0-based index into the options array</li>
     *     <li>{@code explanation} — rationale for the correct answer</li>
     * </ul>
     * <p>
     * The {@code id} field in the JSON is a string like "SC_1.1_Q1". We derive the
     * Room integer primary key from it, and extract the learning outcome from the
     * prefix (e.g. "SC_1.1" → "LO1").
     */
    public static List<Question> parseQuestionsFromJson(Context context) {
        List<Question> questionList = new ArrayList<>();
        try (InputStream is = context.getAssets().open("questions.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            if (read != -1) {
                String jsonContent = new String(buffer, StandardCharsets.UTF_8);

                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Question q = new Question();

                    // ── ID: use the array index + 1 as the Room primary key ──
                    q.setId(i + 1);

                    // ── Type & Category ──
                    q.setType(obj.optString("type", Question.TYPE_STATIC));
                    q.setCategory(obj.optString("category", ""));

                    // ── Question text ──
                    q.setQuestionText(obj.getString("question"));

                    // ── Explanation ──
                    q.setExplanation(obj.optString("explanation", null));

                    // ── Options: JSON array → optionA/B/C/D ──
                    JSONArray opts = obj.optJSONArray("options");
                    if (opts != null && opts.length() >= 2) {
                        q.setOptionA(opts.optString(0, ""));
                        q.setOptionB(opts.optString(1, ""));
                        q.setOptionC(opts.length() > 2 ? opts.optString(2, "") : "");
                        q.setOptionD(opts.length() > 3 ? opts.optString(3, "") : "");
                    }

                    // ── Correct answer: convert 0-based answer_index to 1-based ──
                    int answerIndex = obj.optInt("answer_index", 0);
                    q.setCorrectOption(answerIndex + 1); // Room model uses 1-based

                    // ── Learning Outcome: derive from the string ID prefix ──
                    String stringId = obj.getString("id");
                    q.setLearningOutcome(deriveLearningOutcome(stringId));

                    questionList.add(q);
                }
            }
        } catch (Exception e) {
            Log.e("AppDatabase", "Error parsing questions from JSON", e);
        }
        return questionList;
    }

    /**
     * Derives a section-level Learning Outcome ID from the question's string ID.
     * <p>
     * The returned value uses the "x.y" format that maps directly to the
     * Saga Map level IDs (e.g. "1.1", "1.3", "2.5").
     * <p>
     * Examples:
     * <ul>
     *     <li>"SC_1.1_Q1"  → "1.1"</li>
     *     <li>"SC_1.2_Q5"  → "1.2"</li>
     *     <li>"SC_1.3_Q1"  → "1.3"</li>
     *     <li>"SC_1.4_Q1"  → "1.4"</li>
     *     <li>"MISSION_2"  → "MISSION"</li>
     * </ul>
     */
    private static String deriveLearningOutcome(String stringId) {
        if (stringId == null || stringId.isEmpty()) return "1.1";
        if (stringId.startsWith("MISSION")) return "MISSION";

        // Pattern: SC_1.X_Qn → return "1.X" directly
        try {
            // "SC_1.2_Q5" → split by "_" → ["SC", "1.2", "Q5"]
            String[] parts = stringId.split("_");
            if (parts.length >= 2) {
                // "1.2" is already the format we want
                String section = parts[1];
                if (section.contains(".")) {
                    return section;
                }
            }
        } catch (Exception e) {
            Log.w("AppDatabase", "Could not derive LO from id: " + stringId);
        }
        return "1.1";
    }
}
