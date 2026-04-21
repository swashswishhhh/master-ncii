package com.example.servermasterncii;

import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * RankManager — Pure utility class for rank calculations.
 * <p>
 * Manages the 5-tier rank system based on skill points:
 * <ul>
 *     <li>TRAINEE [T-0] — 0-49 pts (gray)</li>
 *     <li>TECHNICIAN [T-1] — 50-149 pts (neon green)</li>
 *     <li>SPECIALIST [S-2] — 150-299 pts (cyan)</li>
 *     <li>ENGINEER [E-3] — 300-499 pts (gold)</li>
 *     <li>SERVER MASTER [SM-X] — 500+ pts (orange)</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * int skillPoints = RankManager.computeTotalSkillPoints(prefs, ALL_LEVEL_IDS);
 * RankInfo rank = RankManager.getRank(skillPoints);
 * String title = rank.title;  // "ENGINEER"
 * int color = rank.color;     // 0xFFFFD700
 * </pre>
 *
 * <h3>Design</h3>
 * Pure Java utility class with zero Android context dependency.
 * All methods are static. No constructor. Fully testable without Activity.
 */
public class RankManager {

    // ═══════════════════════════════════════════════════════════════════════
    // Rank Data Structure
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * RankInfo — Immutable data class representing a rank tier.
     */
    public static class RankInfo {
        public final String title;
        public final String badge;
        public final int color;
        public final int minPoints;
        public final int maxPoints;  // -1 means no upper limit (top rank)

        public RankInfo(String title, String badge, int color, int minPoints, int maxPoints) {
            this.title = title;
            this.badge = badge;
            this.color = color;
            this.minPoints = minPoints;
            this.maxPoints = maxPoints;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Rank Tiers
    // ═══════════════════════════════════════════════════════════════════════

    private static final RankInfo[] RANKS = {
            new RankInfo("TRAINEE",      "[T-0]",  Color.parseColor("#888888"), 0,   49),
            new RankInfo("TECHNICIAN",   "[T-1]",  Color.parseColor("#39FF7F"), 50,  149),
            new RankInfo("SPECIALIST",   "[S-2]",  Color.parseColor("#00BCD4"), 150, 299),
            new RankInfo("ENGINEER",     "[E-3]",  Color.parseColor("#FFD700"), 300, 499),
            new RankInfo("SERVER MASTER","[SM-X]", Color.parseColor("#FF6B35"), 500, -1)
    };

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gets the rank tier for the given skill points.
     *
     * @param skillPoints total skill points earned
     * @return the matching RankInfo
     */
    public static RankInfo getRank(int skillPoints) {
        for (RankInfo rank : RANKS) {
            if (rank.maxPoints == -1) {
                // Top rank (no upper limit)
                if (skillPoints >= rank.minPoints) {
                    return rank;
                }
            } else {
                // Normal rank (has upper limit)
                if (skillPoints >= rank.minPoints && skillPoints <= rank.maxPoints) {
                    return rank;
                }
            }
        }
        // Fallback (should never happen)
        return RANKS[0];
    }

    /**
     * Gets the rank title for the given skill points.
     *
     * @param skillPoints total skill points earned
     * @return rank title string (e.g., "ENGINEER")
     */
    public static String getRankTitle(int skillPoints) {
        return getRank(skillPoints).title;
    }

    /**
     * Gets the rank color for the given skill points.
     *
     * @param skillPoints total skill points earned
     * @return rank color as int (e.g., 0xFFFFD700)
     */
    public static int getRankColor(int skillPoints) {
        return getRank(skillPoints).color;
    }

    /**
     * Calculates progress percentage within the current rank tier.
     * <p>
     * For max rank (SERVER MASTER), always returns 100.
     *
     * @param skillPoints total skill points earned
     * @return progress percentage (0-100)
     */
    public static int getProgressPercent(int skillPoints) {
        RankInfo current = getRank(skillPoints);

        // Max rank reached
        if (current.maxPoints == -1) {
            return 100;
        }

        // Calculate progress within current tier
        int pointsInTier = skillPoints - current.minPoints;
        int tierRange = current.maxPoints - current.minPoints + 1;
        
        // Prevent division by zero
        if (tierRange <= 0) {
            return 0;
        }
        
        return Math.min(100, (pointsInTier * 100) / tierRange);
    }

    /**
     * Gets the next rank tier, or null if already at max rank.
     *
     * @param skillPoints total skill points earned
     * @return next RankInfo, or null if at SERVER MASTER
     */
    public static RankInfo getNextRank(int skillPoints) {
        RankInfo current = getRank(skillPoints);

        // Find current rank index
        int currentIndex = -1;
        for (int i = 0; i < RANKS.length; i++) {
            if (RANKS[i] == current) {
                currentIndex = i;
                break;
            }
        }

        // Already at max rank
        if (currentIndex == RANKS.length - 1) {
            return null;
        }

        return RANKS[currentIndex + 1];
    }

    /**
     * Computes total skill points from SharedPreferences.
     * <p>
     * Sums all {@code score_x.y} values for the given level IDs.
     *
     * @param prefs    SharedPreferences instance
     * @param levelIds array of level IDs (e.g., "1.1", "2.3")
     * @return total skill points
     * @throws IllegalArgumentException if prefs or levelIds is null
     */
    public static int computeTotalSkillPoints(SharedPreferences prefs, String[] levelIds) {
        if (prefs == null) {
            throw new IllegalArgumentException("SharedPreferences cannot be null");
        }
        if (levelIds == null) {
            throw new IllegalArgumentException("Level IDs array cannot be null");
        }
        
        int total = 0;
        for (String levelId : levelIds) {
            if (levelId != null) {
                total += prefs.getInt("score_" + levelId, 0);
            }
        }
        return total;
    }

    /**
     * Computes number of missions completed (score/total >= 0.70).
     *
     * @param prefs    SharedPreferences instance
     * @param levelIds array of level IDs
     * @return count of completed missions
     * @throws IllegalArgumentException if prefs or levelIds is null
     */
    public static int computeMissionsComplete(SharedPreferences prefs, String[] levelIds) {
        if (prefs == null) {
            throw new IllegalArgumentException("SharedPreferences cannot be null");
        }
        if (levelIds == null) {
            throw new IllegalArgumentException("Level IDs array cannot be null");
        }
        
        int count = 0;
        for (String levelId : levelIds) {
            if (levelId != null) {
                int score = prefs.getInt("score_" + levelId, 0);
                int total = prefs.getInt("total_" + levelId, 0);

                if (total > 0 && ((double) score / total) >= 0.70) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Computes total correct answers across all levels.
     *
     * @param prefs    SharedPreferences instance
     * @param levelIds array of level IDs
     * @return total correct answers
     */
    public static int computeTotalCorrect(SharedPreferences prefs, String[] levelIds) {
        return computeTotalSkillPoints(prefs, levelIds);  // Same as skill points
    }

    /**
     * Computes total attempted questions across all levels.
     *
     * @param prefs    SharedPreferences instance
     * @param levelIds array of level IDs
     * @return total attempted questions
     * @throws IllegalArgumentException if prefs or levelIds is null
     */
    public static int computeTotalAttempted(SharedPreferences prefs, String[] levelIds) {
        if (prefs == null) {
            throw new IllegalArgumentException("SharedPreferences cannot be null");
        }
        if (levelIds == null) {
            throw new IllegalArgumentException("Level IDs array cannot be null");
        }
        
        int total = 0;
        for (String levelId : levelIds) {
            if (levelId != null) {
                total += prefs.getInt("total_" + levelId, 0);
            }
        }
        return total;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private Constructor (prevent instantiation)
    // ═══════════════════════════════════════════════════════════════════════

    private RankManager() {
        throw new AssertionError("RankManager is a utility class and cannot be instantiated");
    }
}
