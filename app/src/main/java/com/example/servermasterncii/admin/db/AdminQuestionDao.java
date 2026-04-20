package com.example.servermasterncii.admin.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.servermasterncii.admin.model.AdminQuestion;

import java.util.List;

/**
 * Room DAO for {@link AdminQuestion} — questions belonging to admin missions.
 *
 * <p>Questions are cascade-deleted when their parent {@link
 * com.example.servermasterncii.admin.model.AdminMission} is deleted
 * (enforced by the foreign key constraint on the entity).</p>
 */
@Dao
public interface AdminQuestionDao {

    // ── Writes ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AdminQuestion question);

    @Update
    void update(AdminQuestion question);

    @Delete
    void delete(AdminQuestion question);

    // ── Reads ──────────────────────────────────────────────────────────────

    /**
     * Live list of all questions for a mission, ordered by sort position.
     * Observed by the editor UI to auto-refresh when questions are added/removed.
     */
    @Query("SELECT * FROM admin_questions WHERE missionId = :missionId ORDER BY sortOrder ASC")
    LiveData<List<AdminQuestion>> getByMissionLive(int missionId);

    /**
     * Synchronous read — use only from a background thread (e.g. publish workflow).
     */
    @Query("SELECT * FROM admin_questions WHERE missionId = :missionId ORDER BY sortOrder ASC")
    List<AdminQuestion> getByMission(int missionId);

    /** Synchronous single-row lookup. */
    @Query("SELECT * FROM admin_questions WHERE id = :id LIMIT 1")
    AdminQuestion getById(int id);

    /** Total question count for a mission — used for the dashboard card subtitle. */
    @Query("SELECT COUNT(*) FROM admin_questions WHERE missionId = :missionId")
    int countForMission(int missionId);

    /**
     * Count of questions that pass the validity check:
     * non-empty text, all 4 options filled, correct answer in [1..4].
     * Used to drive the publish-button enabled state.
     */
    @Query("SELECT COUNT(*) FROM admin_questions " +
           "WHERE missionId = :missionId " +
           "AND questionText IS NOT NULL AND TRIM(questionText) != '' " +
           "AND optionA IS NOT NULL AND TRIM(optionA) != '' " +
           "AND optionB IS NOT NULL AND TRIM(optionB) != '' " +
           "AND optionC IS NOT NULL AND TRIM(optionC) != '' " +
           "AND optionD IS NOT NULL AND TRIM(optionD) != '' " +
           "AND correctOption >= 1 AND correctOption <= 4")
    int countValidForMission(int missionId);
}
