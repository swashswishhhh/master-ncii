package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivitySettingsBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * SettingsActivity — App settings with Theme, Account, and About sections.
 * <p>
 * <h3>Sections</h3>
 * <ul>
 *     <li><b>Theme</b> — Cyber Dark or Light Grid</li>
 *     <li><b>Account</b> — Opens ProfileActivity</li>
 *     <li><b>About</b> — Shows app information dialog</li>
 * </ul>
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private ThemeManager themeManager;
    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        // ── Block guest users ──────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("server_master_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);
        if (isGuest) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Access Restricted")
                    .setMessage("Settings are only available for registered accounts.\n\nSign in with Google or Facebook to access this feature.")
                    .setPositiveButton("OK", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return; // stop onCreate — no binding, no crash
        }
        // ──────────────────────────────────────────────────────────

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        themeManager = ThemeManager.getInstance();
        currentTheme = themeManager.getSelectedTheme(this);

        setupToolbar();
        setupThemeCards();
        setupAccountRow();
        setupAboutRow();
        setupLogoutRow();
        updateSelectedTheme();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupThemeCards() {
        binding.cardCyberDark.setOnClickListener(v -> applyTheme(ThemeManager.THEME_CYBER));
        binding.cardLightGrid.setOnClickListener(v -> applyTheme(ThemeManager.THEME_LIGHT));
    }

    private void setupAccountRow() {
        binding.cardAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void setupLogoutRow() {
        binding.cardLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Terminate Session")
                    .setMessage("Are you sure you want to log out?\n\nYour progress is saved to your account.")
                    .setPositiveButton("LOGOUT", (dialog, which) -> {
                        getSharedPreferences("server_master_prefs", MODE_PRIVATE)
                                .edit().clear().apply();
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void setupAboutRow() {
        binding.cardAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void applyTheme(String theme) {
        if (theme.equals(currentTheme)) {
            return;
        }
        themeManager.setSelectedTheme(this, theme);
        navigateWithNewTheme();
    }

    private void navigateWithNewTheme() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateSelectedTheme() {
        resetCard(binding.cardCyberDark, binding.badgeCyberDark);
        resetCard(binding.cardLightGrid, binding.badgeLightGrid);

        switch (currentTheme) {
            case ThemeManager.THEME_CYBER:
                highlightCard(binding.cardCyberDark, binding.badgeCyberDark, 0xFF39FF7F);
                break;
            case ThemeManager.THEME_LIGHT:
                highlightCard(binding.cardLightGrid, binding.badgeLightGrid, 0xFF00BCD4);
                break;
            case ThemeManager.THEME_TERMINAL:
                highlightCard(binding.cardCyberDark, binding.badgeCyberDark, 0xFF39FF7F);
                break;
        }
    }

    private void resetCard(MaterialCardView card, View badge) {
        card.setStrokeColor(0x00000000);
        card.setStrokeWidth(0);
        badge.setVisibility(View.GONE);
    }

    private void highlightCard(MaterialCardView card, View badge, int color) {
        card.setStrokeColor(color);
        card.setStrokeWidth(dpToPx(3));
        badge.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("About BrainTap")
                .setMessage("Version 1.0\n\n" +
                        "A cyberpunk-themed learning assessment app for Computer Technology Student CNSC-COTT\n\n" +
                        "© 2026 BrainTap")
                .setPositiveButton("OK", null)
                .show();
    }
}