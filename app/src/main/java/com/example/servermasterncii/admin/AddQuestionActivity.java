package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.servermasterncii.R;
import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityAddQuestionBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AddQuestionActivity — Admin interface for creating new questions.
 * 
 * Features:
 * - Loads chapters and missions from chapters.json
 * - Cascading spinners (Chapter → Mission)
 * - 4 choice inputs with auto-populated correct answer spinner
 * - Difficulty selection
 * - Optional explanation field
 * - PUBLISH (published: true) or SAVE DRAFT (published: false)
 * - Full cyberpunk styling
 */
public class AddQuestionActivity extends AppCompatActivity {

    // Intent extras for editing existing questions
    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    public static final String EXTRA_QUESTION_TEXT = "extra_question_text";
    public static final String EXTRA_CHOICE_1 = "extra_choice_1";
    public static final String EXTRA_CHOICE_2 = "extra_choice_2";
    public static final String EXTRA_CHOICE_3 = "extra_choice_3";
    public static final String EXTRA_CHOICE_4 = "extra_choice_4";
    public static final String EXTRA_CORRECT_ANSWER = "extra_correct_answer";
    public static final String EXTRA_CATEGORY = "extra_category";

    private ActivityAddQuestionBinding binding;
    private AdminViewModel viewModel;
    private String editingQuestionId; // null if creating new, non-null if editing
    
    private List<Chapter> chapters = new ArrayList<>();
    private Chapter selectedChapter;
    private Mission selectedMission;
    
    private String[] difficultyLevels = {"easy", "medium", "hard"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        
        binding = ActivityAddQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        
        // Check if editing existing question
        editingQuestionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        
        setupToolbar();
        loadChapters();
        setupSpinners();
        setupButtons();
        observeViewModel();
        
        // Populate fields if editing
        if (editingQuestionId != null) {
            populateFieldsForEdit();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = editingQuestionId != null ? "Edit Question" : "Add Question";
            getSupportActionBar().setTitle(title);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Loads chapters and missions from assets/chapters.json
     */
    /**
     * Loads chapters and their missions from Firestore.
     * Replaces the old JSON-based loadChapters().
     */
    private void loadChapters() {
        setLoading(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("chapters")
                .orderBy("order")
                .get()
                .addOnSuccessListener(chapterSnapshot -> {

                    chapters.clear();
                    final int[] pendingChapters = {chapterSnapshot.size()};

                    if (pendingChapters[0] == 0) {
                        setLoading(false);
                        showError("No chapters found.");
                        return;
                    }

                    for (QueryDocumentSnapshot chapterDoc : chapterSnapshot) {
                        Chapter chapter   = new Chapter();
                        chapter.id        = chapterDoc.getId();
                        chapter.title     = chapterDoc.getString("title");

                        // Fetch missions subcollection for each chapter
                        db.collection("chapters")
                                .document(chapterDoc.getId())
                                .collection("missions")
                                .orderBy("order")
                                .get()
                                .addOnSuccessListener(missionSnapshot -> {
                                    for (QueryDocumentSnapshot missionDoc : missionSnapshot) {
                                        String mId    = missionDoc.getString("id");
                                        String mTitle = missionDoc.getString("title");
                                        Boolean isLocal = missionDoc.getBoolean("isLocal");

                                        if (mId != null && mTitle != null) {
                                            // Add ⬡ prefix for Firestore-only missions
                                            String displayTitle = Boolean.TRUE.equals(isLocal)
                                                    ? mTitle
                                                    : "⬡ " + mTitle;
                                            chapter.missions.add(
                                                    new Mission(mId, displayTitle,
                                                            !Boolean.TRUE.equals(isLocal)));
                                        }
                                    }

                                    chapters.add(chapter);
                                    pendingChapters[0]--;

                                    // All chapters loaded
                                    if (pendingChapters[0] == 0) {
                                        // Sort chapters by their id
                                        chapters.sort((a, b) -> a.id.compareTo(b.id));
                                        runOnUiThread(() -> {
                                            setLoading(false);
                                            setupSpinners();  // now populate spinners
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    pendingChapters[0]--;
                                    Log.e("AddQuestion", "Failed missions for "
                                            + chapterDoc.getId(), e);
                                    if (pendingChapters[0] == 0) {
                                        runOnUiThread(() -> setLoading(false));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Failed to load chapters: " + e.getMessage());
                });
    }

    /**
     * Fetches admin-created missions from Firestore and appends
     * them to the matching chapter's mission list.
     * Called after loadChapters() finishes.
     */
    private void loadFirestoreMissions() {
        FirebaseFirestore.getInstance()
                .collection("missions")
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String missionId   = doc.getString("missionId");
                        String title       = doc.getString("title");
                        String chapterId   = doc.getString("chapterId");

                        if (missionId == null || title == null || chapterId == null) continue;

                        // Find the matching chapter and append
                        for (Chapter chapter : chapters) {
                            if (chapter.id.equals(chapterId)) {

                                // Avoid duplicates
                                boolean exists = false;
                                for (Mission m : chapter.missions) {
                                    if (m.id.equals(missionId)) { exists = true; break; }
                                }

                                if (!exists) {
                                    // ⬡ prefix signals admin-created mission
                                    chapter.missions.add(new Mission(
                                            missionId,
                                            "⬡ " + title,
                                            true  // isFirestore = true
                                    ));
                                }
                                break;
                            }
                        }
                    }

                    // Refresh spinner with updated mission list
                    runOnUiThread(() -> {
                        if (selectedChapter != null) {
                            updateMissionSpinner();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("AddQuestion", "Failed to load Firestore missions: " + e.getMessage()));
    }

    /**
     * Sets up all spinners with adapters and listeners
     */
    private void setupSpinners() {
        // Chapter Spinner
        List<String> chapterTitles = new ArrayList<>();
        for (Chapter chapter : chapters) {
            chapterTitles.add(chapter.title);
        }
        
        ArrayAdapter<String> chapterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, chapterTitles);
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerChapter.setAdapter(chapterAdapter);
        
        binding.spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedChapter = chapters.get(position);
                updateMissionSpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedChapter = null;
            }
        });
        
        // Difficulty Spinner
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, difficultyLevels);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(difficultyAdapter);
        
        // Correct Answer Spinner - will be populated when choices are entered
        setupCorrectAnswerSpinner();
    }

    /**
     * Updates mission spinner based on selected chapter
     */
    private void updateMissionSpinner() {
        if (selectedChapter == null) {
            return;
        }
        
        List<String> missionTitles = new ArrayList<>();
        for (Mission mission : selectedChapter.missions) {
            missionTitles.add(mission.title);
        }
        
        ArrayAdapter<String> missionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, missionTitles);
        missionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMission.setAdapter(missionAdapter);
        
        binding.spinnerMission.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMission = selectedChapter.missions.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMission = null;
            }
        });
    }

    /**
     * Sets up correct answer spinner to auto-populate from choices
     */
    private void setupCorrectAnswerSpinner() {
        // Add text watchers to update correct answer spinner when choices change
        binding.etChoiceA.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateCorrectAnswerChoices();
        });
        binding.etChoiceB.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateCorrectAnswerChoices();
        });
        binding.etChoiceC.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateCorrectAnswerChoices();
        });
        binding.etChoiceD.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateCorrectAnswerChoices();
        });
    }

    /**
     * Updates correct answer spinner with current choice values
     */
    private void updateCorrectAnswerChoices() {
        List<String> choices = new ArrayList<>();
        
        String choiceA = binding.etChoiceA.getText().toString().trim();
        String choiceB = binding.etChoiceB.getText().toString().trim();
        String choiceC = binding.etChoiceC.getText().toString().trim();
        String choiceD = binding.etChoiceD.getText().toString().trim();
        
        if (!TextUtils.isEmpty(choiceA)) choices.add("A: " + choiceA);
        if (!TextUtils.isEmpty(choiceB)) choices.add("B: " + choiceB);
        if (!TextUtils.isEmpty(choiceC)) choices.add("C: " + choiceC);
        if (!TextUtils.isEmpty(choiceD)) choices.add("D: " + choiceD);
        
        if (!choices.isEmpty()) {
            ArrayAdapter<String> correctAnswerAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, choices);
            correctAnswerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCorrectAnswer.setAdapter(correctAnswerAdapter);
        }
    }

    /**
     * Sets up button click listeners
     */
    private void setupButtons() {
        binding.btnPublish.setOnClickListener(v -> {
            if (validateInputs()) {
                saveQuestion(true);
            }
        });
        
        binding.btnSaveDraft.setOnClickListener(v -> {
            if (validateInputs()) {
                saveQuestion(false);
            }
        });
    }

    /**
     * Populates fields when editing an existing question
     */
    private void populateFieldsForEdit() {
        Intent intent = getIntent();
        
        // Populate question text
        String questionText = intent.getStringExtra(EXTRA_QUESTION_TEXT);
        if (questionText != null) {
            binding.etQuestionText.setText(questionText);
        }
        
        // Populate choices
        String choice1 = intent.getStringExtra(EXTRA_CHOICE_1);
        String choice2 = intent.getStringExtra(EXTRA_CHOICE_2);
        String choice3 = intent.getStringExtra(EXTRA_CHOICE_3);
        String choice4 = intent.getStringExtra(EXTRA_CHOICE_4);
        
        if (choice1 != null) binding.etChoiceA.setText(choice1);
        if (choice2 != null) binding.etChoiceB.setText(choice2);
        if (choice3 != null) binding.etChoiceC.setText(choice3);
        if (choice4 != null) binding.etChoiceD.setText(choice4);
        
        // Update correct answer spinner after choices are populated
        updateCorrectAnswerChoices();
        
        // Select correct answer
        String correctAnswer = intent.getStringExtra(EXTRA_CORRECT_ANSWER);
        if (correctAnswer != null && binding.spinnerCorrectAnswer.getAdapter() != null) {
            for (int i = 0; i < binding.spinnerCorrectAnswer.getAdapter().getCount(); i++) {
                String item = binding.spinnerCorrectAnswer.getAdapter().getItem(i).toString();
                if (item.contains(correctAnswer)) {
                    binding.spinnerCorrectAnswer.setSelection(i);
                    break;
                }
            }
        }
        
        // Update button text for editing
        binding.btnPublish.setText("UPDATE");
        binding.btnSaveDraft.setText("SAVE CHANGES");
    }

    /**
     * Validates all input fields
     */
    private boolean validateInputs() {
        // Clear previous errors
        binding.etQuestionText.setError(null);
        binding.etChoiceA.setError(null);
        binding.etChoiceB.setError(null);
        binding.etChoiceC.setError(null);
        binding.etChoiceD.setError(null);
        
        boolean isValid = true;
        
        // Validate mission selection
        if (selectedChapter == null || selectedMission == null) {
            showError("Please select a chapter and mission");
            return false;
        }
        
        // Validate question text
        String questionText = binding.etQuestionText.getText().toString().trim();
        if (TextUtils.isEmpty(questionText)) {
            binding.etQuestionText.setError("Question text is required");
            isValid = false;
        }
        
        // Validate all choices
        String choiceA = binding.etChoiceA.getText().toString().trim();
        if (TextUtils.isEmpty(choiceA)) {
            binding.etChoiceA.setError("Choice A is required");
            isValid = false;
        }
        
        String choiceB = binding.etChoiceB.getText().toString().trim();
        if (TextUtils.isEmpty(choiceB)) {
            binding.etChoiceB.setError("Choice B is required");
            isValid = false;
        }
        
        String choiceC = binding.etChoiceC.getText().toString().trim();
        if (TextUtils.isEmpty(choiceC)) {
            binding.etChoiceC.setError("Choice C is required");
            isValid = false;
        }
        
        String choiceD = binding.etChoiceD.getText().toString().trim();
        if (TextUtils.isEmpty(choiceD)) {
            binding.etChoiceD.setError("Choice D is required");
            isValid = false;
        }
        
        // Validate correct answer selection
        if (binding.spinnerCorrectAnswer.getAdapter() == null || 
            binding.spinnerCorrectAnswer.getAdapter().getCount() == 0) {
            showError("Please enter all choices first");
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * Saves question to Firestore (create or update)
     */
    private void saveQuestion(boolean published) {
        setLoading(true);
        
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("User not authenticated");
            setLoading(false);
            return;
        }
        
        // Collect data
        String questionText = binding.etQuestionText.getText().toString().trim();
        
        List<String> choices = Arrays.asList(
                binding.etChoiceA.getText().toString().trim(),
                binding.etChoiceB.getText().toString().trim(),
                binding.etChoiceC.getText().toString().trim(),
                binding.etChoiceD.getText().toString().trim()
        );
        
        // Get correct answer from spinner
        String correctAnswerSelection = binding.spinnerCorrectAnswer.getSelectedItem().toString();
        // Extract the actual answer text (remove "A: ", "B: ", etc.)
        String correctAnswer = correctAnswerSelection.substring(3);
        
        String difficulty = binding.spinnerDifficulty.getSelectedItem().toString();
        String explanation = binding.etExplanation.getText().toString().trim();
        
        // Create AdminQuestion object
        AdminQuestion question = new AdminQuestion();
        question.setId(editingQuestionId); // null if creating new
        question.setQuestionText(questionText);
        question.setChoices(choices);
        question.setCorrectAnswer(correctAnswer);
        question.setChapterId(selectedChapter.id);
        question.setMissionId(selectedMission.id);
        question.setDifficulty(difficulty);
        question.setExplanation(explanation);
        question.setPublished(published);
        question.setCreatedBy(currentUser.getUid());
        
        // Save to Firestore via ViewModel
        if (editingQuestionId != null) {
            // Update existing question
            viewModel.updateQuestion(question);
        } else {
            // Create new question
            viewModel.publishQuestion(question);
        }
    }

    /**
     * Observes ViewModel LiveData
     */
    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                setLoading(isLoading);
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
        
        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish(); // Close activity on success
            }
        });
    }

    /**
     * Shows/hides loading overlay
     */
    private void setLoading(boolean loading) {
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPublish.setEnabled(!loading);
        binding.btnSaveDraft.setEnabled(!loading);
    }

    /**
     * Shows error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ═══════════════════════════════════════════════════════════════
    // Data Models
    // ═══════════════════════════════════════════════════════════════

    private static class Chapter {
        String id;
        String title;
        List<Mission> missions = new ArrayList<>();
    }

    private static class Mission {
        String id;
        String title;
        boolean isFirestore; // ← add this

        Mission(String id, String title, boolean isFirestore) {
            this.id          = id;
            this.title       = title;
            this.isFirestore = isFirestore;
        }
    }
}
