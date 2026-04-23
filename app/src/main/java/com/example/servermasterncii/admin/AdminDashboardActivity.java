package com.example.servermasterncii.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.servermasterncii.LoginActivity;
import com.example.servermasterncii.ThemeManager;
import com.example.servermasterncii.databinding.ActivityAdminDashboardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private AdminViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        loadAdminInfo();
        setupButtons();
        observeViewModel();
        viewModel.loadStats();
    }

    private void loadAdminInfo() {
        String displayName = getIntent().getStringExtra(LoginActivity.EXTRA_DISPLAY_NAME);
        if (displayName != null) {
            binding.tvAdminName.setText(displayName.toUpperCase());
        }
    }

    private void setupButtons() {

        // ← Add this
        binding.btnAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.servermasterncii.admin.AnalyticsActivity.class)));
        // Manage existing questions
        binding.btnManageQuestions.setOnClickListener(v ->
                startActivity(new Intent(this, QuestionManagerActivity.class)));

        // Add a new question
        binding.btnAddQuestion.setOnClickListener(v ->
                startActivity(new Intent(this, AddQuestionActivity.class)));

        // Add a new mission
        binding.btnAddMission.setOnClickListener(v ->
                startActivity(new Intent(this, AddMissionActivity.class)));

        // ── NEW: Manage (view/delete/publish) missions ──
        binding.btnManageMissions.setOnClickListener(v ->
                startActivity(new Intent(this, MissionManagerActivity.class)));

        binding.btnSignOut.setOnClickListener(v -> showSignOutDialog());
    }

    private void observeViewModel() {
        viewModel.getTotalQuestions().observe(this, count ->
                binding.tvTotalQuestions.setText(String.valueOf(count)));

        viewModel.getTotalUsers().observe(this, count ->
                binding.tvTotalUsers.setText(String.valueOf(count)));

        viewModel.getIsLoading().observe(this, isLoading ->
                binding.loadingOverlay.setVisibility(
                        Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage(error)
                        .setPositiveButton("OK", (d, w) -> viewModel.clearError())
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadStats();
    }

    private void showSignOutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("TERMINATE SESSION")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}