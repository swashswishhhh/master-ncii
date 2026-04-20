package com.example.servermasterncii;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.servermasterncii.databinding.ActivityLoginBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * LoginActivity — Cyber-Terminal themed Firebase authentication screen.
 *
 * <h3>Auth Providers</h3>
 * <ul>
 *   <li><b>Google</b> — uses {@link GoogleSignInClient} + Firebase credential</li>
 *   <li><b>Facebook</b> — uses Facebook SDK {@link LoginManager} + Firebase credential</li>
 *   <li><b>Guest</b> — Firebase anonymous sign-in</li>
 * </ul>
 *
 * <h3>State Flow</h3>
 * All auth logic lives in {@link LoginViewModel}. This Activity only:
 * <ol>
 *   <li>Triggers auth actions on button clicks</li>
 *   <li>Observes {@code LoginViewModel.getAuthState()} LiveData</li>
 *   <li>Reacts to SUCCESS by navigating to {@link MainActivity}</li>
 *   <li>Reacts to ERROR by showing a Snackbar</li>
 * </ol>
 *
 * <h3>Navigation on Success</h3>
 * Passes {@code displayName} and {@code photoUrl} to {@link MainActivity}
 * via Intent extras.
 *
 * <h3>Setup Required</h3>
 * See the dependency guide at the bottom of this file.
 */
public class LoginActivity extends AppCompatActivity {

    // ── Intent extra keys (read by MainActivity) ───────────────────────────
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    public static final String EXTRA_PHOTO_URL    = "extra_photo_url";
    public static final String EXTRA_IS_GUEST     = "extra_is_guest";

    // ── ViewModel ──────────────────────────────────────────────────────────
    private LoginViewModel viewModel;

    // ── ViewBinding ────────────────────────────────────────────────────────
    private ActivityLoginBinding binding;

    // ── Google Sign-In ─────────────────────────────────────────────────────
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            // Exchange Google token for Firebase credential
                            viewModel.signInWithCredential(
                                    GoogleAuthProvider.getCredential(account.getIdToken(), null));
                        } catch (ApiException e) {
                            // Map common Google error codes
                            String msg;
                            switch (e.getStatusCode()) {
                                case 12501: msg = "Sign-in cancelled."; break;
                                case 10:    msg = "Sign-in failed — check SHA-1 in Firebase Console."; break;
                                case 7:     msg = "Network error — check your connection."; break;
                                default:    msg = "Google sign-in error: " + e.getStatusCode(); break;
                            }
                            showError(msg);
                            viewModel.resetState();
                        }
                    });

    // ── Facebook Login ─────────────────────────────────────────────────────
    private CallbackManager facebookCallbackManager;

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // TEMPORARY: Print Facebook Key Hash to Logcat
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                Log.d("FB_KEY_HASH", "Key Hash: " + keyHash);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("FB_KEY_HASH", "NameNotFoundException", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e("FB_KEY_HASH", "NoSuchAlgorithmException", e);
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        setupGoogleSignIn();
        setupFacebookLogin();
        setupButtons();
        observeAuthState();
        animateCardEntrance();

        // Log configuration status
        logConfigurationStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Google Sign-In setup
    // ══════════════════════════════════════════════════════════════════════

    private void setupGoogleSignIn() {
        // Replace R.string.default_web_client_id with your actual Web Client ID
        // from Firebase Console → Authentication → Sign-in method → Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Facebook Login setup
    // ══════════════════════════════════════════════════════════════════════

    private void setupFacebookLogin() {
        facebookCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(
                facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("FB_LOGIN", "✅ Facebook login successful");
                        AccessToken token = loginResult.getAccessToken();
                        Log.d("FB_LOGIN", "Token: " + token.getToken().substring(0, 20) + "...");
                        Log.d("FB_LOGIN", "User ID: " + token.getUserId());
                        
                        // Exchange Facebook token for Firebase credential
                        viewModel.signInWithCredential(
                                FacebookAuthProvider.getCredential(token.getToken()));
                    }

                    @Override
                    public void onCancel() {
                        Log.w("FB_LOGIN", "⚠️ Facebook login cancelled by user");
                        showError("Facebook login cancelled.");
                        viewModel.resetState();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e("FB_LOGIN", "❌ Facebook login error", error);
                        
                        String errorMsg = error.getMessage();
                        String userMsg;
                        
                        // Parse common Facebook errors
                        if (errorMsg != null) {
                            if (errorMsg.contains("key hash")) {
                                userMsg = "Key hash not configured. Check Logcat for FB_KEY_HASH and add it to Meta Developer Console.";
                            } else if (errorMsg.contains("190")) {
                                userMsg = "Invalid OAuth token. Verify Facebook App ID and Client Token in strings.xml.";
                            } else if (errorMsg.contains("network")) {
                                userMsg = "Network error. Check your internet connection.";
                            } else {
                                userMsg = "Facebook error: " + errorMsg;
                            }
                        } else {
                            userMsg = "Facebook login failed. Check Logcat for details.";
                        }
                        
                        showError(userMsg);
                        viewModel.resetState();
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Button wiring
    // ══════════════════════════════════════════════════════════════════════

    private void setupButtons() {
        binding.btnGoogle.setOnClickListener(v -> {
            // Sign out first to force account picker every time
            googleSignInClient.signOut().addOnCompleteListener(task ->
                    googleLauncher.launch(googleSignInClient.getSignInIntent()));
        });

        binding.btnFacebook.setOnClickListener(v ->
                LoginManager.getInstance().logInWithReadPermissions(
                        this,
                        facebookCallbackManager,
                        Arrays.asList("public_profile")));  // ✅ Only public_profile

        binding.btnGuest.setOnClickListener(v -> viewModel.signInAsGuest());
    }

    // ══════════════════════════════════════════════════════════════════════
    // Auth state observer
    // ══════════════════════════════════════════════════════════════════════

    private void observeAuthState() {
        viewModel.getAuthState().observe(this, state -> {
            switch (state.type) {
                case IDLE:
                    setLoading(false);
                    break;

                case LOADING:
                    setLoading(true);
                    break;

                case SUCCESS:
                    setLoading(false);
                    navigateToDashboard(state.user);
                    break;

                case ERROR:
                    setLoading(false);
                    showError(state.message);
                    viewModel.resetState();
                    break;
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Navigates to {@link MainActivity} (the Dashboard / Saga Map),
     * passing the user's display name and photo URL as extras.
     */
    private void navigateToDashboard(FirebaseUser user) {
        boolean isGuest = user.isAnonymous();

        String displayName = isGuest
                ? "GUEST_OPERATIVE"
                : (user.getDisplayName() != null ? user.getDisplayName() : "OPERATIVE");

        String photoUrl = (user.getPhotoUrl() != null)
                ? user.getPhotoUrl().toString()
                : "";

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_DISPLAY_NAME, displayName);
        intent.putExtra(EXTRA_PHOTO_URL,    photoUrl);
        intent.putExtra(EXTRA_IS_GUEST,     isGuest);
        // Clear the back stack so the user can't go back to login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private void setLoading(boolean loading) {
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGoogle.setEnabled(!loading);
        binding.btnFacebook.setEnabled(!loading);
        binding.btnGuest.setEnabled(!loading);
        binding.loginCard.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void showError(String message) {
        if (binding == null) return;
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(0xFFFF3B3B)
                .setTextColor(0xFFFFFFFF)
                .setActionTextColor(0xFF39FF7F)
                .setAction("OK", v -> { /* dismiss */ })
                .show();
    }

    /** Slides the login card up from below on first launch. */
    private void animateCardEntrance() {
        binding.loginCard.setTranslationY(120f);
        binding.loginCard.setAlpha(0f);
        binding.loginCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
    }

    /**
     * Logs the current authentication configuration status for debugging.
     * Check Logcat with filter "AUTH_CONFIG" to verify your setup.
     */
    private void logConfigurationStatus() {
        Log.d("AUTH_CONFIG", "════════════════════════════════════════");
        Log.d("AUTH_CONFIG", "🔧 AUTHENTICATION CONFIGURATION STATUS");
        Log.d("AUTH_CONFIG", "════════════════════════════════════════");
        
        // Package name
        Log.d("AUTH_CONFIG", "📦 Package: " + getPackageName());
        
        // Facebook configuration
        String fbAppId = getString(R.string.facebook_app_id);
        String fbClientToken = getString(R.string.facebook_client_token);
        Log.d("AUTH_CONFIG", "📘 Facebook App ID: " + fbAppId);
        Log.d("AUTH_CONFIG", "📘 Facebook Client Token: " + fbClientToken.substring(0, 8) + "...");
        
        // Google configuration
        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.contains("YOUR_")) {
            Log.e("AUTH_CONFIG", "❌ Google Web Client ID NOT SET! Update strings.xml");
        } else {
            Log.d("AUTH_CONFIG", "✅ Google Web Client ID configured");
        }
        
        // Facebook Access Token check
        AccessToken fbToken = AccessToken.getCurrentAccessToken();
        boolean isFbLoggedIn = fbToken != null && !fbToken.isExpired();
        Log.d("AUTH_CONFIG", "📘 Facebook logged in: " + isFbLoggedIn);
        
        Log.d("AUTH_CONFIG", "════════════════════════════════════════");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Activity Result Handling (CRITICAL for Facebook Login)
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Pass the result to Facebook SDK
        if (facebookCallbackManager != null) {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
            Log.d("FB_LOGIN", "📲 Activity result passed to Facebook SDK");
        }
    }
}
