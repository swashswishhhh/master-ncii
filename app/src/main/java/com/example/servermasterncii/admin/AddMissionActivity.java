package com.example.servermasterncii.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityAddMissionBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unified Mission Builder — 3-step wizard.
 *
 * Step 1: Fill mission metadata (chapter, title, ID, difficulty, description)
 * Step 2: Add questions inline — no activity switch needed
 * Step 3: Review summary → Publish or Save Draft
 *
 * The mission is written to Firestore once (at publish/draft time).
 * Questions are batched in memory and written together at the end.
 */
public class AddMissionActivity extends AppCompatActivity {

    private ActivityAddMissionBinding binding;
    private AdminViewModel viewModel;
    private FirebaseFirestore db;

    // ── Wizard state ──────────────────────────────────────────────
    private static final int STEP_MISSION_INFO = 0;
    private static final int STEP_ADD_QUESTIONS = 1;
    private static final int STEP_REVIEW = 2;
    private int currentStep = STEP_MISSION_INFO;

    // ── Chapter / difficulty data ─────────────────────────────────
    private final String[] chapterIds    = {"chapter_1", "chapter_2", "chapter_3"};
    private final String[] chapterTitles = {
            "Chapter 1 — User Access",
            "Chapter 2 — Server Setup",
            "Chapter 3 — Maintenance"
    };
    private final String[] difficulties  = {"beginner", "intermediate", "advanced"};

    // ── Saved mission reference (set after step 1 next) ──────────
    private String savedMissionDocId = null; // Firestore doc id after first write
    private String resolvedChapterId;
    private String resolvedMissionId;
    private String resolvedTitle;
    private String resolvedDifficulty;

    // ── Question batch ────────────────────────────────────────────
    private final List<AdminQuestion> pendingQuestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityAddMissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupToolbar();
        setupSpinners();
        setupMissionIdManualEntry(); // Changed from auto-fill to manual entry
        setupStepButtons();
        observeViewModel();
        goToStep(STEP_MISSION_INFO);
    }

    // ─────────────────────────────────────────────────────────────
    // Setup helpers
    // ─────────────────────────────────────────────────────────────

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (currentStep == STEP_MISSION_INFO) {
                finish();
            } else {
                goToStep(currentStep - 1);
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> chapterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, chapterTitles);
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerChapter.setAdapter(chapterAdapter);

        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, difficulties);
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(diffAdapter);
    }

    /** Manual entry for Mission ID — no auto-fill, just format hint */
    private void setupMissionIdManualEntry() {
        binding.etMissionId.setHint("e.g. MISSION 1.6");
        binding.etMissionId.setText("");  // remove the "MISSION " pre-fill


    }

    private void setupStepButtons() {
        // Step 1 → Step 2
        binding.btnNext.setOnClickListener(v -> {
            if (validateMissionInfo()) advanceToQuestions();
        });

        // Step 2: add a question to the batch
        binding.btnAddQuestion.setOnClickListener(v -> {
            if (validateQuestionForm()) saveQuestionToBatch();
        });

        // Step 2 → Step 3
        binding.btnDoneQuestions.setOnClickListener(v -> {
            if (pendingQuestions.isEmpty()) {
                Toast.makeText(this,
                        "Add at least 1 question before continuing",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            goToStep(STEP_REVIEW);
        });

        // Step 3: Publish
        binding.btnPublishMission.setOnClickListener(v -> saveMission(true));

        // Step 3: Save draft
        binding.btnSaveDraft.setOnClickListener(v -> saveMission(false));
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null) setLoading(loading);
        });
        viewModel.getError().observe(this, err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
        viewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Step navigation
    // ─────────────────────────────────────────────────────────────

    private void goToStep(int step) {
        currentStep = step;

        // Update step indicator pills
        updateStepIndicator(step);

        // Show / hide sections
        binding.sectionMissionInfo.setVisibility(
                step == STEP_MISSION_INFO ? View.VISIBLE : View.GONE);
        binding.sectionAddQuestions.setVisibility(
                step == STEP_ADD_QUESTIONS ? View.VISIBLE : View.GONE);
        binding.sectionReview.setVisibility(
                step == STEP_REVIEW ? View.VISIBLE : View.GONE);

        // Update review section when entering step 3
        if (step == STEP_REVIEW) {
            populateReviewSection();
        }

        // Toolbar title
        String[] titles = {"⬡ NEW MISSION", "⬡ ADD QUESTIONS", "⬡ REVIEW"};
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(titles[step]);
    }

    private void updateStepIndicator(int activeStep) {
        // Pills: step1Pill, step2Pill, step3Pill defined in XML
        int activeColor   = getResources().getColor(
                com.google.android.material.R.color.design_default_color_primary, getTheme());
        int inactiveColor = 0xFF1E1E3A;
        int activeText    = 0xFF0A0A0F;
        int inactiveText  = 0xFF6B6B99;

        View[] pills   = {binding.pill1, binding.pill2, binding.pill3};
        for (int i = 0; i < pills.length; i++) {
            boolean active = (i == activeStep);
            pills[i].setBackgroundResource(active
                    ? com.example.servermasterncii.R.drawable.bg_pill_active
                    : com.example.servermasterncii.R.drawable.bg_pill_inactive);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Step 1 → Step 2
    // ─────────────────────────────────────────────────────────────

    private void advanceToQuestions() {
        // Cache mission data for the context bar in Step 2
        resolvedChapterId = chapterIds[binding.spinnerChapter.getSelectedItemPosition()];
        resolvedTitle = binding.etMissionTitle.getText().toString().trim();
        resolvedDifficulty = difficulties[binding.spinnerDifficulty.getSelectedItemPosition()];

        // Get raw mission ID and normalize spaces
        String rawId = binding.etMissionId.getText().toString().trim();
        // Normalize: collapse multiple spaces, e.g. "MISSION  1.6" → "MISSION 1.6"
        String normalizedId = rawId.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);

        // Validate mission ID format (must be "MISSION X.Y")
        if (!normalizedId.matches("MISSION \\d+\\.\\d+")) {
            Toast.makeText(this,
                    "Mission ID must follow format: MISSION X.Y (e.g., MISSION 1.6)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Convert "MISSION 1.6" → "mission_1_6" for Firestore storage
        resolvedMissionId = normalizedId
                .toLowerCase(Locale.ROOT)   // "mission 1.6"
                .replace(" ", "_")          // "mission_1.6"
                .replace(".", "_");         // "mission_1_6" ✅

        // Show context bar in step 2
        binding.tvContextBar.setText(
                chapterTitles[binding.spinnerChapter.getSelectedItemPosition()]
                        + "  ›  " + resolvedTitle);

        clearQuestionForm();
        goToStep(STEP_ADD_QUESTIONS);
        refreshQuestionCount();
    }

    // ─────────────────────────────────────────────────────────────
    // Step 2 — Question management
    // ─────────────────────────────────────────────────────────────

    private void saveQuestionToBatch() {
        String qText  = binding.etQText.getText().toString().trim();
        String choiceA = binding.etQChoiceA.getText().toString().trim();
        String choiceB = binding.etQChoiceB.getText().toString().trim();
        String choiceC = binding.etQChoiceC.getText().toString().trim();
        String choiceD = binding.etQChoiceD.getText().toString().trim();

        // Determine correct answer from radio group
        String correct = getSelectedCorrectAnswer(choiceA, choiceB, choiceC, choiceD);

        String difficulty = binding.spinnerQDifficulty.getSelectedItem().toString();
        String explanation = binding.etQExplanation.getText().toString().trim();

        AdminQuestion q = new AdminQuestion();
        q.setQuestionText(qText);
        q.setChoices(Arrays.asList(choiceA, choiceB, choiceC, choiceD));
        q.setCorrectAnswer(correct);
        q.setChapterId(resolvedChapterId);
        q.setMissionId(resolvedMissionId);
        q.setDifficulty(difficulty);
        q.setExplanation(explanation);
        q.setPublished(false); // committed at final publish

        pendingQuestions.add(q);

        addQuestionChip(pendingQuestions.size(), qText);
        clearQuestionForm();
        refreshQuestionCount();

        Toast.makeText(this,
                "Q" + pendingQuestions.size() + " added — keep going or tap Done",
                Toast.LENGTH_SHORT).show();
    }

    /** Returns the correct answer text based on which radio button is selected. */
    private String getSelectedCorrectAnswer(String a, String b, String c, String d) {
        int checkedId = binding.rgCorrectAnswer.getCheckedRadioButtonId();
        if (checkedId == binding.rbAnswerA.getId()) return a;
        if (checkedId == binding.rbAnswerB.getId()) return b;
        if (checkedId == binding.rbAnswerC.getId()) return c;
        if (checkedId == binding.rbAnswerD.getId()) return d;
        return a; // fallback
    }

    /** Adds a dismissible chip for each saved question. Tap chip to remove it. */
    private void addQuestionChip(int number, String questionText) {
        Chip chip = new Chip(this);
        chip.setText("Q" + number);
        chip.setCheckable(false);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(
                com.example.servermasterncii.R.color.chip_background); // define in colors.xml
        chip.setTextColor(0xFF00F5FF);
        chip.setCloseIconTint(
                android.content.res.ColorStateList.valueOf(0xFF6B6B99));

        // Long-press to see the question text
        chip.setOnLongClickListener(v -> {
            Toast.makeText(this, questionText, Toast.LENGTH_LONG).show();
            return true;
        });

        // Close = remove from batch
        final int index = pendingQuestions.size() - 1;
        chip.setOnCloseIconClickListener(v -> {
            pendingQuestions.remove(index);
            binding.chipGroupQuestions.removeView(chip);
            refreshQuestionCount();
            // Renumber remaining chips
            renumberChips();
        });

        binding.chipGroupQuestions.addView(chip);
    }

    private void renumberChips() {
        int count = binding.chipGroupQuestions.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.chipGroupQuestions.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setText("Q" + (i + 1));
            }
        }
    }

    private void refreshQuestionCount() {
        int count = pendingQuestions.size();
        binding.tvQuestionCount.setText(count + " question" + (count == 1 ? "" : "s") + " added");
        binding.btnDoneQuestions.setEnabled(count > 0);
    }

    private void clearQuestionForm() {
        binding.etQText.setText("");
        binding.etQChoiceA.setText("");
        binding.etQChoiceB.setText("");
        binding.etQChoiceC.setText("");
        binding.etQChoiceD.setText("");
        binding.etQExplanation.setText("");
        binding.rgCorrectAnswer.check(binding.rbAnswerA.getId());

        // Reset difficulty to easy
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"easy", "medium", "hard"});
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerQDifficulty.setAdapter(diffAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // Step 3 — Review
    // ─────────────────────────────────────────────────────────────

    private void populateReviewSection() {
        binding.tvReviewChapter.setText(
                chapterTitles[binding.spinnerChapter.getSelectedItemPosition()]);
        binding.tvReviewTitle.setText(resolvedTitle);
        binding.tvReviewMissionId.setText(resolvedMissionId);
        binding.tvReviewDifficulty.setText(resolvedDifficulty);
        binding.tvReviewQuestionCount.setText(
                pendingQuestions.size() + " questions ready");
    }

    // ─────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────

    private boolean validateMissionInfo() {
        boolean valid = true;

        String title = binding.etMissionTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.etMissionTitle.setError("Mission title is required");
            valid = false;
        }

        String missionId = binding.etMissionId.getText().toString().trim();
        if (TextUtils.isEmpty(missionId)) {
            binding.etMissionId.setError("Mission ID is required");
            valid = false;
        } else if (!missionId.matches("MISSION \\d+\\.\\d+")) {
            binding.etMissionId.setError("Must follow format: MISSION X.Y (e.g., MISSION 1.6)");
            valid = false;
        }

        return valid;
    }

    private boolean validateQuestionForm() {
        boolean valid = true;

        if (TextUtils.isEmpty(binding.etQText.getText())) {
            binding.etQText.setError("Question text is required");
            valid = false;
        }
        if (TextUtils.isEmpty(binding.etQChoiceA.getText())) {
            binding.etQChoiceA.setError("Required");
            valid = false;
        }
        if (TextUtils.isEmpty(binding.etQChoiceB.getText())) {
            binding.etQChoiceB.setError("Required");
            valid = false;
        }
        if (TextUtils.isEmpty(binding.etQChoiceC.getText())) {
            binding.etQChoiceC.setError("Required");
            valid = false;
        }
        if (TextUtils.isEmpty(binding.etQChoiceD.getText())) {
            binding.etQChoiceD.setError("Required");
            valid = false;
        }

        return valid;
    }

    // ─────────────────────────────────────────────────────────────
    // Firestore writes  (same paths as original)
    // ─────────────────────────────────────────────────────────────

    private void saveMission(boolean published) {
        setLoading(true);
        populateReviewSection();

        String adminEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";

        String description = binding.etDescription.getText().toString().trim();

        // ── Mission document (same structure as original) ─────────
        Map<String, Object> missionData = new HashMap<>();
        missionData.put("title",       resolvedTitle);
        missionData.put("missionId",   resolvedMissionId);
        missionData.put("chapterId",   resolvedChapterId);
        missionData.put("description", description);
        missionData.put("difficulty",  resolvedDifficulty);
        missionData.put("published",   published);
        missionData.put("isFirestore", true);
        missionData.put("isLocal",     false);
        missionData.put("createdBy",   adminEmail);
        missionData.put("createdAt",   Timestamp.now());

        Map<String, Object> subcollectionData = new HashMap<>();
        subcollectionData.put("id",          resolvedMissionId);
        subcollectionData.put("title",       resolvedTitle);
        subcollectionData.put("order",       999);
        subcollectionData.put("isLocal",     false);
        subcollectionData.put("isPublished", published);

        db.collection("missions")
                .add(missionData)
                .addOnSuccessListener(missionRef -> {
                    savedMissionDocId = missionRef.getId();

                    // Write to chapter subcollection
                    db.collection("chapters")
                            .document(resolvedChapterId)
                            .collection("missions")
                            .document(resolvedMissionId)
                            .set(subcollectionData)
                            .addOnSuccessListener(v -> {
                                // Now batch-write all questions
                                writeAllQuestions(published, missionRef.getId());
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "❌ Chapter save failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "❌ Mission save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Writes all pending questions as a Firestore batch under the mission. */
    private void writeAllQuestions(boolean published, String missionDocId) {
        if (pendingQuestions.isEmpty()) {
            onSaveComplete(published);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (AdminQuestion q : pendingQuestions) {
            q.setPublished(published);
            q.setMissionId(resolvedMissionId);   // already normalized "mission_1_7"
            q.setChapterId(resolvedChapterId);

            // Build the question map manually for the batch
            Map<String, Object> qData = new HashMap<>();
            qData.put("questionText",  q.getQuestionText());
            qData.put("choices",       q.getChoices());
            qData.put("correctAnswer", q.getCorrectAnswer());
            qData.put("chapterId",     q.getChapterId());
            qData.put("missionId",     q.getMissionId());
            qData.put("difficulty",    q.getDifficulty());
            qData.put("explanation",   q.getExplanation());
            qData.put("published",     q.isPublished());
            qData.put("createdAt",     Timestamp.now());
            qData.put("createdBy",     FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "");

            // Auto-generate a new doc ref in the "questions" collection
            DocumentReference docRef = db.collection("questions").document();
            batch.set(docRef, qData);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d("AddMission", "Batch wrote " + pendingQuestions.size() + " questions ✅");
                    onSaveComplete(published);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e("AddMission", "Batch write FAILED: " + e.getMessage());
                    Toast.makeText(this,
                            "❌ Questions save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onSaveComplete(boolean published) {
        setLoading(false);
        String msg = published
                ? "✅ Mission published with " + pendingQuestions.size() + " questions!"
                : "📝 Draft saved with " + pendingQuestions.size() + " questions!";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }

    // ─────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPublishMission.setEnabled(!loading);
        binding.btnSaveDraft.setEnabled(!loading);
        binding.btnNext.setEnabled(!loading);
        binding.btnAddQuestion.setEnabled(!loading);
        binding.btnDoneQuestions.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}