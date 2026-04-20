package com.example.servermasterncii.admin.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for a question authored inside an admin mission.
 *
 * <p>Each question belongs to exactly one {@link AdminMission} via
 * {@code missionId}. The foreign key cascades deletes so removing a
 * mission also removes all its questions.</p>
 *
 * <h3>Validation rule</h3>
 * A question is considered <em>valid</em> when:
 * <ul>
 *   <li>{@link #questionText} is non-empty</li>
 *   <li>All four options are non-empty</li>
 *   <li>{@link #correctOption} is in [1..4]</li>
 * </ul>
 * The publish button in the dashboard stays disabled until every question
 * in the mission passes this check.
 */
@Entity(
    tableName = "admin_questions",
    foreignKeys = @ForeignKey(
        entity    = AdminMission.class,
        parentColumns = "id",
        childColumns  = "missionId",
        onDelete  = ForeignKey.CASCADE
    ),
    indices = @Index("missionId")
)
public class AdminQuestion {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** FK → AdminMission.id */
    public int missionId;

    public String questionText;
    public String optionA;
    public String optionB;
    public String optionC;
    public String optionD;

    /** 1 = A, 2 = B, 3 = C, 4 = D.  0 = not yet selected (invalid). */
    public int correctOption;

    /** Display order within the mission (0-based). */
    public int sortOrder;

    public AdminQuestion() {
        this.correctOption = 0;
    }

    public AdminQuestion(int missionId, int sortOrder) {
        this.missionId     = missionId;
        this.sortOrder     = sortOrder;
        this.correctOption = 0;
    }

    /**
     * Returns true when this question has all required fields filled in
     * and a correct answer selected.
     */
    public boolean isValid() {
        return questionText != null && !questionText.trim().isEmpty()
            && optionA != null && !optionA.trim().isEmpty()
            && optionB != null && !optionB.trim().isEmpty()
            && optionC != null && !optionC.trim().isEmpty()
            && optionD != null && !optionD.trim().isEmpty()
            && correctOption >= 1 && correctOption <= 4;
    }
}
