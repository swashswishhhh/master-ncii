package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servermasterncii.databinding.ActivityProfileBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProfileActivity — displays user stats, rank, and account customization.
 *
 * Stats are computed from the SAME level list that MainActivity uses:
 *   - 21 hardcoded base levels (BASE_LEVEL_IDS)
 *   - ALL published Firestore missions (not just played ones)
 *
 * This guarantees that "5 / 21" on Profile always matches
 * "5 of 21 missions complete" on the Saga Map.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "server_master_prefs";
    private static final String KEY_SCORE  = "score_";
    private static final String KEY_TOTAL  = "total_";

    // The same 21 hardcoded levels as MainActivity.ALL_LEVEL_IDS
    private static final String[] BASE_LEVEL_IDS = {
            "1.1","1.2","1.3","1.4","1.5",
            "2.1","2.2","2.3","2.4","2.5","2.6","2.7",
            "2.8","2.9","2.10","2.11","2.12",
            "3.1","3.2","3.3","3.4"
    };

    private ActivityProfileBinding binding;
    private SharedPreferences prefs;

    // The resolved full level ID list — populated after Firestore fetch
    private List<String> allLevelIds = new ArrayList<>(Arrays.asList(BASE_LEVEL_IDS));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupToolbar();
        populateProfile();

        // Show stats immediately with the 21 base levels,
        // then re-render once Firestore missions are loaded.
        populateStats();
        populateRank();
        setupButtons();

        // Fetch all published Firestore missions — same query as MainActivity
        fetchFirestoreMissionsAndRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // =====================================================================
    // Toolbar
    // =====================================================================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PROFILE");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    // =====================================================================
    // Firestore mission fetch — mirrors MainActivity.loadFirestoreMissions()
    // =====================================================================

    /**
     * Fetches all published Firestore missions and appends their IDs to
     * allLevelIds, then re-renders stats and rank.
     *
     * Uses the EXACT same Firestore query as MainActivity so the total
     * mission count is always identical on both screens.
     */
    private void fetchFirestoreMissionsAndRefresh() {
        FirebaseFirestore.getInstance()
                .collection("missions")
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Start fresh from the 21 base levels
                    allLevelIds = new ArrayList<>(Arrays.asList(BASE_LEVEL_IDS));

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String missionId = doc.getString("missionId");
                        if (missionId == null) continue;

                        // Avoid duplicates (same guard as MainActivity)
                        if (!allLevelIds.contains(missionId)) {
                            allLevelIds.add(missionId);
                        }
                    }

                    // Re-render with the full, correct list
                    runOnUiThread(() -> {
                        populateStats();
                        populateRank();
                    });
                })
                .addOnFailureListener(e -> {
                    // Firestore unavailable — stats already shown with 21 base levels,
                    // which is a safe fallback. No crash, no blank screen.
                    android.util.Log.w("ProfileActivity",
                            "Could not fetch Firestore missions: " + e.getMessage());
                });
    }

    // =====================================================================
    // Profile header
    // =====================================================================

    private void populateProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.isAnonymous()) {
            binding.tvDisplayName.setText("GUEST_OPERATIVE");
            binding.tvEmail.setText("No account — progress is local only");
            binding.tvAccountType.setText("[ GUEST MODE ]");
            binding.btnEditName.setVisibility(View.GONE);
            return;
        }

        String name  = user.getDisplayName();
        String email = user.getEmail();

        binding.tvDisplayName.setText(
                name != null && !name.isEmpty()
                        ? name.toUpperCase(Locale.ROOT)
                        : "OPERATIVE");
        binding.tvEmail.setText(email != null ? email : "");
        binding.tvAccountType.setText("[ AUTHENTICATED ]");
        binding.btnEditName.setVisibility(View.VISIBLE);
    }

    // =====================================================================
    // Stats — uses allLevelIds which now matches MainActivity exactly
    // =====================================================================

    private void populateStats() {
        int totalSkillPoints = 0;
        int completedCount   = 0;
        int totalLevels      = allLevelIds.size();
        int totalProgress    = 0;

        for (String levelId : allLevelIds) {
            int score = prefs.getInt(KEY_SCORE + levelId, 0);
            int total = prefs.getInt(KEY_TOTAL + levelId, 0);

            totalSkillPoints += score;

            // Compute per-level progress % the same way MainActivity does:
            // level.getProgressPercent() = min(100, (score/total)*100)
            int pct = (total > 0) ? Math.min(100, (int) ((score / (float) total) * 100)) : 0;
            totalProgress += pct;

            // Completed = same 70% threshold as MainActivity.Level.isCompleted()
            if (total > 0 && pct >= 70) completedCount++;
        }

        // Overall mastery = same formula as MainActivity.updateHallOfFame()
        int overallPercent = totalLevels > 0 ? totalProgress / totalLevels : 0;

        binding.tvSkillPoints.setText(String.valueOf(totalSkillPoints));
        binding.tvMissionsCompleted.setText(completedCount + " / " + totalLevels);
        binding.tvOverallMastery.setText(overallPercent + "%");
        binding.progressMastery.setProgress(overallPercent, true);
    }

    // =====================================================================
    // Rank — uses same overallPercent formula as populateStats()
    // =====================================================================

    private void populateRank() {
        int totalProgress = 0;
        int totalLevels   = allLevelIds.size();

        for (String levelId : allLevelIds) {
            int score = prefs.getInt(KEY_SCORE + levelId, 0);
            int total = prefs.getInt(KEY_TOTAL + levelId, 0);
            int pct   = (total > 0) ? Math.min(100, (int) ((score / (float) total) * 100)) : 0;
            totalProgress += pct;
        }

        int overallPercent = totalLevels > 0 ? totalProgress / totalLevels : 0;

        String rank;
        String rankIcon;
        int    rankColor;

        if (overallPercent == 100) {
            rank      = "SERVER MASTER";
            rankIcon  = "⬡";
            rankColor = 0xFF00F5FF;
        } else if (overallPercent >= 80) {
            rank      = "SENIOR ENGINEER";
            rankIcon  = "◈";
            rankColor = 0xFF39FF14;
        } else if (overallPercent >= 60) {
            rank      = "ENGINEER";
            rankIcon  = "◇";
            rankColor = 0xFF39FF7F;
        } else if (overallPercent >= 40) {
            rank      = "SPECIALIST";
            rankIcon  = "○";
            rankColor = 0xFFFFD700;
        } else if (overallPercent >= 20) {
            rank      = "TECHNICIAN";
            rankIcon  = "△";
            rankColor = 0xFFFFA500;
        } else {
            rank      = "RECRUIT";
            rankIcon  = "□";
            rankColor = 0xFF888888;
        }

        binding.tvRankIcon.setText(rankIcon);
        binding.tvRankTitle.setText(rank);
        binding.tvRankTitle.setTextColor(rankColor);
        binding.tvRankIcon.setTextColor(rankColor);

        if (overallPercent < 100) {
            int nextThreshold = overallPercent < 20 ? 20
                    : overallPercent < 40 ? 40
                    : overallPercent < 60 ? 60
                    : overallPercent < 80 ? 80
                    : 100;
            binding.tvRankHint.setText(
                    String.format(Locale.getDefault(),
                            "Reach %d%% mastery to rank up", nextThreshold));
        } else {
            binding.tvRankHint.setText("Maximum rank achieved!");
        }
    }

    // =====================================================================
    // Buttons
    // =====================================================================

    private void setupButtons() {
        binding.btnEditName.setOnClickListener(v -> showEditNameDialog());
        binding.btnSyncProgress.setOnClickListener(v -> syncProgress());
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    // =====================================================================
    // Edit display name
    // =====================================================================

    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Enter your display name");
        String current = user.getDisplayName();
        if (current != null) input.setText(current);
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Display Name")
                .setView(input)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveDisplayName(user, newName);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveDisplayName(FirebaseUser user, String newName) {
        setLoading(true);

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdate)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("displayName", newName);

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(v2 -> {
                                setLoading(false);
                                binding.tvDisplayName.setText(newName.toUpperCase(Locale.ROOT));
                                Toast.makeText(this, "✅ Display name updated!",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "Name updated locally. Firestore sync failed.",
                                        Toast.LENGTH_SHORT).show();
                                binding.tvDisplayName.setText(newName.toUpperCase(Locale.ROOT));
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to update name: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // =====================================================================
    // Manual progress sync
    // =====================================================================

    private void syncProgress() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(this,
                    "Sign in to sync progress across devices",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        ProgressManager.pullFromFirestore(prefs, new ProgressManager.OnSyncCompleteListener() {
            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    setLoading(false);
                    // Re-fetch Firestore missions then refresh stats
                    fetchFirestoreMissionsAndRefresh();
                    Toast.makeText(ProfileActivity.this,
                            "✅ Progress synced!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this,
                            "Sync failed: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // =====================================================================
    // Logout
    // =====================================================================

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Terminate Session")
                .setMessage("Are you sure you want to log out?\n\nYour progress is saved to your account.")
                .setPositiveButton("LOGOUT", (dialog, which) -> logout())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void logout() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =====================================================================
    // UI helpers
    // =====================================================================

    private void setLoading(boolean loading) {
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEditName.setEnabled(!loading);
        binding.btnSyncProgress.setEnabled(!loading);
        binding.btnLogout.setEnabled(!loading);
    }
}