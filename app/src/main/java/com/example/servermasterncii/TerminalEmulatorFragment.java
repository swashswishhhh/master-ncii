package com.example.servermasterncii;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * TerminalEmulatorFragment — A simulated Windows Command Prompt (cmd.exe).
 * <p>
 * Uses a {@link ScrollView} containing a {@link TextView} for command history
 * and an {@link EditText} for user input. Implements a simple command parser:
 * <ul>
 *     <li><b>ipconfig</b> — displays the static IP set in Mission 4 (192.168.1.1)</li>
 *     <li><b>ping 192.168.1.1</b> — shows 4 "Reply from…" lines with 500ms delay</li>
 *     <li><b>help</b> — lists available commands</li>
 *     <li><b>cls</b> — clears the terminal output</li>
 *     <li><b>hostname</b> — shows a simulated hostname</li>
 *     <li><b>whoami</b> — shows the current simulated user</li>
 *     <li><b>exit</b> — finishes the activity</li>
 * </ul>
 */
public class TerminalEmulatorFragment extends Fragment {

    // Static network config from Mission 4
    private static final String STATIC_IP      = "192.168.1.1";
    private static final String SUBNET_MASK    = "255.255.255.0";
    private static final String DEFAULT_GATEWAY = "192.168.1.254";
    private static final String DNS_SERVER     = "192.168.1.254";
    private static final String MAC_ADDRESS    = "00-1A-2B-3C-4D-5E";
    private static final String HOSTNAME       = "SERVER-NCII";
    private static final String PROMPT         = "C:\\Users\\Admin>";

    // Ping timing constants
    private static final int    PING_COUNT       = 4;
    private static final long   PING_DELAY_MS    = 500;
    private static final int    PING_MIN_TIME_MS = 1;
    private static final int    PING_MAX_TIME_MS = 4;

    // Colors for styled output
    private static final int COLOR_WHITE   = 0xFFCCCCCC;
    private static final int COLOR_GREEN   = 0xFF4EC94E;
    private static final int COLOR_YELLOW  = 0xFFFFD54F;
    private static final int COLOR_RED     = 0xFFF44336;
    private static final int COLOR_CYAN    = 0xFF4DD0E1;

    private TextView tvOutput;
    private EditText etInput;
    private ScrollView scrollOutput;
    private LinearLayout hintBanner;

    // Celebration overlay views
    private FrameLayout overlayContainer;
    private LottieAnimationView lottieAnimation;
    private TextView tvSuccessTitle;
    private TextView tvSuccessMessage;
    private MaterialButton btnDismissOverlay;
    private MediaPlayer mediaPlayer;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SpannableStringBuilder outputBuffer = new SpannableStringBuilder();
    private final Random random = new Random();

    private boolean isProcessingCommand = false;

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terminal_emulator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvOutput     = view.findViewById(R.id.tvOutput);
        etInput      = view.findViewById(R.id.etInput);
        scrollOutput = view.findViewById(R.id.scrollOutput);
        hintBanner   = view.findViewById(R.id.hintBanner);

        // Celebration overlay views
        overlayContainer  = view.findViewById(R.id.overlayContainer);
        lottieAnimation   = view.findViewById(R.id.lottieAnimation);
        tvSuccessTitle    = view.findViewById(R.id.tvSuccessTitle);
        tvSuccessMessage  = view.findViewById(R.id.tvSuccessMessage);
        btnDismissOverlay = view.findViewById(R.id.btnDismissOverlay);

        // Close button (finishes hosting Activity with result)
        view.findViewById(R.id.btnWinClose).setOnClickListener(v -> finishWithResult());

        // Dismiss hint banner
        view.findViewById(R.id.btnDismissHint).setOnClickListener(v ->
                hintBanner.setVisibility(View.GONE));

        // Dismiss celebration overlay → return result
        btnDismissOverlay.setOnClickListener(v -> {
            overlayContainer.setVisibility(View.GONE);
            lottieAnimation.cancelAnimation();
            finishWithResult();
        });

        // Handle "Enter" / "Done" on soft keyboard
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                onCommandSubmitted();
                return true;
            }
            return false;
        });

        // Show boot banner
        showBootBanner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        releaseMediaPlayer();
    }

    // =====================================================================
    // Boot banner
    // =====================================================================

    private void showBootBanner() {
        appendColored("Microsoft Windows [Version 10.0.19045.3324]\n", COLOR_WHITE);
        appendColored("(c) Microsoft Corporation. All rights reserved.\n\n", COLOR_WHITE);
    }

    // =====================================================================
    // Command submission
    // =====================================================================

    private void onCommandSubmitted() {
        if (isProcessingCommand) return;

        String raw = etInput.getText().toString().trim();
        etInput.setText("");

        // Echo the command line
        appendColored(PROMPT, COLOR_WHITE);
        appendColored(raw + "\n", COLOR_GREEN);

        if (raw.isEmpty()) {
            scrollToBottom();
            return;
        }

        parseCommand(raw);
    }

    // =====================================================================
    // Command parser
    // =====================================================================

    private void parseCommand(String raw) {
        String cmd = raw.toLowerCase(Locale.ROOT).trim();

        if (cmd.equals("ipconfig")) {
            executeIpconfig();
        } else if (cmd.equals("ipconfig /all")) {
            executeIpconfigAll();
        } else if (cmd.startsWith("ping ")) {
            String target = raw.substring(5).trim();
            executePing(target);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            executeHelp();
        } else if (cmd.equals("cls")) {
            executeCls();
        } else if (cmd.equals("hostname")) {
            executeHostname();
        } else if (cmd.equals("whoami")) {
            executeWhoami();
        } else if (cmd.equals("exit")) {
            playCelebration();
        } else if (cmd.equals("ver")) {
            executeVer();
        } else if (cmd.equals("systeminfo")) {
            executeSysteminfo();
        } else {
            appendColored("'" + raw + "' is not recognized as an internal or external command,\n"
                    + "operable program or batch file.\n\n", COLOR_RED);
            scrollToBottom();
        }
    }

    // =====================================================================
    // Command: ipconfig
    // =====================================================================

    private void executeIpconfig() {
        appendColored("\nWindows IP Configuration\n\n", COLOR_YELLOW);
        appendColored("Ethernet adapter Ethernet:\n\n", COLOR_WHITE);
        appendColored("   Connection-specific DNS Suffix  . : local\n", COLOR_WHITE);
        appendColored("   IPv4 Address. . . . . . . . . . : ", COLOR_WHITE);
        appendColored(STATIC_IP + "\n", COLOR_GREEN);
        appendColored("   Subnet Mask . . . . . . . . . . : " + SUBNET_MASK + "\n", COLOR_WHITE);
        appendColored("   Default Gateway . . . . . . . . : " + DEFAULT_GATEWAY + "\n", COLOR_WHITE);
        appendColored("\n", COLOR_WHITE);
        scrollToBottom();
    }

    // =====================================================================
    // Command: ipconfig /all
    // =====================================================================

    private void executeIpconfigAll() {
        appendColored("\nWindows IP Configuration\n\n", COLOR_YELLOW);
        appendColored("   Host Name . . . . . . . . . . . : " + HOSTNAME + "\n", COLOR_WHITE);
        appendColored("   Primary Dns Suffix  . . . . . . : \n", COLOR_WHITE);
        appendColored("   Node Type . . . . . . . . . . . : Hybrid\n", COLOR_WHITE);
        appendColored("   IP Routing Enabled. . . . . . . : No\n", COLOR_WHITE);
        appendColored("   WINS Proxy Enabled. . . . . . . : No\n\n", COLOR_WHITE);

        appendColored("Ethernet adapter Ethernet:\n\n", COLOR_WHITE);
        appendColored("   Connection-specific DNS Suffix  . : local\n", COLOR_WHITE);
        appendColored("   Description . . . . . . . . . . : Intel(R) Ethernet Controller\n", COLOR_WHITE);
        appendColored("   Physical Address. . . . . . . . : " + MAC_ADDRESS + "\n", COLOR_WHITE);
        appendColored("   DHCP Enabled. . . . . . . . . . : No\n", COLOR_WHITE);
        appendColored("   Autoconfiguration Enabled . . . : Yes\n", COLOR_WHITE);
        appendColored("   IPv4 Address. . . . . . . . . . : ", COLOR_WHITE);
        appendColored(STATIC_IP + "(Preferred)\n", COLOR_GREEN);
        appendColored("   Subnet Mask . . . . . . . . . . : " + SUBNET_MASK + "\n", COLOR_WHITE);
        appendColored("   Default Gateway . . . . . . . . : " + DEFAULT_GATEWAY + "\n", COLOR_WHITE);
        appendColored("   DNS Servers . . . . . . . . . . : " + DNS_SERVER + "\n", COLOR_WHITE);
        appendColored("   NetBIOS over Tcpip. . . . . . . : Enabled\n\n", COLOR_WHITE);
        scrollToBottom();
    }

    // =====================================================================
    // Command: ping <target>
    // =====================================================================

    private void executePing(String target) {
        isProcessingCommand = true;
        etInput.setEnabled(false);

        appendColored("\nPinging " + target + " with 32 bytes of data:\n", COLOR_WHITE);
        scrollToBottom();

        // Send 4 replies with realistic delay
        for (int i = 0; i < PING_COUNT; i++) {
            final int index = i;
            handler.postDelayed(() -> {
                int time = PING_MIN_TIME_MS + random.nextInt(PING_MAX_TIME_MS - PING_MIN_TIME_MS + 1);
                appendColored("Reply from " + target + ": bytes=32 time=" + time + "ms TTL=128\n",
                        COLOR_GREEN);
                scrollToBottom();

                // After the last reply, show statistics
                if (index == PING_COUNT - 1) {
                    handler.postDelayed(() -> showPingStatistics(target), PING_DELAY_MS);
                }
            }, PING_DELAY_MS * (i + 1));
        }
    }

    private void showPingStatistics(String target) {
        appendColored("\nPing statistics for " + target + ":\n", COLOR_YELLOW);
        appendColored("    Packets: Sent = 4, Received = 4, Lost = 0 (0% loss),\n", COLOR_WHITE);
        appendColored("Approximate round trip times in milli-seconds:\n", COLOR_WHITE);
        appendColored("    Minimum = 1ms, Maximum = 4ms, Average = 2ms\n\n", COLOR_WHITE);
        scrollToBottom();

        isProcessingCommand = false;
        etInput.setEnabled(true);
        etInput.requestFocus();
    }

    // =====================================================================
    // Command: help
    // =====================================================================

    private void executeHelp() {
        appendColored("\nAvailable commands:\n\n", COLOR_YELLOW);
        appendColored("  IPCONFIG       ", COLOR_CYAN);
        appendColored("Displays IP address configuration\n", COLOR_WHITE);
        appendColored("  IPCONFIG /ALL  ", COLOR_CYAN);
        appendColored("Displays full IP configuration details\n", COLOR_WHITE);
        appendColored("  PING <host>    ", COLOR_CYAN);
        appendColored("Sends ICMP echo requests to a host\n", COLOR_WHITE);
        appendColored("  HOSTNAME       ", COLOR_CYAN);
        appendColored("Displays the computer name\n", COLOR_WHITE);
        appendColored("  WHOAMI         ", COLOR_CYAN);
        appendColored("Displays the current user\n", COLOR_WHITE);
        appendColored("  VER            ", COLOR_CYAN);
        appendColored("Displays the Windows version\n", COLOR_WHITE);
        appendColored("  SYSTEMINFO     ", COLOR_CYAN);
        appendColored("Displays system information\n", COLOR_WHITE);
        appendColored("  CLS            ", COLOR_CYAN);
        appendColored("Clears the terminal screen\n", COLOR_WHITE);
        appendColored("  EXIT           ", COLOR_CYAN);
        appendColored("Closes the terminal\n\n", COLOR_WHITE);
        scrollToBottom();
    }

    // =====================================================================
    // Command: cls
    // =====================================================================

    private void executeCls() {
        outputBuffer.clear();
        tvOutput.setText("");
    }

    // =====================================================================
    // Command: hostname
    // =====================================================================

    private void executeHostname() {
        appendColored(HOSTNAME + "\n\n", COLOR_GREEN);
        scrollToBottom();
    }

    // =====================================================================
    // Command: whoami
    // =====================================================================

    private void executeWhoami() {
        appendColored(HOSTNAME.toLowerCase(Locale.ROOT) + "\\admin\n\n", COLOR_GREEN);
        scrollToBottom();
    }

    // =====================================================================
    // Command: ver
    // =====================================================================

    private void executeVer() {
        appendColored("\nMicrosoft Windows [Version 10.0.19045.3324]\n\n", COLOR_WHITE);
        scrollToBottom();
    }

    // =====================================================================
    // Command: systeminfo
    // =====================================================================

    private void executeSysteminfo() {
        String date = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a",
                Locale.US).format(new Date());

        appendColored("\nHost Name:                 " + HOSTNAME + "\n", COLOR_WHITE);
        appendColored("OS Name:                   Microsoft Windows 10 Pro\n", COLOR_WHITE);
        appendColored("OS Version:                10.0.19045 Build 19045\n", COLOR_WHITE);
        appendColored("System Type:               x64-based PC\n", COLOR_WHITE);
        appendColored("Network Card(s):           1 NIC(s) Installed.\n", COLOR_WHITE);
        appendColored("                           [01]: Intel(R) Ethernet Controller\n", COLOR_WHITE);
        appendColored("                                 Connection Name: Ethernet\n", COLOR_WHITE);
        appendColored("                                 DHCP Enabled:    No\n", COLOR_WHITE);
        appendColored("                                 IP address(es)\n", COLOR_WHITE);
        appendColored("                                 [01]: ", COLOR_WHITE);
        appendColored(STATIC_IP + "\n", COLOR_GREEN);
        appendColored("System Boot Time:          " + date + "\n\n", COLOR_WHITE);
        scrollToBottom();
    }

    // =====================================================================
    // Helpers — styled output
    // =====================================================================

    /**
     * Appends colored text to the output buffer and updates the TextView.
     */
    private void appendColored(String text, int color) {
        SpannableString span = new SpannableString(text);
        span.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        outputBuffer.append(span);
        tvOutput.setText(outputBuffer);
    }

    /**
     * Scrolls the output ScrollView to the bottom after a brief layout pass.
     */
    private void scrollToBottom() {
        scrollOutput.post(() -> scrollOutput.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Finishes the hosting Activity, routing through
     * {@link TerminalEmulatorActivity#completeAndFinish()} to set RESULT_OK.
     */
    private void finishWithResult() {
        if (getActivity() instanceof TerminalEmulatorActivity) {
            ((TerminalEmulatorActivity) getActivity()).completeAndFinish();
        } else if (getActivity() != null) {
            getActivity().finish();
        }
    }

    // =====================================================================
    // Celebration: Lottie + Sound
    // =====================================================================

    /**
     * Shows the dark overlay, plays the Lottie celebration animation,
     * and triggers a system notification chime as a success sound effect.
     */
    private void playCelebration() {
        // Show the overlay
        overlayContainer.setVisibility(View.VISIBLE);
        overlayContainer.setAlpha(0f);
        overlayContainer.animate()
                .alpha(1f)
                .setDuration(400)
                .start();

        // Reset and play Lottie
        lottieAnimation.setProgress(0f);
        lottieAnimation.playAnimation();

        // Play system notification sound as a success chime
        playSuccessSound();

        // After the animation ends, reveal the success text
        lottieAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showSuccessText();
            }
        });

        // Fallback: show text after 2.5 s even if animation listener doesn't fire
        handler.postDelayed(this::showSuccessText, 2500);
    }

    /**
     * Fades in the "Task Completed!" text and the Continue button
     * with a pleasant overshoot scale animation.
     */
    private void showSuccessText() {
        if (tvSuccessTitle == null) return;

        // Guard against double-firing (listener + handler)
        if (tvSuccessTitle.getVisibility() == View.VISIBLE) return;

        tvSuccessTitle.setVisibility(View.VISIBLE);
        tvSuccessMessage.setVisibility(View.VISIBLE);
        btnDismissOverlay.setVisibility(View.VISIBLE);

        // Scale-up entrance
        View[] views = {tvSuccessTitle, tvSuccessMessage, btnDismissOverlay};
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setScaleX(0.6f);
            view.setScaleY(0.6f);
            view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(i * 150L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    /**
     * Plays the system default notification sound as a success chime.
     */
    private void playSuccessSound() {
        try {
            releaseMediaPlayer();
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(requireContext(), notification);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Non-critical — the animation alone is fine
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
    }
}
