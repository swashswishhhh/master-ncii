package com.example.servermasterncii.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * POJO representing a single quiz item for the CSS NC II Quiz Game.
 * <p>
 * Supports three question types:
 * <ul>
 *     <li><b>static</b> / <b>true_false</b> — standard multiple-choice in QuizActivity</li>
 *     <li><b>interactive_cmd</b> — launches TerminalEmulatorActivity</li>
 *     <li><b>interactive_ui</b> — launches a simulator (Firewall, IP Config, Workgroup, etc.)</li>
 * </ul>
 *
 * The {@link #type} and {@link #category} fields drive the GameRouter logic inside
 * QuizActivity, which decides whether to stay in the quiz or start an external Activity.
 */
@Entity(tableName = "questions")
public class Question {

    /** Question types recognized by the GameRouter. */
    public static final String TYPE_STATIC          = "static";
    public static final String TYPE_TRUE_FALSE      = "true_false";
    public static final String TYPE_INTERACTIVE_CMD  = "interactive_cmd";
    public static final String TYPE_INTERACTIVE_UI   = "interactive_ui";

    /** Categories used for interactive_ui routing. */
    public static final String CATEGORY_FIREWALL    = "Firewall Config";
    public static final String CATEGORY_IP_CONFIG   = "IP Config";
    public static final String CATEGORY_WORKGROUP   = "Workgroup Config";

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private int correctOption; // 1 = A, 2 = B, 3 = C, 4 = D  (0 for interactive)
    private String learningOutcome;

    /** Question type: "static", "true_false", "interactive_cmd", or "interactive_ui". */
    private String type;

    /** Category string — used by GameRouter to pick the right interactive Activity. */
    private String category;

    /** Brief explanation shown after the user answers (optional, may be null). */
    private String explanation;

    // Default constructor for Room / serialization
    public Question() {
    }

    public Question(int id, String questionText, String optionA, String optionB,
                    String optionC, String optionD, int correctOption,
                    String learningOutcome, String type, String category,
                    String explanation) {
        this.id = id;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.learningOutcome = learningOutcome;
        this.type = type;
        this.category = category;
        this.explanation = explanation;
    }

    // ── Getters & Setters ───────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getQuestionText()             { return questionText; }
    public void setQuestionText(String t)       { this.questionText = t; }

    public String getOptionA()                  { return optionA; }
    public void setOptionA(String optionA)      { this.optionA = optionA; }

    public String getOptionB()                  { return optionB; }
    public void setOptionB(String optionB)      { this.optionB = optionB; }

    public String getOptionC()                  { return optionC; }
    public void setOptionC(String optionC)      { this.optionC = optionC; }

    public String getOptionD()                  { return optionD; }
    public void setOptionD(String optionD)      { this.optionD = optionD; }

    public int getCorrectOption()               { return correctOption; }
    public void setCorrectOption(int c)         { this.correctOption = c; }

    public String getLearningOutcome()           { return learningOutcome; }
    public void setLearningOutcome(String lo)    { this.learningOutcome = lo; }

    public String getType()                     { return type; }
    public void setType(String type)            { this.type = type; }

    public String getCategory()                 { return category; }
    public void setCategory(String category)    { this.category = category; }

    public String getExplanation()              { return explanation; }
    public void setExplanation(String e)        { this.explanation = e; }

    // ── Convenience ─────────────────────────────────────────────────

    /**
     * Returns {@code true} if this question should be handled inside QuizActivity
     * (i.e. it is a standard multiple-choice or true/false question).
     */
    public boolean isStaticQuestion() {
        return TYPE_STATIC.equals(type) || TYPE_TRUE_FALSE.equals(type);
    }

    /**
     * Returns {@code true} if this question requires launching an interactive Activity.
     */
    public boolean isInteractive() {
        return TYPE_INTERACTIVE_CMD.equals(type) || TYPE_INTERACTIVE_UI.equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id == question.id &&
                correctOption == question.correctOption &&
                Objects.equals(questionText, question.questionText) &&
                Objects.equals(optionA, question.optionA) &&
                Objects.equals(optionB, question.optionB) &&
                Objects.equals(optionC, question.optionC) &&
                Objects.equals(optionD, question.optionD) &&
                Objects.equals(learningOutcome, question.learningOutcome) &&
                Objects.equals(type, question.type) &&
                Objects.equals(category, question.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, questionText, optionA, optionB, optionC, optionD,
                correctOption, learningOutcome, type, category);
    }
}
