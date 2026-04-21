package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityQuestionManagerBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class QuestionManagerActivity extends AppCompatActivity
        implements QuestionAdapter.OnQuestionActionListener {

    private ActivityQuestionManagerBinding binding;
    private AdminViewModel viewModel;
    private QuestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityQuestionManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeViewModel();
        viewModel.loadQuestions();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("// QUESTION MANAGER");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new QuestionAdapter(this);
        binding.recyclerQuestions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerQuestions.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAddQuestion.setOnClickListener(v ->
                startActivity(new Intent(this, AddQuestionActivity.class)));
    }

    private void observeViewModel() {

        // ── Questions list ─────────────────────────────────────────────────
        viewModel.getQuestions().observe(this, questions -> {
            adapter.submitList(questions);

            boolean isEmpty = questions == null || questions.isEmpty();
            binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerQuestions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        // ── Loading state ──────────────────────────────────────────────────
        viewModel.getIsLoading().observe(this, isLoading ->
                binding.progressBar.setVisibility(
                        Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        // ── Errors ─────────────────────────────────────────────────────────
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(0xFFFF2D78)
                        .setTextColor(0xFFFFFFFF)
                        .show();
                viewModel.clearError(); // ← prevents re-showing on rotation
            }
        });

        // ── Success messages ───────────────────────────────────────────────
        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(0xFF00FF9F)
                        .setTextColor(0xFF0A0A0F)
                        .show();
                viewModel.clearSuccessMessage(); // ← prevents re-showing on rotation
            }
        });
    }

    // ── Refresh on return from AddQuestionActivity ─────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadQuestions();
    }

    // ── QuestionAdapter.OnQuestionActionListener ───────────────────────────

    @Override
    public void onEditQuestion(AdminQuestion question) {
        Intent intent = new Intent(this, AddQuestionActivity.class);
        intent.putExtra(AddQuestionActivity.EXTRA_QUESTION_ID,    question.getId());
        intent.putExtra(AddQuestionActivity.EXTRA_QUESTION_TEXT,  question.getQuestionText());
        intent.putExtra(AddQuestionActivity.EXTRA_CHOICE_1,       question.getChoices().get(0));
        intent.putExtra(AddQuestionActivity.EXTRA_CHOICE_2,       question.getChoices().get(1));
        intent.putExtra(AddQuestionActivity.EXTRA_CHOICE_3,       question.getChoices().get(2));
        intent.putExtra(AddQuestionActivity.EXTRA_CHOICE_4,       question.getChoices().get(3));
        intent.putExtra(AddQuestionActivity.EXTRA_CORRECT_ANSWER, question.getCorrectAnswer());
        // Pass chapterId + missionId instead of the missing getCategory()
        intent.putExtra(AddQuestionActivity.EXTRA_CATEGORY,       question.getChapterId());
        startActivity(intent);
    }

    @Override
    public void onDeleteQuestion(AdminQuestion question) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("⚠ DELETE QUESTION")
                .setMessage("This action cannot be undone.\n\n" + question.getQuestionText())
                .setPositiveButton("DELETE", (dialog, which) ->
                        viewModel.deleteQuestion(question.getId()))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}