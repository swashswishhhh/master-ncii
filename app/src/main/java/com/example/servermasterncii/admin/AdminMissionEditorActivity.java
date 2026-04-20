package com.example.servermasterncii.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.servermasterncii.R;
import com.example.servermasterncii.admin.model.AdminMission;
import com.example.servermasterncii.admin.model.AdminQuestion;
import com.example.servermasterncii.databinding.ActivityAdminMissionEditorBinding;
import com.example.servermasterncii.databinding.ItemAdminQuestionBinding;
import com.example.servermasterncii.db.AppDatabase;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminMissionEditorActivity — two-step mission creation and editing form.
 *
 * <h3>Step 1 — Mission Metadata</h3>
 * <ul>
 *   <li>Title text field</li>
 *   <li>Difficulty radio group (Beginner / Intermediate / Advanced)</li>
 *   <li>Game Mechanic Type radio group (Organizer / Monitor / Matrix)</li>
 *   <li>"Next: Add Questions" button — saves metadata to Room and advances to Step 2</li>
 * </ul>
 *
 * <h3>Step 2 — Question Builder</h3>
 * <ul>
 *   <li>Dynamic list of question cards inflated from {@code item_admin_question.xml}</li>
 *   <li>Each card: question text, 4 option fields, radio buttons to select correct answer</li>
 *   <li>Real-time validation dot per card (red = invalid, green = valid)</li>
 *   <li>Validation bar at top shows overall status</li>
 *   <li>PUBLISH button in toolbar — enabled only when all questions are valid</li>
 * </ul>
 *
 * <h3>Edit mode</h3>
 * When launched with {@link AdminDashboardActivity#EXTRA_MISSION_ID}, loads the
 * existing mission and its questions from Room and pre-populates all fields.
 */
public class AdminMissionEditorActivity extends AppCompatActivity {

    // ── State ──────────────────────────────────────────────────────────────
    private ActivityAdminMissionEditorBinding binding;
    private AdminViewModel viewModel;

    /** Room ID of the mission being edited. -1 = new mission not yet saved. */
    private int missionId = -1;

    /** In-memory list of question bindings — parallel to the Room question list. */
    private final List<QuestionCard> questionCards = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAdminMissionEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Check if editing an existing mission
        missionId = getIntent().getIntExtra(AdminDashboardActivity.EXTRA_MISSION_ID, -1);

        setupToolbar();
        setupStep1();
        observePublishResult();

        if (missionId != -1) {
            loadExistingMission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Toolbar
    // ══════════════════════════════════════════════════════════════════════

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnPublish.setOnClickListener(v -> {
            if (missionId != -1) {
                saveAllQuestions(() -> viewModel.publishMission(missionId));
            }
        });

        // Publish button starts disabled — enabled by validation observer
        binding.btnPublish.setEnabled(false);
        binding.btnPublish.setBackgroundTintList(
                ColorStateList.valueOf(0xFF30363D));

        viewModel.getCanPublish().observe(this, canPublish -> {
            binding.btnPublish.setEnabled(canPublish);
            binding.btnPublish.setBackgroundTintList(
                    ColorStateList.valueOf(canPublish ? 0xFF3FB950 : 0xFF30363D));
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Step 1 — Metadata
    // ══════════════════════════════════════════════════════════════════════

    private void setupStep1() {
        binding.btnNextToQuestions.setOnClickListener(v -> {
            String title = binding.etTitle.getText() != null
                    ? binding.etTitle.getText().toString().trim() : "";

            if (title.isEmpty()) {
                binding.tilTitle.setError("Mission title is required");
                return;
            }
            binding.tilTitle.setError(null);

            String difficulty = getSelectedDifficulty();
            String mechanic   = getSelectedMechanic();

            if (missionId == -1) {
                // Create new mission
                viewModel.createMission(title, difficulty, mechanic, newId -> {
                    missionId = newId;
                    runOnUiThread(() -> {
                        binding.tvMissionName.setText(title);
                        advanceToStep2();
                    });
                });
            } else {
                // Update existing mission metadata
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AdminMission m = AppDatabase.getDatabase(this).adminMissionDao().getById(missionId);
                    if (m != null) {
                        m.title        = title;
                        m.difficulty   = difficulty;
                        m.mechanicType = mechanic;
                        viewModel.updateMission(m);
                    }
                    runOnUiThread(() -> {
                        binding.tvMissionName.setText(title);
                        advanceToStep2();
                    });
                });
            }
        });
    }

    private String getSelectedDifficulty() {
        int id = binding.rgDifficulty.getCheckedRadioButtonId();
        if (id == R.id.rbIntermediate) return "Intermediate";
        if (id == R.id.rbAdvanced)     return "Advanced";
        return "Beginner";
    }

    private String getSelectedMechanic() {
        int id = binding.rgMechanic.getCheckedRadioButtonId();
        if (id == R.id.rbMonitor) return AdminMission.MECHANIC_MONITOR;
        if (id == R.id.rbMatrix)  return AdminMission.MECHANIC_MATRIX;
        return AdminMission.MECHANIC_ORGANIZER;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Step 2 — Question Builder
    // ══════════════════════════════════════════════════════════════════════

    private void advanceToStep2() {
        // Update tab indicators
        binding.tvStep1Tab.setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_admin_tab_idle));
        binding.tvStep1Tab.setTextColor(0xFF8B949E);
        binding.tvStep2Tab.setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_admin_tab_active));
        binding.tvStep2Tab.setTextColor(0xFF58A6FF);

        binding.stepMetadata.setVisibility(View.GONE);
        binding.stepQuestions.setVisibility(View.VISIBLE);

        // Observe questions from Room
        viewModel.getQuestionsForMission(missionId).observe(this, questions -> {
            if (questions != null && !questions.isEmpty()) {
                // Sync cards with Room data
                syncQuestionCards(questions);
            }
        });

        binding.btnAddQuestion.setOnClickListener(v ->
                viewModel.addQuestion(missionId, newId ->
                        runOnUiThread(() -> addQuestionCard(newId, null))));

        // Trigger initial validation
        viewModel.revalidateMission(missionId);
    }

    /**
     * Syncs the in-memory card list with the Room question list.
     * Only adds cards for questions that don't already have a card.
     */
    private void syncQuestionCards(List<AdminQuestion> questions) {
        for (AdminQuestion q : questions) {
            boolean exists = false;
            for (QuestionCard card : questionCards) {
                if (card.questionId == q.id) { exists = true; break; }
            }
            if (!exists) {
                addQuestionCard(q.id, q);
            }
        }
    }

    /**
     * Inflates a question card, populates it with data if provided,
     * and appends it to the question container.
     */
    private void addQuestionCard(int questionId, AdminQuestion existing) {
        ItemAdminQuestionBinding cardBinding = ItemAdminQuestionBinding.inflate(
                LayoutInflater.from(this), binding.questionContainer, false);

        int cardIndex = questionCards.size();
        cardBinding.tvQuestionNumber.setText(String.valueOf(cardIndex + 1));

        // Pre-populate if editing
        if (existing != null) {
            if (existing.questionText != null) cardBinding.etQuestionText.setText(existing.questionText);
            if (existing.optionA != null)      cardBinding.etOptionA.setText(existing.optionA);
            if (existing.optionB != null)      cardBinding.etOptionB.setText(existing.optionB);
            if (existing.optionC != null)      cardBinding.etOptionC.setText(existing.optionC);
            if (existing.optionD != null)      cardBinding.etOptionD.setText(existing.optionD);

            // Set correct answer radio
            switch (existing.correctOption) {
                case 1: cardBinding.radioA.setChecked(true); break;
                case 2: cardBinding.radioB.setChecked(true); break;
                case 3: cardBinding.radioC.setChecked(true); break;
                case 4: cardBinding.radioD.setChecked(true); break;
            }
        }

        QuestionCard card = new QuestionCard(questionId, cardBinding);
        questionCards.add(card);

        // Wire radio buttons as a logical group
        setupRadioGroup(card);

        // Wire text watchers for real-time validation
        setupTextWatchers(card);

        // Delete button
        cardBinding.btnDeleteQuestion.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AdminQuestion q = AppDatabase.getDatabase(this)
                        .adminQuestionDao().getById(questionId);
                if (q != null) viewModel.deleteQuestion(q, missionId);
            });
            binding.questionContainer.removeView(cardBinding.getRoot());
            questionCards.remove(card);
            renumberCards();
            revalidateAll();
        });

        binding.questionContainer.addView(cardBinding.getRoot());
        updateValidationDot(card);
        revalidateAll();
    }

    /**
     * Wires the four radio buttons so only one can be selected at a time,
     * and saves the selection to Room on change.
     */
    private void setupRadioGroup(QuestionCard card) {
        ItemAdminQuestionBinding b = card.binding;

        View.OnClickListener radioListener = v -> {
            // Uncheck all others
            b.radioA.setChecked(v == b.radioA);
            b.radioB.setChecked(v == b.radioB);
            b.radioC.setChecked(v == b.radioC);
            b.radioD.setChecked(v == b.radioD);

            int correct = 0;
            if (b.radioA.isChecked()) correct = 1;
            else if (b.radioB.isChecked()) correct = 2;
            else if (b.radioC.isChecked()) correct = 3;
            else if (b.radioD.isChecked()) correct = 4;

            final int finalCorrect = correct;
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AdminQuestion q = AppDatabase.getDatabase(this)
                        .adminQuestionDao().getById(card.questionId);
                if (q != null) {
                    q.correctOption = finalCorrect;
                    viewModel.saveQuestion(q, missionId);
                }
            });

            updateValidationDot(card);
            revalidateAll();
        };

        b.radioA.setOnClickListener(radioListener);
        b.radioB.setOnClickListener(radioListener);
        b.radioC.setOnClickListener(radioListener);
        b.radioD.setOnClickListener(radioListener);
    }

    /**
     * Attaches TextWatchers to all five text fields in a question card.
     * On each change, saves the field to Room and re-validates.
     */
    private void setupTextWatchers(QuestionCard card) {
        ItemAdminQuestionBinding b = card.binding;

        attachWatcher(b.etQuestionText, card, "question");
        attachWatcher(b.etOptionA,      card, "optionA");
        attachWatcher(b.etOptionB,      card, "optionB");
        attachWatcher(b.etOptionC,      card, "optionC");
        attachWatcher(b.etOptionD,      card, "optionD");
    }

    private void attachWatcher(com.google.android.material.textfield.TextInputEditText field,
                                QuestionCard card, String fieldName) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AdminQuestion q = AppDatabase.getDatabase(
                            AdminMissionEditorActivity.this)
                            .adminQuestionDao().getById(card.questionId);
                    if (q == null) return;
                    switch (fieldName) {
                        case "question": q.questionText = text; break;
                        case "optionA":  q.optionA = text; break;
                        case "optionB":  q.optionB = text; break;
                        case "optionC":  q.optionC = text; break;
                        case "optionD":  q.optionD = text; break;
                    }
                    viewModel.saveQuestion(q, missionId);
                });
                updateValidationDot(card);
                revalidateAll();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation UI
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Updates the small colored dot on a question card based on whether
     * all its fields are currently filled in.
     */
    private void updateValidationDot(QuestionCard card) {
        ItemAdminQuestionBinding b = card.binding;
        boolean valid = isCardValid(b);
        b.validationDot.setBackground(ContextCompat.getDrawable(this,
                valid ? R.drawable.bg_validation_dot_valid
                      : R.drawable.bg_validation_dot_invalid));
    }

    private boolean isCardValid(ItemAdminQuestionBinding b) {
        String q  = b.etQuestionText.getText() != null ? b.etQuestionText.getText().toString().trim() : "";
        String a  = b.etOptionA.getText() != null ? b.etOptionA.getText().toString().trim() : "";
        String bv = b.etOptionB.getText() != null ? b.etOptionB.getText().toString().trim() : "";
        String c  = b.etOptionC.getText() != null ? b.etOptionC.getText().toString().trim() : "";
        String d  = b.etOptionD.getText() != null ? b.etOptionD.getText().toString().trim() : "";
        boolean hasAnswer = b.radioA.isChecked() || b.radioB.isChecked()
                || b.radioC.isChecked() || b.radioD.isChecked();
        return !q.isEmpty() && !a.isEmpty() && !bv.isEmpty()
                && !c.isEmpty() && !d.isEmpty() && hasAnswer;
    }

    /**
     * Checks all cards and updates the validation bar + publish button state.
     */
    private void revalidateAll() {
        if (questionCards.isEmpty()) {
            setValidationBar(false, "Add at least one question to enable Publish");
            return;
        }
        boolean allValid = true;
        int invalidCount = 0;
        for (QuestionCard card : questionCards) {
            if (!isCardValid(card.binding)) {
                allValid = false;
                invalidCount++;
            }
        }
        if (allValid) {
            setValidationBar(true, "All " + questionCards.size() + " question(s) valid — ready to publish");
        } else {
            setValidationBar(false, invalidCount + " question(s) incomplete");
        }
        // Also trigger ViewModel revalidation for the publish button
        if (missionId != -1) viewModel.revalidateMission(missionId);
    }

    private void setValidationBar(boolean valid, String message) {
        binding.validationIndicator.setBackground(ContextCompat.getDrawable(this,
                valid ? R.drawable.bg_validation_dot_valid
                      : R.drawable.bg_validation_dot_invalid));
        binding.tvValidationStatus.setText(message);
        binding.tvValidationStatus.setTextColor(valid ? 0xFF3FB950 : 0xFF8B949E);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    /** Re-numbers the question badges after a deletion. */
    private void renumberCards() {
        for (int i = 0; i < questionCards.size(); i++) {
            questionCards.get(i).binding.tvQuestionNumber.setText(String.valueOf(i + 1));
        }
    }

    /**
     * Saves all in-memory question card states to Room before triggering publish.
     * Runs on the DB executor; calls {@code onComplete} on the main thread when done.
     */
    private void saveAllQuestions(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (QuestionCard card : questionCards) {
                ItemAdminQuestionBinding b = card.binding;
                AdminQuestion q = AppDatabase.getDatabase(this)
                        .adminQuestionDao().getById(card.questionId);
                if (q == null) continue;

                q.questionText = b.etQuestionText.getText() != null
                        ? b.etQuestionText.getText().toString().trim() : "";
                q.optionA = b.etOptionA.getText() != null
                        ? b.etOptionA.getText().toString().trim() : "";
                q.optionB = b.etOptionB.getText() != null
                        ? b.etOptionB.getText().toString().trim() : "";
                q.optionC = b.etOptionC.getText() != null
                        ? b.etOptionC.getText().toString().trim() : "";
                q.optionD = b.etOptionD.getText() != null
                        ? b.etOptionD.getText().toString().trim() : "";

                if (b.radioA.isChecked())      q.correctOption = 1;
                else if (b.radioB.isChecked()) q.correctOption = 2;
                else if (b.radioC.isChecked()) q.correctOption = 3;
                else if (b.radioD.isChecked()) q.correctOption = 4;

                AppDatabase.getDatabase(this).adminQuestionDao().update(q);
            }
            runOnUiThread(onComplete);
        });
    }

    /** Loads an existing mission into the form fields. */
    private void loadExistingMission() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AdminMission m = AppDatabase.getDatabase(this)
                    .adminMissionDao().getById(missionId);
            if (m == null) return;

            runOnUiThread(() -> {
                binding.tvMissionName.setText(m.title != null ? m.title : "");
                if (m.title != null) binding.etTitle.setText(m.title);

                // Restore difficulty
                if ("Intermediate".equals(m.difficulty)) {
                    binding.rgDifficulty.check(R.id.rbIntermediate);
                } else if ("Advanced".equals(m.difficulty)) {
                    binding.rgDifficulty.check(R.id.rbAdvanced);
                } else {
                    binding.rgDifficulty.check(R.id.rbBeginner);
                }

                // Restore mechanic
                if (AdminMission.MECHANIC_MONITOR.equals(m.mechanicType)) {
                    binding.rgMechanic.check(R.id.rbMonitor);
                } else if (AdminMission.MECHANIC_MATRIX.equals(m.mechanicType)) {
                    binding.rgMechanic.check(R.id.rbMatrix);
                } else {
                    binding.rgMechanic.check(R.id.rbOrganizer);
                }

                // Jump straight to Step 2 if mission already has questions
                advanceToStep2();
            });
        });
    }

    /** Observes publish result and shows Snackbar feedback. */
    private void observePublishResult() {
        viewModel.getPublishResult().observe(this, result -> {
            if (result.state == AdminViewModel.PublishState.LOADING) {
                binding.loadingOverlay.setVisibility(View.VISIBLE);
            } else {
                binding.loadingOverlay.setVisibility(View.GONE);
            }

            if (result.state == AdminViewModel.PublishState.SUCCESS) {
                showSnackbar(result.message, false);
                viewModel.resetPublishState();
            } else if (result.state == AdminViewModel.PublishState.ERROR) {
                showSnackbar(result.message, true);
                viewModel.resetPublishState();
            }
        });
    }

    private void showSnackbar(String message, boolean isError) {
        if (binding == null) return;
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(isError ? 0xFFF85149 : 0xFF3FB950)
                .setTextColor(0xFFFFFFFF)
                .show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner class — question card holder
    // ══════════════════════════════════════════════════════════════════════

    /** Pairs a Room question ID with its inflated ViewBinding. */
    private static class QuestionCard {
        final int questionId;
        final ItemAdminQuestionBinding binding;

        QuestionCard(int questionId, ItemAdminQuestionBinding binding) {
            this.questionId = questionId;
            this.binding    = binding;
        }
    }
}
