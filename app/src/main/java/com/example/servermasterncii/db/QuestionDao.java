package com.example.servermasterncii.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.servermasterncii.model.Question;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Question> questions);

    @Query("SELECT * FROM questions WHERE learningOutcome = :learningOutcome")
    List<Question> getQuestionsByLearningOutcome(String learningOutcome);

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    List<Question> getQuestionsByIds(List<Integer> ids);

    @Query("SELECT * FROM questions")
    List<Question> getAllQuestions();

    /**
     * Returns all questions ordered by their ID (preserving the JSON insertion order).
     * Used by the GameRouter to present questions in the intended sequence
     * rather than a random order — important for interactive missions that
     * should come after their related theory questions.
     */
    @Query("SELECT * FROM questions WHERE learningOutcome = :learningOutcome ORDER BY id ASC")
    List<Question> getQuestionsByLearningOutcomeOrdered(String learningOutcome);

    /**
     * Returns all interactive mission entries.
     */
    @Query("SELECT * FROM questions WHERE type IN ('interactive_cmd', 'interactive_ui') ORDER BY id ASC")
    List<Question> getInteractiveMissions();

    /**
     * Returns all questions whose learningOutcome starts with the given
     * chapter prefix (e.g. "1." for Chapter 1, "2." for Chapter 2).
     */
    @Query("SELECT * FROM questions WHERE learningOutcome LIKE :chapterPrefix || '%' ORDER BY id ASC")
    List<Question> getQuestionsByChapterPrefix(String chapterPrefix);
}
