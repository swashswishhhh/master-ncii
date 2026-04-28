package com.example.servermasterncii;

import android.content.Context;
import android.util.TypedValue;

/**
 * ThemeColors — resolves theme-aware colors at runtime.
 *
 * Use this anywhere a layout has hardcoded hex colors that
 * need to change between Cyber Dark and Light Grid.
 *
 * Usage:
 *   view.setBackgroundColor(ThemeColors.bgCard(context));
 *   textView.setTextColor(ThemeColors.textPrimary(context));
 */
public class ThemeColors {

    public static int resolve(Context ctx, int attrRes) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attrRes, tv, true);
        return tv.data;
    }

    public static int bgDeepest(Context ctx) {
        return resolve(ctx, R.attr.bgDeepest);
    }

    public static int bgPrimary(Context ctx) {
        return resolve(ctx, R.attr.bgPrimary);
    }

    public static int bgSurface(Context ctx) {
        return resolve(ctx, R.attr.bgSurface);
    }

    public static int bgCard(Context ctx) {
        return resolve(ctx, R.attr.bgCard);
    }

    public static int textPrimary(Context ctx) {
        return resolve(ctx, R.attr.textPrimary);
    }

    public static int textSecondary(Context ctx) {
        return resolve(ctx, R.attr.textSecondary);
    }

    public static int textMuted(Context ctx) {
        return resolve(ctx, R.attr.textMuted);
    }

    public static int accent(Context ctx) {
        return resolve(ctx, R.attr.accentColor);
    }

    public static int accentDim(Context ctx) {
        return resolve(ctx, R.attr.accentColorDim);
    }

    public static int strokeLocked(Context ctx) {
        return resolve(ctx, R.attr.strokeLocked);
    }

    public static int divider(Context ctx) {
        return resolve(ctx, R.attr.dividerColor);
    }

    // Convenience: checks if current theme is light
    public static boolean isLight(Context ctx) {
        String theme = ThemeManager.getInstance().getSelectedTheme(ctx);
        return ThemeManager.THEME_LIGHT.equals(theme);
    }
}