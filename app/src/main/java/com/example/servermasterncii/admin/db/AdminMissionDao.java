package com.example.servermasterncii.admin.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.servermasterncii.admin.model.AdminMission;

import java.util.List;

/**
 * Room DAO for {@link AdminMission} — admin-authored mission drafts.
 *
 * <p>All write operations run on the caller's thread (use
 * {@link com.example.servermasterncii.db.AppDatabase#databaseWriteExecutor}
 * or the ViewModel's executor). Read operations that return LiveData are
 * automatically observed on the main thread by Room.</p>
 */
@Dao
public interface AdminMissionDao {

    // ── Writes ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AdminMission mission);

    @Update
    void update(AdminMission mission);

    @Delete
    void delete(AdminMission mission);

    /**
     * Atomically updates only the sync-related columns after a successful
     * Firestore publish, avoiding a full object read-modify-write cycle.
     */
    @Query("UPDATE admin_missions " +
           "SET syncStatus = :status, firestoreDocId = :docId, lastModifiedMs = :ts " +
           "WHERE id = :id")
    void updateSyncStatus(int id, String status, String docId, long ts);

    // ── Reads ──────────────────────────────────────────────────────────────

    /** Returns all missions ordered by most-recently-modified first. */
    @Query("SELECT * FROM admin_missions ORDER BY lastModifiedMs DESC")
    LiveData<List<AdminMission>> getAllLive();

    /** Synchronous read — use only from a background thread. */
    @Query("SELECT * FROM admin_missions ORDER BY lastModifiedMs DESC")
    List<AdminMission> getAll();

    /** Synchronous single-row lookup by primary key. */
    @Query("SELECT * FROM admin_missions WHERE id = :id LIMIT 1")
    AdminMission getById(int id);

    /** Count of all missions (useful for stats). */
    @Query("SELECT COUNT(*) FROM admin_missions")
    int count();

    /** Count of missions with a given sync status. */
    @Query("SELECT COUNT(*) FROM admin_missions WHERE syncStatus = :status")
    int countByStatus(String status);
}
