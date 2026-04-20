package com.example.servermasterncii.admin.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing an admin-authored mission (quiz set).
 *
 * <p>Missions are drafted locally in Room and published to Firestore
 * via the {@link com.example.servermasterncii.admin.AdminViewModel}
 * publish workflow.</p>
 *
 * <h3>Sync Status</h3>
 * <ul>
 *   <li>{@link #STATUS_DRAFT}  — created/edited locally, not yet pushed</li>
 *   <li>{@link #STATUS_SYNCED} — last local state matches Firestore</li>
 * </ul>
 */
@Entity(tableName = "admin_missions")
public class AdminMission {

    public static final String STATUS_DRAFT  = "DRAFT";
    public static final String STATUS_SYNCED = "SYNCED";

    public static final String MECHANIC_ORGANIZER = "Organizer";
    public static final String MECHANIC_MONITOR   = "Monitor";
    public static final String MECHANIC_MATRIX    = "Matrix";

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Human-readable mission title, e.g. "RDP Configuration". */
    public String title;

    /** Difficulty label: "Beginner", "Intermediate", "Advanced". */
    public String difficulty;

    /** Game mechanic type: Organizer / Monitor / Matrix. */
    public String mechanicType;

    /** Sync state: DRAFT or SYNCED. */
    public String syncStatus;

    /** Epoch millis of last local edit. */
    public long lastModifiedMs;

    /** Firestore document ID — null until first publish. */
    public String firestoreDocId;

    public AdminMission() {
        this.syncStatus     = STATUS_DRAFT;
        this.lastModifiedMs = System.currentTimeMillis();
    }

    public AdminMission(String title, String difficulty, String mechanicType) {
        this.title          = title;
        this.difficulty     = difficulty;
        this.mechanicType   = mechanicType;
        this.syncStatus     = STATUS_DRAFT;
        this.lastModifiedMs = System.currentTimeMillis();
    }
}
