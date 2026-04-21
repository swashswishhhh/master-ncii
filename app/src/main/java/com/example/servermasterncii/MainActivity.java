package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.servermasterncii.databinding.ActivityMainBinding;
import com.example.servermasterncii.model.Level;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Saga Map — the main hub of Server Master NC II.
 * <p>
 * Displays 21 levels across 3 CSS NC II Learning Outcome chapters,
 * selectable via a {@link com.google.android.material.bottomnavigation.BottomNavigationView}.
 * Each chapter's levels are shown as Mission Cards in a 2-column grid.
 *
 * <h3>Chapter / Level Structure</h3>
 * <ul>
 *     <li>Chapter 1 — User Access: levels 1.1 – 1.5</li>
 *     <li>Chapter 2 — Server Setup: levels 2.1 – 2.12</li>
 *     <li>Chapter 3 — Maintenance: levels 3.1 – 3.4</li>
 * </ul>
 *
 * <h3>Locking System</h3>
 * Levels must be completed sequentially within each chapter (≥ 70% score).
 * Chapter 2 unlocks when level 1.5 is completed. Chapter 3 unlocks when
 * level 2.12 is completed.
 *
 * <h3>Hall of Fame</h3>
 * The header card aggregates "Skill Points" (total correct answers across
 * all levels) and overall mastery percentage.
 *
 * <h3>Persistence</h3>
 * Uses {@link SharedPreferences} ({@value PREFS_NAME}) to persist:
 * <ul>
 *     <li>{@code score_x.y}    — best score for each level (0–N)</li>
 *     <li>{@code total_x.y}    — total questions attempted for each level</li>
 *     <li>{@code unlocked_x.y} — whether a level has been unlocked</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity implements LevelAdapter.OnLevelClickListener {

    // ─── Constants ───────────────────────────────────────────────
    private static final String PREFS_NAME = "server_master_prefs";
    private static final double UNLOCK_THRESHOLD = 0.70; // 70% to unlock next level

    /** SharedPreferences key prefixes */
    private static final String KEY_SCORE    = "score_";    // e.g. "score_1.3"
    private static final String KEY_TOTAL    = "total_";    // e.g. "total_1.3"
    private static final String KEY_UNLOCKED = "unlocked_"; // e.g. "unlocked_1.4"

    /** Chapter titles and Lottie assets */
    private static final String[] CHAPTER_TITLES = {
            "CHAPTER 1: USER ACCESS",
            "CHAPTER 2: SERVER SETUP",
            "CHAPTER 3: MAINTENANCE"
    };
    private static final String[] CHAPTER_SUBTITLES = {
            "Levels 1.1 – 1.5",
            "Levels 2.1 – 2.12",
            "Levels 3.1 – 3.4"
    };
    private static final String[] CHAPTER_LOTTIE = {
            "anim_shield.json",
            "anim_network.json",
            "anim_terminal.json"
    };

    // ─── UI (ViewBinding) ────────────────────────────────────────
    private ActivityMainBinding binding;

    // ─── Data ────────────────────────────────────────────────────
    private LevelAdapter adapter;
    private List<Level> allLevels;
    private List<Level> displayedLevels;
    private SharedPreferences prefs;
    private int currentChapter = 1;

    // Tracks which simulator level is pending a result
    private String pendingSimulatorLevelId;

    // Launcher for simulator activities (saves score on completion)
    private final ActivityResultLauncher<Intent> simulatorLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getBooleanExtra("extra_task_completed", false)
                                && pendingSimulatorLevelId != null) {

                            prefs.edit()
                                    .putInt(KEY_SCORE + pendingSimulatorLevelId, 1)
                                    .putInt(KEY_TOTAL + pendingSimulatorLevelId, 1)
                                    .putBoolean(KEY_UNLOCKED + pendingSimulatorLevelId, true)
                                    .apply();

                            pendingSimulatorLevelId = null;
                            refreshLevels();
                            updateHallOfFame();
                        }
                    }
            );

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate()
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        // DO NOT call EdgeToEdge.enable(this) — it causes the nav bar overlap
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        allLevels = buildAllLevels();

        displayedLevels = filterByChapter(currentChapter);
        adapter = new LevelAdapter(displayedLevels, this);

        GridLayoutManager gridLayout = new GridLayoutManager(this, 2);
        binding.recyclerLevels.setLayoutManager(gridLayout);
        binding.recyclerLevels.setAdapter(adapter);
        binding.recyclerLevels.setHasFixedSize(false);

        setupBottomNav();
        updateChapterUI(currentChapter);
        updateHallOfFame();
        setupSettingsButton();

        loadFirestoreMissions(); // ← Add this line
    }



    /**
     * Re-sync progress and lock state whenever the user returns from
     * QuizActivity / ResultActivity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshLevels();
        updateHallOfFame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // =====================================================================
    // Bottom Navigation
    // =====================================================================

    /**
     * Wires the Settings button to open SettingsActivity.
     */
    private void setupSettingsButton() {
        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Wires the BottomNavigationView to switch between chapters.
     */
    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chapter_1) {
                switchToChapter(1);
                return true;
            } else if (id == R.id.nav_chapter_2) {
                switchToChapter(2);
                return true;
            } else if (id == R.id.nav_chapter_3) {
                switchToChapter(3);
                return true;
            }
            return false;
        });
    }

    /**
     * Switches the displayed levels to the given chapter.
     */
    private void switchToChapter(int chapter) {
        currentChapter = chapter;
        displayedLevels.clear();
        displayedLevels.addAll(filterByChapter(chapter));
        adapter.notifyDataSetChanged();
        updateChapterUI(chapter);

        // Scroll to top when switching chapters
        binding.recyclerLevels.scrollToPosition(0);
    }

    /**
     * Updates the chapter banner title, subtitle, and Lottie icon.
     */
    private void updateChapterUI(int chapter) {
        int idx = chapter - 1;
        binding.tvChapterTitle.setText(CHAPTER_TITLES[idx]);
        binding.tvChapterSubtitle.setText(CHAPTER_SUBTITLES[idx]);
        binding.lottieChapterIcon.setAnimation(CHAPTER_LOTTIE[idx]);
        binding.lottieChapterIcon.playAnimation();
    }

    // =====================================================================
    // Level data — 21 levels across 3 chapters
    // =====================================================================

    /**
     * Constructs the master list of all 21 NC II levels.
     * <p>
     * Each level's unlock state is derived from SharedPreferences.
     * The first level of each chapter has special unlock rules:
     * <ul>
     *     <li>1.1 — always unlocked</li>
     *     <li>2.1 — unlocked when 1.5 is completed</li>
     *     <li>3.1 — unlocked when 2.12 is completed</li>
     * </ul>
     */
    private List<Level> buildAllLevels() {
        List<Level> list = new ArrayList<>();

        // ═══════════════════════════════════════════════════
        // CHAPTER 1: USER ACCESS (1.1 – 1.5)
        // ═══════════════════════════════════════════════════
        list.add(level(1,  "P2P Networks",       "Peer-to-Peer Basics",       "1.1", "anim_network.json",          1, "Standard_Quiz",   true));
        list.add(level(2,  "Workgroups",          "Windows Workgroup Setup",   "1.2", "anim_workgroup.json",        1, "Standard_Quiz",   isLevelUnlocked("1.2")));
        list.add(level(3,  "Firewalls",           "Windows Firewall Config",   "1.3", "anim_shield.json",           1, "Interactive_UI",  isLevelUnlocked("1.3")));
        list.add(level(4,  "IP Addressing",       "IPv4 & Subnetting",         "1.4", "anim_ip_address.json",       1, "Interactive_UI",  isLevelUnlocked("1.4")));
        list.add(level(5,  "CMD Basics",          "Terminal Commands",          "1.5", "anim_terminal.json",         1, "Interactive_CMD", isLevelUnlocked("1.5")));

        // ═══════════════════════════════════════════════════
        // CHAPTER 2: SERVER SETUP (2.1 – 2.12)
        // ═══════════════════════════════════════════════════
        list.add(level(6,  "Server Roles",        "Intro to Server Roles",     "2.1",  "anim_server_roles.json",    2, "Standard_Quiz",   isLevelUnlocked("2.1")));
        list.add(level(7,  "Windows Server",      "OS Installation",           "2.2",  "anim_windows_server.json",  2, "Standard_Quiz",   isLevelUnlocked("2.2")));
        list.add(level(8,  "Active Directory",    "Domain Services",           "2.3",  "anim_active_directory.json", 2, "Standard_Quiz",   isLevelUnlocked("2.3")));
        list.add(level(9,  "DNS Server",          "Name Resolution",           "2.4",  "anim_dns.json",             2, "Standard_Quiz",   isLevelUnlocked("2.4")));
        list.add(level(10, "DHCP Server",         "Dynamic IP Allocation",     "2.5",  "anim_dhcp.json",            2, "Standard_Quiz",   isLevelUnlocked("2.5")));
        list.add(level(11, "File Server",         "Shared Storage",            "2.6",  "anim_file_server.json",     2, "Standard_Quiz",   isLevelUnlocked("2.6")));
        list.add(level(12, "Print Server",        "Network Printing",          "2.7",  "anim_print_server.json",    2, "Standard_Quiz",   isLevelUnlocked("2.7")));
        list.add(level(13, "Group Policy",        "GPO Management",            "2.8",  "anim_group_policy.json",    2, "Standard_Quiz",   isLevelUnlocked("2.8")));
        list.add(level(14, "Backup Server",       "Data Backup",               "2.9",  "anim_backup.json",          2, "Standard_Quiz",   isLevelUnlocked("2.9")));
        list.add(level(15, "Remote Desktop",      "RDP Configuration",         "2.10", "anim_remote_desktop.json",  2, "Interactive_UI",  isLevelUnlocked("2.10")));
        list.add(level(16, "Server Security",     "Hardening Servers",         "2.11", "anim_server_security.json", 2, "Standard_Quiz",   isLevelUnlocked("2.11")));
        list.add(level(17, "Monitoring",          "Performance Monitor",       "2.12", "anim_monitoring.json",      2, "Standard_Quiz",   isLevelUnlocked("2.12")));

        // ═══════════════════════════════════════════════════
        // CHAPTER 3: MAINTENANCE (3.1 – 3.4)
        // ═══════════════════════════════════════════════════
        list.add(level(18, "Troubleshooting",     "Network Diagnostics",       "3.1", "anim_troubleshoot.json",     3, "Interactive_CMD", isLevelUnlocked("3.1")));
        list.add(level(19, "Updates & Patches",   "System Maintenance",        "3.2", "anim_updates.json",          3, "Standard_Quiz",   isLevelUnlocked("3.2")));
        list.add(level(20, "Documentation",       "IT Documentation",          "3.3", "anim_documentation.json",    3, "Standard_Quiz",   isLevelUnlocked("3.3")));
        list.add(level(21, "Decommission",        "Server Retirement",         "3.4", "anim_decommission.json",     3, "Standard_Quiz",   isLevelUnlocked("3.4")));

        return list;
    }

    /**
     * Fetches admin-created missions from Firestore and appends
     * them to allLevels for the current chapter display.
     */
    private void loadFirestoreMissions() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("missions")
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean newMissionsAdded = false;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String missionId   = doc.getString("missionId");
                        String title       = doc.getString("title");
                        String description = doc.getString("description");
                        String chapterId   = doc.getString("chapterId");
                        String difficulty  = doc.getString("difficulty");

                        if (missionId == null || title == null || chapterId == null) continue;

                        // Convert chapterId to chapter number
                        int chapterNum = 1;
                        if ("chapter_2".equals(chapterId)) chapterNum = 2;
                        else if ("chapter_3".equals(chapterId)) chapterNum = 3;

                        // Check if this mission already exists in allLevels
                        boolean alreadyExists = false;
                        for (Level l : allLevels) {
                            if (missionId.equals(l.getLevelId())) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            // Pick a lottie based on chapter
                            String lottie = "anim_network.json";
                            if (chapterNum == 2) lottie = "anim_server_roles.json";
                            else if (chapterNum == 3) lottie = "anim_terminal.json";

                            Level firestoreLevel = new Level(
                                    allLevels.size() + 1,  // sequential number
                                    title,
                                    description != null ? description : "",
                                    missionId,             // e.g. "mission_1_6"
                                    lottie,
                                    chapterNum,
                                    "Firestore_Quiz",      // type flag
                                    0,                     // progress starts at 0
                                    true                   // published = unlocked
                            );
                            allLevels.add(firestoreLevel);
                            newMissionsAdded = true;
                        }
                    }

                    if (newMissionsAdded) {
                        // Refresh the current chapter display
                        displayedLevels.clear();
                        displayedLevels.addAll(filterByChapter(currentChapter));
                        adapter.notifyDataSetChanged();
                        updateHallOfFame();
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("MainActivity", "Failed to load Firestore missions", e));
    }

    /**
     * Factory method for constructing a Level with progress from SharedPreferences.
     */
    private Level level(int num, String title, String subtitle, String levelId,
                        String lottie, int chapter, String levelType, boolean unlocked) {
        return new Level(num, title, subtitle, levelId, lottie,
                chapter, levelType, getProgressForLevel(levelId), unlocked);
    }

    /**
     * Returns only the levels belonging to the given chapter number.
     */
    private List<Level> filterByChapter(int chapter) {
        return allLevels.stream()
                .filter(l -> l.getChapter() == chapter)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // SharedPreferences — persistence helpers
    // =====================================================================

    /**
     * Returns the completion percentage (0–100) for a given level.
     * Calculated as (bestScore / totalQuestions) * 100.
     */
    private int getProgressForLevel(String levelId) {
        int score = prefs.getInt(KEY_SCORE + levelId, 0);
        int total = prefs.getInt(KEY_TOTAL + levelId, 0);
        if (total == 0) return 0;
        return Math.min(100, (int) ((score / (float) total) * 100));
    }

    /**
     * Checks if a level is unlocked via SharedPreferences.
     * <p>
     * The unlock cascades: to be unlocked, the <i>previous</i> level must
     * have a score ≥ {@link #UNLOCK_THRESHOLD}.
     * <p>
     * Cross-chapter gateway rules:
     * <ul>
     *     <li>1.1 → always unlocked</li>
     *     <li>2.1 → unlocked when 1.5 is completed</li>
     *     <li>3.1 → unlocked when 2.12 is completed</li>
     * </ul>
     */
    private boolean isLevelUnlocked(String levelId) {
        if ("1.1".equals(levelId)) return true;

        // Firestore missions ("mission_1_1") are always unlocked —
        // they are admin-created bonus content, not part of the gate chain
        if (levelId.startsWith("mission_")) {
            return true;
        }

        // Check explicit unlock flag first
        if (prefs.getBoolean(KEY_UNLOCKED + levelId, false)) return true;

        // Derive the previous level's ID and check its score
        String previousLevelId = getPreviousLevelId(levelId);
        if (previousLevelId != null) {
            int prevScore = prefs.getInt(KEY_SCORE + previousLevelId, 0);
            int prevTotal = prefs.getInt(KEY_TOTAL + previousLevelId, 0);
            if (prevTotal > 0 && (prevScore / (double) prevTotal) >= UNLOCK_THRESHOLD) {
                prefs.edit().putBoolean(KEY_UNLOCKED + levelId, true).apply();
                return true;
            }
        }

        return false;
    }

    /**
     * Maps a level ID to the ID of the level before it.
     * Handles both intra-chapter sequencing and cross-chapter gateways.
     */
    private String getPreviousLevelId(String levelId) {
        switch (levelId) {
            // Chapter 1 progression
            case "1.2":  return "1.1";
            case "1.3":  return "1.2";
            case "1.4":  return "1.3";
            case "1.5":  return "1.4";

            // Chapter 2 gateway + progression
            case "2.1":  return "1.5";   // cross-chapter gate
            case "2.2":  return "2.1";
            case "2.3":  return "2.2";
            case "2.4":  return "2.3";
            case "2.5":  return "2.4";
            case "2.6":  return "2.5";
            case "2.7":  return "2.6";
            case "2.8":  return "2.7";
            case "2.9":  return "2.8";
            case "2.10": return "2.9";
            case "2.11": return "2.10";
            case "2.12": return "2.11";

            // Chapter 3 gateway + progression
            case "3.1":  return "2.12";  // cross-chapter gate
            case "3.2":  return "3.1";
            case "3.3":  return "3.2";
            case "3.4":  return "3.3";

            default:     return null;
        }
    }

    // =====================================================================
    // Hall of Fame — aggregate stats
    // =====================================================================

    /**
     * Updates the Hall of Fame header with aggregated stats across ALL
     * 21 levels, regardless of which chapter tab is selected.
     */
    private void updateHallOfFame() {
        int totalSkillPoints = 0;
        int totalProgress = 0;
        int completedCount = 0;

        for (Level level : allLevels) {
            totalSkillPoints += prefs.getInt(KEY_SCORE + level.getLevelId(), 0);
            totalProgress += level.getProgressPercent();
            if (level.isCompleted()) completedCount++;
        }

        int overallPercent = allLevels.isEmpty() ? 0 : totalProgress / allLevels.size();

        binding.tvSkillPoints.setText(String.valueOf(totalSkillPoints));
        binding.progressOverall.setProgress(overallPercent);
        binding.tvOverallPercent.setText(
                String.format(Locale.getDefault(), "%d%%", overallPercent));
        binding.tvLevelsCompleted.setText(
                String.format(Locale.getDefault(),
                        "%d of %d missions complete", completedCount, allLevels.size()));
    }

    // =====================================================================
    // Refresh
    // =====================================================================

    /**
     * Re-reads SharedPreferences and updates every level's progress
     * and lock state. Called in {@link #onResume()}.
     */
    private void refreshLevels() {
        for (Level level : allLevels) {
            level.setProgressPercent(getProgressForLevel(level.getLevelId()));
            // Firestore missions stay unlocked always — don't let isLevelUnlocked() re-lock them
            if (level.getLevelId().startsWith("mission_")) {
                level.setUnlocked(true);
            } else {
                level.setUnlocked(isLevelUnlocked(level.getLevelId()));
            }
        }
        displayedLevels.clear();
        displayedLevels.addAll(filterByChapter(currentChapter));
        adapter.notifyDataSetChanged();
    }

    // =====================================================================
    // Level click handling
    // =====================================================================

    /**
     * Handles a tap on a level card.
     * <p>
     * If locked → toast message. If unlocked → launch QuizActivity with
     * the level's ID and type as extras. If no questions exist for the level,
     * shows a "Coming Soon" dialog.
     */
    @Override
    public void onLevelClick(Level level) {
        if (!level.isUnlocked()) {
            Toast.makeText(this, "⚡ Complete the previous mission to unlock!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String levelId = level.getLevelId();

        // Direct routing for simulator levels
        switch (levelId) {
            case "1.3":
                pendingSimulatorLevelId = levelId;
                simulatorLauncher.launch(new Intent(this, FirewallSimulatorActivity.class));
                return;

            case "1.4":
                pendingSimulatorLevelId = levelId;
                simulatorLauncher.launch(new Intent(this, IPConfigSimulatorActivity.class));
                return;

            case "1.5":
                pendingSimulatorLevelId = levelId;
                simulatorLauncher.launch(new Intent(this, TerminalEmulatorActivity.class));
                return;

            default:
                // Check if this is a Firestore-created mission
                if ("Firestore_Quiz".equals(level.getLevelType())) {
                    // missionId format: "mission_1_6"
                    // chapterId derived from level.getChapter()
                    String chapterId = "chapter_" + level.getChapter();

                    Intent intent = new Intent(this, QuizActivity.class);
                    intent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, level.getLevelId());
                    intent.putExtra("LEVEL_TYPE", "Firestore_Quiz");
                    intent.putExtra("chapterId", chapterId);
                    intent.putExtra("missionId", level.getLevelId());
                    startActivity(intent);
                } else {
                    // Existing local JSON quiz flow — unchanged
                    Intent intent = new Intent(this, QuizActivity.class);
                    intent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, levelId);
                    intent.putExtra("LEVEL_TYPE", level.getLevelType());
                    startActivity(intent);
                }
        }
    }
}