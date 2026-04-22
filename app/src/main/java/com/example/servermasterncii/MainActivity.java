package com.example.servermasterncii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.servermasterncii.databinding.ActivityMainBinding;
import com.example.servermasterncii.model.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements LevelAdapter.OnLevelClickListener {

    // ─── Constants ───────────────────────────────────────────────
    private static final String PREFS_NAME       = "server_master_prefs";
    private static final double UNLOCK_THRESHOLD = 0.70;

    private static final String KEY_SCORE    = "score_";
    private static final String KEY_TOTAL    = "total_";
    private static final String KEY_UNLOCKED = "unlocked_";

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

    // All standard level IDs — used by ProgressManager.pushAllToFirestore
    private static final String[] ALL_LEVEL_IDS = {
            "1.1","1.2","1.3","1.4","1.5",
            "2.1","2.2","2.3","2.4","2.5","2.6","2.7",
            "2.8","2.9","2.10","2.11","2.12",
            "3.1","3.2","3.3","3.4"
    };

    // ─── UI ──────────────────────────────────────────────────────
    private ActivityMainBinding binding;

    // ─── Data ────────────────────────────────────────────────────
    private LevelAdapter adapter;
    private List<Level> allLevels;
    private List<Level> displayedLevels = new ArrayList<>();
    private SharedPreferences prefs;
    private int currentChapter = 1;
    private boolean isGuest    = false;

    // Tracks which simulator level is pending a result
    private String pendingSimulatorLevelId;

    // ─── Simulator launcher ───────────────────────────────────────
    private final ActivityResultLauncher<Intent> simulatorLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getBooleanExtra("extra_task_completed", false)
                                && pendingSimulatorLevelId != null) {

                            // Use ProgressManager — handles guest check + Firestore sync
                            String nextLevel = getNextLevelIdForSimulator(pendingSimulatorLevelId);
                            ProgressManager.saveSimulatorProgress(
                                    prefs, pendingSimulatorLevelId, nextLevel);

                            pendingSimulatorLevelId = null;
                            refreshLevels();
                            updateHallOfFame();
                        }
                    });

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs   = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Save guest state so other screens can check it
        boolean isGuest = getIntent().getBooleanExtra(LoginActivity.EXTRA_IS_GUEST, false);
        prefs.edit().putBoolean("is_guest", isGuest).apply();
//        isGuest = getIntent().getBooleanExtra(LoginActivity.EXTRA_IS_GUEST, false);

        // Pull progress from Firestore first (no-op for guests),
        // then build the level list once sync is done
        ProgressManager.pullFromFirestore(prefs, new ProgressManager.OnSyncCompleteListener() {
            @Override
            public void onComplete() {
                runOnUiThread(() -> initUI());
            }

            @Override
            public void onError(String message) {
                // Fall back to local SharedPreferences data
                runOnUiThread(() -> initUI());
            }
        });
    }

    /** Initialises all UI after Firestore progress has been pulled. */
    private void initUI() {
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
        loadFirestoreMissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // allLevels may be null if Firestore pull hasn't finished yet
        if (allLevels != null) {
            refreshLevels();
            updateHallOfFame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // =====================================================================
    // Bottom Navigation
    // =====================================================================

    private void setupSettingsButton() {
        boolean isGuest = prefs.getBoolean("is_guest", false);

        if (isGuest) {
            // Hide settings gear icon entirely for guest users
            binding.btnSettings.setVisibility(View.GONE);
        } else {
            binding.btnSettings.setVisibility(View.VISIBLE);
            binding.btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chapter_1) { switchToChapter(1); return true; }
            if (id == R.id.nav_chapter_2) { switchToChapter(2); return true; }
            if (id == R.id.nav_chapter_3) { switchToChapter(3); return true; }
            return false;
        });
    }

    private void switchToChapter(int chapter) {
        currentChapter = chapter;
        displayedLevels.clear();
        displayedLevels.addAll(filterByChapter(chapter));
        adapter.notifyDataSetChanged();
        updateChapterUI(chapter);
        binding.recyclerLevels.scrollToPosition(0);
    }

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

    private List<Level> buildAllLevels() {
        List<Level> list = new ArrayList<>();

        // CHAPTER 1
        list.add(level(1,  "P2P Networks",     "Peer-to-Peer Basics",     "1.1",  "anim_network.json",         1, "Standard_Quiz",   true));
        list.add(level(2,  "Workgroups",        "Windows Workgroup Setup", "1.2",  "anim_workgroup.json",       1, "Standard_Quiz",   isLevelUnlocked("1.2")));
        list.add(level(3,  "Firewalls",         "Windows Firewall Config", "1.3",  "anim_shield.json",          1, "Interactive_UI",  isLevelUnlocked("1.3")));
        list.add(level(4,  "IP Addressing",     "IPv4 & Subnetting",       "1.4",  "anim_ip_address.json",      1, "Interactive_UI",  isLevelUnlocked("1.4")));
        list.add(level(5,  "CMD Basics",        "Terminal Commands",       "1.5",  "anim_terminal.json",        1, "Interactive_CMD", isLevelUnlocked("1.5")));

        // CHAPTER 2
        list.add(level(6,  "Server Roles",      "Intro to Server Roles",   "2.1",  "anim_server_roles.json",    2, "Standard_Quiz",   isLevelUnlocked("2.1")));
        list.add(level(7,  "Windows Server",    "OS Installation",         "2.2",  "anim_windows_server.json",  2, "Standard_Quiz",   isLevelUnlocked("2.2")));
        list.add(level(8,  "Active Directory",  "Domain Services",         "2.3",  "anim_active_directory.json",2, "Standard_Quiz",   isLevelUnlocked("2.3")));
        list.add(level(9,  "DNS Server",        "Name Resolution",         "2.4",  "anim_dns.json",             2, "Standard_Quiz",   isLevelUnlocked("2.4")));
        list.add(level(10, "DHCP Server",       "Dynamic IP Allocation",   "2.5",  "anim_dhcp.json",            2, "Standard_Quiz",   isLevelUnlocked("2.5")));
        list.add(level(11, "File Server",       "Shared Storage",          "2.6",  "anim_file_server.json",     2, "Standard_Quiz",   isLevelUnlocked("2.6")));
        list.add(level(12, "Print Server",      "Network Printing",        "2.7",  "anim_print_server.json",    2, "Standard_Quiz",   isLevelUnlocked("2.7")));
        list.add(level(13, "Group Policy",      "GPO Management",          "2.8",  "anim_group_policy.json",    2, "Standard_Quiz",   isLevelUnlocked("2.8")));
        list.add(level(14, "Backup Server",     "Data Backup",             "2.9",  "anim_backup.json",          2, "Standard_Quiz",   isLevelUnlocked("2.9")));
        list.add(level(15, "Remote Desktop",    "RDP Configuration",       "2.10", "anim_remote_desktop.json",  2, "Interactive_UI",  isLevelUnlocked("2.10")));
        list.add(level(16, "Server Security",   "Hardening Servers",       "2.11", "anim_server_security.json", 2, "Standard_Quiz",   isLevelUnlocked("2.11")));
        list.add(level(17, "Monitoring",        "Performance Monitor",     "2.12", "anim_monitoring.json",      2, "Standard_Quiz",   isLevelUnlocked("2.12")));

        // CHAPTER 3
        list.add(level(18, "Troubleshooting",   "Network Diagnostics",     "3.1",  "anim_troubleshoot.json",    3, "Interactive_CMD", isLevelUnlocked("3.1")));
        list.add(level(19, "Updates & Patches", "System Maintenance",      "3.2",  "anim_updates.json",         3, "Standard_Quiz",   isLevelUnlocked("3.2")));
        list.add(level(20, "Documentation",     "IT Documentation",        "3.3",  "anim_documentation.json",   3, "Standard_Quiz",   isLevelUnlocked("3.3")));
        list.add(level(21, "Decommission",      "Server Retirement",       "3.4",  "anim_decommission.json",    3, "Standard_Quiz",   isLevelUnlocked("3.4")));

        return list;
    }

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

                        if (missionId == null || title == null || chapterId == null) continue;

                        int chapterNum = 1;
                        if ("chapter_2".equals(chapterId)) chapterNum = 2;
                        else if ("chapter_3".equals(chapterId)) chapterNum = 3;

                        boolean alreadyExists = false;
                        for (Level l : allLevels) {
                            if (missionId.equals(l.getLevelId())) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            String lottie = "anim_network.json";
                            if (chapterNum == 2) lottie = "anim_server_roles.json";
                            else if (chapterNum == 3) lottie = "anim_terminal.json";

                            Level firestoreLevel = new Level(
                                    allLevels.size() + 1,
                                    title,
                                    description != null ? description : "",
                                    missionId,
                                    lottie,
                                    chapterNum,
                                    "Firestore_Quiz",
                                    getProgressForLevel(missionId),
                                    true
                            );
                            allLevels.add(firestoreLevel);
                            newMissionsAdded = true;
                        }
                    }

                    if (newMissionsAdded) {
                        displayedLevels.clear();
                        displayedLevels.addAll(filterByChapter(currentChapter));
                        adapter.notifyDataSetChanged();
                        updateHallOfFame();
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("MainActivity",
                                "Failed to load Firestore missions", e));
    }

    private Level level(int num, String title, String subtitle, String levelId,
                        String lottie, int chapter, String levelType, boolean unlocked) {
        return new Level(num, title, subtitle, levelId, lottie,
                chapter, levelType, getProgressForLevel(levelId), unlocked);
    }

    private List<Level> filterByChapter(int chapter) {
        return allLevels.stream()
                .filter(l -> l.getChapter() == chapter)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // SharedPreferences helpers
    // =====================================================================

    private int getProgressForLevel(String levelId) {
        int score = prefs.getInt(KEY_SCORE + levelId, 0);
        int total = prefs.getInt(KEY_TOTAL + levelId, 0);
        if (total == 0) return 0;
        return Math.min(100, (int) ((score / (float) total) * 100));
    }

    private boolean isLevelUnlocked(String levelId) {
        if ("1.1".equals(levelId)) return true;
        if (levelId.startsWith("mission_")) return true;
        if (prefs.getBoolean(KEY_UNLOCKED + levelId, false)) return true;

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

    private String getPreviousLevelId(String levelId) {
        switch (levelId) {
            case "1.2":  return "1.1";
            case "1.3":  return "1.2";
            case "1.4":  return "1.3";
            case "1.5":  return "1.4";
            case "2.1":  return "1.5";
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
            case "3.1":  return "2.12";
            case "3.2":  return "3.1";
            case "3.3":  return "3.2";
            case "3.4":  return "3.3";
            default:     return null;
        }
    }

    // Maps simulator level → next level to unlock
    private String getNextLevelIdForSimulator(String levelId) {
        switch (levelId) {
            case "1.3": return "1.4";
            case "1.4": return "1.5";
            case "1.5": return "2.1";
            default:    return null;
        }
    }

    // =====================================================================
    // Hall of Fame
    // =====================================================================

    private void updateHallOfFame() {
        int totalSkillPoints = 0;
        int totalProgress    = 0;
        int completedCount   = 0;

        for (Level level : allLevels) {
            totalSkillPoints += prefs.getInt(KEY_SCORE + level.getLevelId(), 0);
            totalProgress    += level.getProgressPercent();
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

    private void refreshLevels() {
        if (allLevels == null) return;
        for (Level level : allLevels) {
            level.setProgressPercent(getProgressForLevel(level.getLevelId()));
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

    @Override
    public void onLevelClick(Level level) {
        if (!level.isUnlocked()) {
            Toast.makeText(this, "⚡ Complete the previous mission to unlock!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String levelId = level.getLevelId();

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
                if ("Firestore_Quiz".equals(level.getLevelType())) {
                    String chapterId = "chapter_" + level.getChapter();
                    Intent intent = new Intent(this, QuizActivity.class);
                    intent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, level.getLevelId());
                    intent.putExtra("LEVEL_TYPE", "Firestore_Quiz");
                    intent.putExtra("chapterId", chapterId);
                    intent.putExtra("missionId", level.getLevelId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, QuizActivity.class);
                    intent.putExtra(QuizActivity.EXTRA_LEARNING_OUTCOME, levelId);
                    intent.putExtra("LEVEL_TYPE", level.getLevelType());
                    startActivity(intent);
                }
        }
    }
}