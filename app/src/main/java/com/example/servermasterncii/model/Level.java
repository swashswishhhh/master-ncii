package com.example.servermasterncii.model;

/**
 * Data class representing a single level on the Saga Map.
 * <p>
 * Each level maps to one section (e.g. 1.1, 2.5, 3.4) in the CSS NC II
 * curriculum and carries display metadata (title, subtitle, progress, lock state,
 * Lottie animation filename) for the {@link com.example.servermasterncii.LevelAdapter}.
 * <p>
 * Levels are grouped into 3 chapters:
 * <ul>
 *     <li>Chapter 1 — User Access (1.1–1.5)</li>
 *     <li>Chapter 2 — Server Setup (2.1–2.12)</li>
 *     <li>Chapter 3 — Maintenance (3.1–3.4)</li>
 * </ul>
 */
public class Level {

    private final int levelNumber;
    private final String title;
    private final String subtitle;
    private final String levelId;         // "1.1", "2.5", etc. — passed to QuizActivity
    private final String lottieAsset;     // filename inside assets/ e.g. "anim_network.json"
    private final int chapter;            // 1, 2, or 3
    private final String levelType;       // "Standard_Quiz", "Interactive_CMD", "Interactive_UI"
    private int progressPercent;          // 0–100
    private boolean unlocked;

    public Level(int levelNumber, String title, String subtitle,
                 String levelId, String lottieAsset,
                 int chapter, String levelType,
                 int progressPercent, boolean unlocked) {
        this.levelNumber = levelNumber;
        this.title = title;
        this.subtitle = subtitle;
        this.levelId = levelId;
        this.lottieAsset = lottieAsset;
        this.chapter = chapter;
        this.levelType = levelType;
        this.progressPercent = progressPercent;
        this.unlocked = unlocked;
    }

    // ── Getters ──────────────────────────────────────────────

    public int getLevelNumber()       { return levelNumber; }
    public String getTitle()          { return title; }
    public String getSubtitle()       { return subtitle; }
    public String getLevelId()        { return levelId; }
    public String getLottieAsset()    { return lottieAsset; }
    public int getChapter()           { return chapter; }
    public String getLevelType()      { return levelType; }
    public int getProgressPercent()   { return progressPercent; }
    public boolean isUnlocked()       { return unlocked; }

    /** Returns true if the level has been completed (score ≥ 70%). */
    public boolean isCompleted()      { return progressPercent >= 70; }

    // ── Setters for mutable state ────────────────────────────

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}
