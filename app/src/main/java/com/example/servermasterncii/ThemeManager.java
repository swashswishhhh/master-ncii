package com.example.servermasterncii;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * ThemeManager — Singleton for managing app-wide theme selection.
 * <p>
 * Persists the selected theme to {@link SharedPreferences} and applies it
 * to activities before {@code setContentView()} is called.
 *
 * <h3>Usage</h3>
 * <pre>
 * // In every Activity's onCreate(), BEFORE super.onCreate():
 * ThemeManager.applyTheme(this);
 * super.onCreate(savedInstanceState);
 * setContentView(...);
 * </pre>
 *
 * <h3>Available Themes</h3>
 * <ul>
 *     <li>{@link #THEME_CYBER} — Cyber Dark (default): neon green on deep black</li>
 *     <li>{@link #THEME_TERMINAL} — Terminal Green: bright lime on dark olive, monospace</li>
 *     <li>{@link #THEME_LIGHT} — Light Grid: cyan on white background</li>
 * </ul>
 *
 * <h3>Persistence</h3>
 * Uses {@code SharedPreferences} with:
 * <ul>
 *     <li>Name: {@value PREFS_NAME}</li>
 *     <li>Key: {@value KEY_SELECTED_THEME}</li>
 *     <li>Values: "cyber", "terminal", "light"</li>
 * </ul>
 */
public class ThemeManager {

    // ─── Constants ───────────────────────────────────────────────
    private static final String PREFS_NAME = "server_master_prefs";
    private static final String KEY_SELECTED_THEME = "selected_theme";

    /** Theme identifiers */
    public static final String THEME_CYBER = "cyber";
    public static final String THEME_TERMINAL = "terminal";
    public static final String THEME_LIGHT = "light";

    /** Default theme */
    private static final String DEFAULT_THEME = THEME_CYBER;

    // ─── Singleton Instance ──────────────────────────────────────
    private static ThemeManager instance;

    private ThemeManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton instance of ThemeManager.
     *
     * @return the ThemeManager instance
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════

    /**
     * Applies the currently selected theme to the given activity.
     * <p>
     * <b>CRITICAL:</b> This must be called in {@code onCreate()} BEFORE
     * {@code super.onCreate()} to ensure the theme is applied before
     * the activity's view hierarchy is created.
     *
     * @param activity the activity to apply the theme to
     */
    public static void applyTheme(Activity activity) {
        String theme = getInstance().getSelectedTheme(activity);
        int themeResId = getThemeResourceId(theme);
        activity.setTheme(themeResId);
    }

    /**
     * Gets the currently selected theme identifier.
     *
     * @param context the context to access SharedPreferences
     * @return the theme identifier ("cyber", "terminal", or "light")
     */
    public String getSelectedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_THEME, DEFAULT_THEME);
    }

    /**
     * Sets the selected theme and persists it to SharedPreferences.
     *
     * @param context the context to access SharedPreferences
     * @param theme   the theme identifier ("cyber", "terminal", or "light")
     */
    public void setSelectedTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_THEME, theme).apply();
    }

    /**
     * Gets the display name for a theme identifier.
     *
     * @param theme the theme identifier
     * @return the human-readable theme name
     */
    public static String getThemeDisplayName(String theme) {
        switch (theme) {
            case THEME_CYBER:
                return "Cyber Dark";
            case THEME_TERMINAL:
                return "Terminal Green";
            case THEME_LIGHT:
                return "Light Grid";
            default:
                return "Unknown";
        }
    }

    /**
     * Gets the description for a theme identifier.
     *
     * @param theme the theme identifier
     * @return the theme description
     */
    public static String getThemeDescription(String theme) {
        switch (theme) {
            case THEME_CYBER:
                return "Neon green on deep black — the classic cyberpunk aesthetic";
            case THEME_TERMINAL:
                return "Bright lime on dark olive — retro terminal vibes with monospace";
            case THEME_LIGHT:
                return "Cyan on white — clean and modern light theme";
            default:
                return "";
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Private Helpers
    // ═════════════════════════════════════════════════════════════

    /**
     * Maps a theme identifier to its corresponding theme resource ID.
     *
     * @param theme the theme identifier
     * @return the theme resource ID
     */
    private static int getThemeResourceId(String theme) {
        switch (theme) {
            case THEME_CYBER:
                return R.style.Theme_ServerMasterNCII_CyberDark;
            case THEME_TERMINAL:
                return R.style.Theme_ServerMasterNCII_TerminalGreen;
            case THEME_LIGHT:
                return R.style.Theme_ServerMasterNCII_LightGrid;
            default:
                return R.style.Theme_ServerMasterNCII_CyberDark;
        }
    }
}
