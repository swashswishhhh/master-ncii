# Admin Role-Based Authentication Setup Guide

This guide explains how to set up and use the admin role-based authentication system in Server Master NC II.

## 📋 Overview

The app now supports two user roles:
- **Admin** - Can manage questions, view stats, and access the admin dashboard
- **Student** - Can take quizzes and track progress (default role)

## 🔧 Setup Instructions

### 1. Deploy Firestore Security Rules

The security rules ensure only admins can modify questions.

**Option A: Using Firebase Console**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Firestore Database** → **Rules**
4. Copy the contents of `firestore.rules` from this project
5. Paste into the rules editor
6. Click **Publish**

**Option B: Using Firebase CLI**
```bash
# Install Firebase CLI if you haven't
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in your project (if not done)
firebase init firestore

# Deploy the rules
firebase deploy --only firestore:rules
```

### 2. Assign Admin Role to a User

You need to manually assign the admin role to at least one user in Firestore.

**Steps:**
1. Sign in to your app with the Google account you want to make admin
2. Go to [Firebase Console](https://console.firebase.google.com/)
3. Navigate to **Firestore Database**
4. Find the `users` collection
5. Locate the document with your user's UID
6. Edit the document and set:
   ```
   role: "admin"
   ```
7. Save the changes

**Example Firestore Document Structure:**
```
users/{uid}
  ├─ email: "admin@example.com"
  ├─ displayName: "Admin User"
  ├─ role: "admin"  ← Set this to "admin"
  └─ createdAt: Timestamp
```

### 3. Test the Admin Access

1. **Sign out** from the app (if already signed in)
2. **Sign in** with the admin account
3. You should be redirected to **Admin Dashboard** instead of the student dashboard
4. From the Admin Dashboard, you can:
   - View total users and questions count
   - Navigate to Question Manager
   - Add, edit, and delete questions

## 📱 Features

### Admin Dashboard
- **Statistics Overview**: View total users and questions
- **Quick Actions**: Navigate to Question Manager or sign out
- **User Management**: (Coming soon)

### Question Manager
- **View All Questions**: See all questions organized by category
- **Add Questions**: Create new questions with 4 choices
- **Edit Questions**: Modify existing questions
- **Delete Questions**: Remove questions with confirmation dialog

### Add/Edit Question Form
- Question text (multi-line)
- 4 answer choices
- Correct answer selector (dropdown)
- Category selector (matches level IDs: 1.1, 1.2, etc.)
- Validation for all required fields

## 🔒 Security Features

### Firestore Security Rules
The security rules enforce:
- ✅ Only authenticated users can read questions
- ✅ Only admins can create, update, or delete questions
- ✅ Users can only read/update their own profile (except role field)
- ✅ Only admins can modify user roles
- ✅ Users automatically get "student" role on first login

### Role Check Flow
```
User signs in with Google
    ↓
LoginActivity checks Firestore for user's role
    ↓
If role == "admin" → AdminDashboardActivity
    ↓
Otherwise → MainActivity (Student Dashboard)
```

## 📂 File Structure

### Java Classes
```
app/src/main/java/com/example/servermasterncii/admin/
├── AdminDashboardActivity.java      # Main admin hub
├── QuestionManagerActivity.java     # List all questions
├── AddQuestionActivity.java         # Add/edit question form
├── AdminViewModel.java              # Handles Firestore operations
├── AdminQuestion.java               # Question model class
└── QuestionAdapter.java             # RecyclerView adapter
```

### Layout Files
```
app/src/main/res/layout/
├── activity_admin_dashboard.xml     # Admin dashboard UI
├── activity_question_manager.xml    # Question list UI
├── activity_add_question.xml        # Question form UI
└── item_admin_question.xml          # Question list item
```

### Modified Files
- `LoginActivity.java` - Added role check logic
- `AndroidManifest.xml` - Registered admin activities

## 🎯 Usage Examples

### Adding a Question
1. Open Admin Dashboard
2. Tap "Total Questions" card or navigate to Question Manager
3. Tap the **+** FAB button
4. Fill in:
   - Question text
   - 4 answer choices
   - Select correct answer from dropdown
   - Select category (level)
5. Tap "Save Question"

### Editing a Question
1. In Question Manager, find the question
2. Tap the **Edit** (pencil) icon
3. Modify the fields
4. Tap "Update Question"

### Deleting a Question
1. In Question Manager, find the question
2. Tap the **Delete** (trash) icon
3. Confirm deletion in the dialog

## 🔍 Troubleshooting

### "Permission denied" when adding questions
- **Cause**: Firestore security rules not deployed or user is not admin
- **Solution**: 
  1. Deploy the security rules (see step 1)
  2. Verify the user has `role: "admin"` in Firestore

### Admin user redirected to student dashboard
- **Cause**: Role not set correctly in Firestore
- **Solution**: 
  1. Check Firestore `users/{uid}` document
  2. Ensure `role` field is exactly `"admin"` (lowercase)
  3. Sign out and sign in again

### Questions not loading
- **Cause**: Firestore rules blocking read access
- **Solution**: 
  1. Verify security rules are deployed
  2. Check Firebase Console → Firestore → Rules tab
  3. Ensure authenticated users can read questions

### App crashes when opening admin screens
- **Cause**: Missing dependencies or layout files
- **Solution**: 
  1. Sync Gradle files
  2. Clean and rebuild project
  3. Check Logcat for specific error messages

## 🚀 Next Steps

### Optional Enhancements
1. **User Manager**: View and manage all registered users
2. **Bulk Import**: Import questions from CSV/JSON
3. **Question Categories**: Add more detailed categorization
4. **Analytics**: Track question performance and user engagement
5. **Image Support**: Add images to questions
6. **Question Bank**: Organize questions by difficulty level

## 📝 Firestore Collections Structure

### users
```json
{
  "uid": {
    "email": "user@example.com",
    "displayName": "User Name",
    "role": "admin" | "student",
    "createdAt": Timestamp
  }
}
```

### questions
```json
{
  "questionId": {
    "questionText": "What is a server?",
    "choices": [
      "Choice 1",
      "Choice 2",
      "Choice 3",
      "Choice 4"
    ],
    "correctAnswer": "Choice 1",
    "category": "1.1",
    "createdAt": Timestamp,
    "updatedAt": Timestamp
  }
}
```

## 📞 Support

If you encounter issues:
1. Check the Logcat for error messages (filter by "AdminDashboard", "AdminViewModel", or "AUTH_ROLE")
2. Verify Firebase configuration in `google-services.json`
3. Ensure all dependencies are up to date in `build.gradle`
4. Check Firestore security rules are deployed correctly

## ✅ Checklist

Before going live:
- [ ] Deploy Firestore security rules
- [ ] Assign admin role to at least one user
- [ ] Test admin login flow
- [ ] Test adding/editing/deleting questions
- [ ] Test student login flow (should not see admin features)
- [ ] Verify security rules prevent unauthorized access
- [ ] Test on multiple devices/accounts

---

**Created**: April 2026  
**Version**: 1.0  
**Author**: Server Master NC II Development Team
