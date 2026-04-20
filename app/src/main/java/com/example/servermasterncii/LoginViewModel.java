package com.example.servermasterncii;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginViewModel — manages Firebase authentication state for LoginActivity.
 *
 * <h3>Auth State Machine</h3>
 * <pre>
 *   IDLE ──► LOADING ──► SUCCESS(user)
 *                    └──► ERROR(message)
 * </pre>
 *
 * <p>The Activity observes {@link #getAuthState()} and reacts to each state
 * transition. The ViewModel never holds a reference to the Activity or any
 * Android View — it only exposes LiveData.</p>
 *
 * <h3>Usage</h3>
 * <ol>
 *   <li>Google: call {@link #signInWithCredential(AuthCredential)} after
 *       receiving the GoogleSignInAccount from the launcher result.</li>
 *   <li>Facebook: call {@link #signInWithCredential(AuthCredential)} after
 *       receiving the AccessToken from the Facebook SDK callback.</li>
 *   <li>Guest: call {@link #signInAsGuest()}.</li>
 * </ol>
 */
public class LoginViewModel extends AndroidViewModel {

    // ── Auth state sealed-class equivalent ─────────────────────────────────

    public enum StateType { IDLE, LOADING, SUCCESS, ERROR }

    /** Immutable snapshot of the current auth state. */
    public static final class AuthState {
        public final StateType type;
        public final FirebaseUser user;   // non-null on SUCCESS
        public final String     message; // non-null on ERROR

        private AuthState(StateType type, FirebaseUser user, String message) {
            this.type    = type;
            this.user    = user;
            this.message = message;
        }

        public static AuthState idle()                        { return new AuthState(StateType.IDLE,    null, null); }
        public static AuthState loading()                     { return new AuthState(StateType.LOADING, null, null); }
        public static AuthState success(FirebaseUser user)    { return new AuthState(StateType.SUCCESS, user, null); }
        public static AuthState error(String message)         { return new AuthState(StateType.ERROR,   null, message); }
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    private final FirebaseAuth firebaseAuth;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.idle());

    // ── Constructor ────────────────────────────────────────────────────────

    public LoginViewModel(@NonNull Application application) {
        super(application);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // ── Exposed LiveData ───────────────────────────────────────────────────

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    // ── Auth actions ───────────────────────────────────────────────────────

    /**
     * Signs in using a Firebase {@link AuthCredential}.
     * Works for both Google ({@code GoogleAuthProvider.getCredential(...)})
     * and Facebook ({@code FacebookAuthProvider.getCredential(...)}).
     */
    public void signInWithCredential(@NonNull AuthCredential credential) {
        authState.setValue(AuthState.loading());

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        authState.setValue(AuthState.success(user));
                    } else {
                        authState.setValue(AuthState.error("Authentication failed — no user returned."));
                    }
                })
                .addOnFailureListener(e -> {
                    authState.setValue(AuthState.error(friendlyError(e)));
                });
    }

    /**
     * Signs in anonymously (Guest Mode).
     * Progress is saved locally; the account can be upgraded later.
     */
    public void signInAsGuest() {
        authState.setValue(AuthState.loading());

        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        authState.setValue(AuthState.success(user));
                    } else {
                        authState.setValue(AuthState.error("Guest sign-in failed."));
                    }
                })
                .addOnFailureListener(e -> {
                    authState.setValue(AuthState.error(friendlyError(e)));
                });
    }

    /** Resets state back to IDLE (e.g. after showing an error Snackbar). */
    public void resetState() {
        authState.setValue(AuthState.idle());
    }

    // ── Error mapping ──────────────────────────────────────────────────────

    /**
     * Converts Firebase exceptions into user-friendly messages.
     * Covers the most common failure modes for a mobile auth flow.
     */
    private String friendlyError(Exception e) {
        if (e == null) return "Unknown error occurred.";
        String msg = e.getMessage() != null ? e.getMessage() : "";

        if (msg.contains("network") || msg.contains("NETWORK_ERROR")
                || msg.contains("Unable to resolve host")) {
            return "Network error — check your connection and try again.";
        }
        if (msg.contains("SIGN_IN_CANCELLED") || msg.contains("12501")) {
            return "Sign-in cancelled.";
        }
        if (msg.contains("SIGN_IN_FAILED") || msg.contains("10:")) {
            return "Sign-in failed — check your SHA-1 fingerprint in Firebase Console.";
        }
        if (msg.contains("account-exists-with-different-credential")) {
            return "An account already exists with a different sign-in method.";
        }
        if (msg.contains("user-disabled")) {
            return "This account has been disabled. Contact support.";
        }
        if (msg.contains("too-many-requests")) {
            return "Too many attempts. Please wait and try again.";
        }
        // Facebook-specific
        if (msg.contains("FacebookException") || msg.contains("FacebookAuthException")) {
            return "Facebook login failed — please try again.";
        }

        return "Authentication error: " + msg;
    }
}
