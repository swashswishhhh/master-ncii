# Role Check Implementation in LoginActivity

This document shows the exact implementation of role-based routing in `LoginActivity.java`.

## 🎯 Key Changes

The role check logic has already been implemented in your `LoginActivity.java`. Here's what it does:

## 📍 Location in Code

**File**: `app/src/main/java/com/example/servermasterncii/LoginActivity.java`

**Method**: `navigateToDashboard(FirebaseUser user)` (lines ~250-310)

## 🔍 Implementation Details

### 1. Main Navigation Method

```java
private void navigateToDashboard(FirebaseUser user) {
    boolean isGuest = user.isAnonymous();

    // Guest users always go to MainActivity
    if (isGuest) {
        navigateToMainActivity(user, true);
        return;
    }

    // Check user role in Firestore
    setLoading(true);
    com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                setLoading(false);
                
                if (documentSnapshot.exists()) {
                    String role = documentSnapshot.getString("role");
                    Log.d("AUTH_ROLE", "User role: " + role);
                    
                    if ("admin".equals(role)) {
                        // Navigate to Admin Dashboard
                        navigateToAdminDashboard(user);
                    } else {
                        // Navigate to Student Dashboard (MainActivity)
                        navigateToMainActivity(user, false);
                    }
                } else {
                    // User document doesn't exist - create it with student role
                    Log.d("AUTH_ROLE", "User document not found, creating with student role");
                    createUserDocument(user);
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Log.e("AUTH_ROLE", "Error fetching user role", e);
                showError("Failed to fetch user data. Please try again.");
                // Default to MainActivity on error
                navigateToMainActivity(user, false);
            });
}
```

### 2. Create User Document (Auto-assign Student Role)

```java
private void createUserDocument(FirebaseUser user) {
    java.util.Map<String, Object> userData = new java.util.HashMap<>();
    userData.put("email", user.getEmail());
    userData.put("displayName", user.getDisplayName());
    userData.put("role", "student");  // Default role
    userData.put("createdAt", com.google.firebase.Timestamp.now());

    com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d("AUTH_ROLE", "User document created with student role");
                navigateToMainActivity(user, false);
            })
            .addOnFailureListener(e -> {
                Log.e("AUTH_ROLE", "Error creating user document", e);
                showError("Failed to create user profile. Please try again.");
            });
}
```

### 3. Navigate to Admin Dashboard

```java
private void navigateToAdminDashboard(FirebaseUser user) {
    String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Admin";
    String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

    Intent intent = new Intent(this, com.example.servermasterncii.admin.AdminDashboardActivity.class);
    intent.putExtra(EXTRA_DISPLAY_NAME, displayName);
    intent.putExtra(EXTRA_PHOTO_URL, photoUrl);
    intent.putExtra(EXTRA_IS_GUEST, false);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

### 4. Navigate to Student Dashboard (MainActivity)

```java
private void navigateToMainActivity(FirebaseUser user, boolean isGuest) {
    String displayName = isGuest
            ? "GUEST_OPERATIVE"
            : (user.getDisplayName() != null ? user.getDisplayName() : "OPERATIVE");

    String photoUrl = (user.getPhotoUrl() != null)
            ? user.getPhotoUrl().toString()
            : "";

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra(EXTRA_DISPLAY_NAME, displayName);
    intent.putExtra(EXTRA_PHOTO_URL, photoUrl);
    intent.putExtra(EXTRA_IS_GUEST, isGuest);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

## 🔄 Flow Diagram

```
User Signs In with Google
         ↓
observeAuthState() → SUCCESS
         ↓
navigateToDashboard(user)
         ↓
    Is Guest? ──Yes──→ MainActivity
         ↓ No
         ↓
Fetch user document from Firestore
         ↓
    ┌────┴────┐
    ↓         ↓
Document   Document
Exists?    Missing?
    ↓         ↓
    ↓    Create with
    ↓    "student" role
    ↓         ↓
Check role   MainActivity
    ↓
┌───┴───┐
↓       ↓
"admin" Other
↓       ↓
Admin   Main
Dashboard Activity
```

## 🔐 Security Notes

1. **Role is stored in Firestore** - Not in Firebase Auth custom claims
2. **Role check happens on every login** - Fresh data from Firestore
3. **Default role is "student"** - New users automatically get student access
4. **Guest users bypass role check** - Always go to MainActivity
5. **Error handling** - Falls back to MainActivity if Firestore fetch fails

## 📊 Firestore Document Structure

### User Document Path
```
users/{uid}
```

### Required Fields
```json
{
  "email": "user@example.com",
  "displayName": "User Name",
  "role": "admin",  // or "student"
  "createdAt": Timestamp
}
```

## 🐛 Debugging

### Enable Logging
The implementation includes debug logs with tag `"AUTH_ROLE"`:

```java
Log.d("AUTH_ROLE", "User role: " + role);
Log.d("AUTH_ROLE", "User document not found, creating with student role");
Log.e("AUTH_ROLE", "Error fetching user role", e);
```

### View Logs in Android Studio
1. Open **Logcat**
2. Filter by tag: `AUTH_ROLE`
3. Watch for role detection and navigation decisions

### Common Log Messages
- ✅ `"User role: admin"` - Admin detected, routing to AdminDashboard
- ✅ `"User role: student"` - Student detected, routing to MainActivity
- ⚠️ `"User document not found, creating with student role"` - New user, creating document
- ❌ `"Error fetching user role"` - Firestore error, check connection and rules

## ✅ Testing Checklist

### Test Admin Flow
1. [ ] Sign in with admin account
2. [ ] Verify log shows `"User role: admin"`
3. [ ] Confirm navigation to AdminDashboardActivity
4. [ ] Verify admin features are accessible

### Test Student Flow
1. [ ] Sign in with non-admin account
2. [ ] Verify log shows `"User role: student"` or document creation
3. [ ] Confirm navigation to MainActivity
4. [ ] Verify admin features are NOT accessible

### Test Guest Flow
1. [ ] Sign in as guest
2. [ ] Confirm immediate navigation to MainActivity
3. [ ] Verify no Firestore query is made

### Test Error Handling
1. [ ] Disable internet connection
2. [ ] Sign in
3. [ ] Verify error message is shown
4. [ ] Verify fallback to MainActivity

## 🔧 Customization Options

### Change Default Role
To change the default role for new users, modify `createUserDocument()`:

```java
userData.put("role", "student");  // Change to "admin" or custom role
```

### Add More Roles
To support additional roles (e.g., "teacher", "moderator"):

```java
if ("admin".equals(role)) {
    navigateToAdminDashboard(user);
} else if ("teacher".equals(role)) {
    navigateToTeacherDashboard(user);
} else {
    navigateToMainActivity(user, false);
}
```

### Use Firebase Auth Custom Claims (Alternative)
For production apps with many admins, consider using Firebase Auth custom claims instead of Firestore:

```java
user.getIdToken(false).addOnSuccessListener(result -> {
    Map<String, Object> claims = result.getClaims();
    if (claims.containsKey("admin") && (Boolean) claims.get("admin")) {
        navigateToAdminDashboard(user);
    } else {
        navigateToMainActivity(user, false);
    }
});
```

## 📚 Related Files

- `LoginActivity.java` - Contains all role check logic
- `AdminDashboardActivity.java` - Admin landing page
- `MainActivity.java` - Student landing page
- `firestore.rules` - Security rules enforcing role permissions

---

**Implementation Status**: ✅ Complete  
**Last Updated**: April 2026
