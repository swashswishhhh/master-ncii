package com.example.servermasterncii;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Host activity for {@link TerminalEmulatorFragment}.
 * <p>
 * Provides a simple container and injects the fragment on first creation.
 * When used as an interactive mission from QuizActivity, returns
 * {@link Activity#RESULT_OK} with {@link #EXTRA_TASK_COMPLETED} = true.
 */
public class TerminalEmulatorActivity extends AppCompatActivity {

    /** Intent extra key: set to {@code true} when the mission is completed. */
    public static final String EXTRA_TASK_COMPLETED = "extra_task_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terminal_emulator);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                });

        // Only add fragment on first creation to survive config changes
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new TerminalEmulatorFragment())
                    .commit();
        }
    }

    /**
     * Called by the fragment when the user completes their mission or
     * presses exit / close. Sets RESULT_OK so the QuizActivity GameRouter
     * knows to award credit and advance to the next question.
     */
    public void completeAndFinish() {
        Intent result = new Intent();
        result.putExtra(EXTRA_TASK_COMPLETED, true);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
