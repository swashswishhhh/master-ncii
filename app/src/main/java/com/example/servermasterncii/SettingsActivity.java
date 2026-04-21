package com.example.servermasterncii;

import android.content.Intent;
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
        // Only Cyber Dark and Light Grid themes
        binding.cardCyberDark.setOnClickListener(v -> applyTheme(ThemeManager.THEME_CYBER));
        binding.cardLightGrid.setOnClickListener(v -> applyTheme(ThemeManager.THEME_LIGHT));
    }

    private void setupAccountRow() {
        binding.cardAccount.setOnClickListener(v -> {
            // TODO: Create ProfileActivity and uncomment this
            // Intent intent = new Intent(this, ProfileActivity.class);
            // startActivity(intent);
            
            // Temporary: Show coming soon message
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Profile")
                    .setMessage("Profile feature coming soon!\n\nThis will show your stats, rank, achievements, and more.")
                    .setPositiveButton("OK", null)
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
        // Reset all cards
        resetCard(binding.cardCyberDark, binding.badgeCyberDark);
        resetCard(binding.cardLightGrid, binding.badgeLightGrid);

        // Highlight selected theme
        switch (currentTheme) {
            case ThemeManager.THEME_CYBER:
                highlightCard(binding.cardCyberDark, binding.badgeCyberDark, 0xFF39FF7F);
                break;
            case ThemeManager.THEME_LIGHT:
                highlightCard(binding.cardLightGrid, binding.badgeLightGrid, 0xFF00BCD4);
                break;
            case ThemeManager.THEME_TERMINAL:
                // Terminal theme still exists in ThemeManager but not shown in Settings
                // If user somehow has Terminal selected, show Cyber Dark as active
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
                .setTitle("About Server Master NC II")
                .setMessage("Version 1.0\n\n" +
                        "A cyberpunk-themed learning app for CSS NC II certification.\n\n" +
                        "Complete missions, earn ranks, and master server administration skills.\n\n" +
                        "© 2026 Server Master NC II")
                .setPositiveButton("OK", null)
                .show();
    }
}
