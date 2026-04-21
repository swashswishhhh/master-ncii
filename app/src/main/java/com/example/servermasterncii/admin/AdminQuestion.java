package com.example.servermasterncii.admin;

import java.util.List;

/**
 * AdminQuestion — Model for admin-created questions stored in Firestore.
 * 
 * This model represents questions created by admins through AddQuestionActivity.
 * These questions are stored in Firestore and merged with local JSON questions
 * at runtime by QuestionLoader.
 * 
 * Firestore collection: questions
 * Document structure matches all fields in this class.
 */
public class AdminQuestion {
    
    private String id;                  // Firestore document ID (null for new questions)
    private String questionText;        // The question text
    private List<String> choices;       // 4 answer choices
    private String correctAnswer;       // The correct answer string (not index)
    private String chapterId;           // e.g., "chapter_1"
    private String missionId;           // e.g., "mission_1_1"
    private String difficulty;          // "easy" | "medium" | "hard"
    private String explanation;         // Optional explanation (can be empty)
    private boolean published;          // true = visible to students, false = draft
    private String createdBy;           // Firebase Auth UID of creator

    /**
     * Empty constructor required for Firestore deserialization.
     */
    public AdminQuestion() {
    }

    /**
     * Full constructor with all fields.
     */
    public AdminQuestion(String id, String questionText, List<String> choices, 
                        String correctAnswer, String chapterId, String missionId,
                        String difficulty, String explanation, boolean published, 
                        String createdBy) {
        this.id = id;
        this.questionText = questionText;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.chapterId = chapterId;
        this.missionId = missionId;
        this.difficulty = difficulty;
        this.explanation = explanation;
        this.published = published;
        this.createdBy = createdBy;
    }

    // ═══════════════════════════════════════════════════════════════
    // Getters and Setters
    // ═══════════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
