# Firestore Setup Guide

## Quick Setup

### 1. Deploy Security Rules

**Option A: Firebase CLI**
```bash
firebase deploy --only firestore:rules
```

**Option B: Firebase Console**
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to Firestore Database → Rules
4. Copy contents from `firestore.rules` file
5. Click "Publish"

### 2. Initialize User Roles

For each admin user, create a document in Firestore:

**Collection:** `users`  
**Document ID:** User's Firebase Auth UID  
**Fields:**
```javascript
{
  role: "admin",
  email: "admin@example.com",
  displayName: "Admin User",
  createdAt: Timestamp
}
```

**For students:**
```javascript
{
  role: "student",
  email: "student@example.com",
  displayName: "Student User",
  createdAt: Timestamp
}
```

### 3. Test Security Rules

Use the Firebase Console Rules Playground:

**Test 1: Student reads published question**
```javascript
// Should ALLOW
get /databases/(default)/documents/questions/test123
auth.uid = "student_uid"
resource.data.published = true
```

**Test 2: Student reads draft question**
```javascript
// Should DENY
get /databases/(default)/documents/questions/test123
auth.uid = "student_uid"
resource.data.published = false
```

**Test 3: Admin creates question**
```javascript
// Should ALLOW
create /databases/(default)/documents/questions/new123
auth.uid = "admin_uid"
// User document has role: "admin"
```

## Firestore Collections

### questions
**Purpose:** Store admin-created questions

**Document Structure:**
```javascript
{
  questionText: "What is a peer-to-peer network?",
  choices: [
    "A network with a central server",
    "A network where each device acts as both client and server",
    "A network limited to file sharing",
    "A network that cannot share printers"
  ],
  correctAnswer: "A network where each device acts as both client and server",
  chapterId: "chapter_1",
  missionId: "mission_1_1",
  difficulty: "easy",
  explanation: "In P2P, every computer has equal authority.",
  published: true,
  createdBy: "admin_uid_here",
  createdAt: Timestamp(2026-04-21 10:30:00),
  updatedAt: Timestamp(2026-04-21 11:45:00)
}
```

**Indexes Required:**
- `chapterId` (Ascending) + `missionId` (Ascending) + `published` (Ascending)
- `published` (Ascending) + `createdAt` (Descending)

**Create indexes:**
1. Firebase Console → Firestore → Indexes
2. Click "Create Index"
3. Add fields as listed above
4. Or wait for Firestore to prompt you when queries fail

### users
**Purpose:** Store user profiles and roles

**Document Structure:**
```javascript
{
  role: "admin",  // or "student"
  email: "user@example.com",
  displayName: "John Doe",
  photoURL: "https://...",
  createdAt: Timestamp(2026-04-21 09:00:00),
  lastLogin: Timestamp(2026-04-21 10:30:00)
}
```

**No indexes required** (simple queries only)

### missions (Optional)
**Purpose:** Store admin-created missions

**Document Structure:**
```javascript
{
  title: "Custom Mission Title",
  difficulty: "medium",
  mechanicType: "quiz",
  publishedAt: Timestamp(2026-04-21 12:00:00),
  questions: [
    {
      questionText: "...",
      optionA: "...",
      optionB: "...",
      optionC: "...",
      optionD: "...",
      correctOption: 2,
      sortOrder: 0
    }
  ]
}
```

### progress (Optional)
**Purpose:** Track user progress

**Document Structure:**
```javascript
{
  userId: "user_uid_here",
  missionId: "mission_1_1",
  completed: true,
  score: 85,
  totalQuestions: 10,
  correctAnswers: 8,
  completedAt: Timestamp(2026-04-21 14:30:00)
}
```

## Security Rules Explained

### Student Access
```javascript
// Students can only read published questions
allow read: if isAuthenticated() && resource.data.published == true;
```

**What this means:**
- Students must be logged in
- They can only see questions where `published: true`
- Draft questions (`published: false`) are hidden

### Admin Access
```javascript
// Admins have full access to all questions
allow read, write: if isAdmin();
```

**What this means:**
- Admins can read all questions (published and drafts)
- Admins can create, update, and delete questions
- Admin status is checked via `users/{uid}.role == "admin"`

### User Document Access
```javascript
// Users can read and write their own document
allow read, write: if isAuthenticated() && request.auth.uid == userId;
```

**What this means:**
- Users can only access their own user document
- Cannot read or modify other users' documents
- Admins can read all user documents

## Initial Data Setup

### Step 1: Create Admin User

1. **Sign up via app** with Google Sign-In
2. **Get the user's UID** from Firebase Console → Authentication
3. **Create user document** in Firestore:

```javascript
// Collection: users
// Document ID: [USER_UID_FROM_AUTH]
{
  role: "admin",
  email: "admin@example.com",
  displayName: "Admin User",
  createdAt: [SERVER_TIMESTAMP]
}
```

### Step 2: Test Admin Access

1. Login to app with admin account
2. Navigate to Admin Dashboard
3. Click "Add Question"
4. Fill out form and click "Publish"
5. Check Firestore Console to verify question was created

### Step 3: Create Test Student

1. Sign up with different Google account
2. Get the user's UID
3. Create user document with `role: "student"`
4. Login and verify:
   - Can see published questions in quiz
   - Cannot access Admin Dashboard
   - Cannot see draft questions

## Firestore Console Quick Actions

### View All Questions
```
Firestore → questions → [View all documents]
```

### Filter Published Questions
```
Firestore → questions → Add filter
Field: published
Operator: ==
Value: true
```

### Find Questions by Mission
```
Firestore → questions → Add filter
Field: missionId
Operator: ==
Value: mission_1_1
```

### Delete Test Questions
```
Firestore → questions → [Select document] → Delete
```

## Monitoring

### Check Security Rule Violations

1. Firebase Console → Firestore → Usage
2. Look for "Permission Denied" errors
3. Review the request details
4. Update rules if needed

### Monitor Question Creation

1. Firebase Console → Firestore → questions
2. Sort by `createdAt` descending
3. Verify new questions appear
4. Check all fields are populated correctly

### Track User Activity

1. Firebase Console → Authentication → Users
2. Check "Last sign-in" timestamps
3. Cross-reference with Firestore user documents

## Troubleshooting

### Issue: "Permission Denied" when creating question

**Cause:** User role not set or not "admin"

**Solution:**
1. Check Firestore → users → [user_uid]
2. Verify `role: "admin"` exists
3. If missing, create the field
4. Logout and login again

### Issue: Student sees draft questions

**Cause:** Security rules not deployed or incorrect

**Solution:**
1. Verify `firestore.rules` is deployed
2. Check rules in Firebase Console
3. Test with Rules Playground
4. Redeploy if needed

### Issue: Questions not loading in quiz

**Cause:** Missing indexes or network error

**Solution:**
1. Check Logcat for error messages
2. Look for index creation links in console
3. Click links to create required indexes
4. Wait 1-2 minutes for indexes to build
5. Retry loading questions

### Issue: Correct answer not working

**Cause:** String mismatch between choice and correctAnswer

**Solution:**
1. Open question document in Firestore
2. Verify `correctAnswer` exactly matches one of the `choices`
3. Check for extra spaces or case differences
4. Update document if needed

## Best Practices

### 1. Always Set User Roles
- Create user document immediately after signup
- Default to "student" role
- Manually promote to "admin" as needed

### 2. Use Published Flag
- Save as draft first (`published: false`)
- Review question before publishing
- Publish when ready (`published: true`)

### 3. Validate Data
- Ensure all required fields are present
- Verify correctAnswer matches a choice
- Check difficulty is valid ("easy", "medium", "hard")

### 4. Monitor Usage
- Check Firestore usage regularly
- Set up billing alerts
- Optimize queries to reduce reads

### 5. Backup Data
- Export Firestore data regularly
- Keep local copies of important questions
- Use version control for rules

## Cost Optimization

### Firestore Pricing (as of 2026)
- **Reads:** $0.06 per 100,000 documents
- **Writes:** $0.18 per 100,000 documents
- **Deletes:** $0.02 per 100,000 documents
- **Storage:** $0.18 per GB/month

### Tips to Reduce Costs
1. **Cache questions locally** after first load
2. **Use pagination** for large question lists
3. **Limit real-time listeners** (use get() instead)
4. **Delete test data** regularly
5. **Use local JSON** for static questions

### Free Tier Limits
- **Reads:** 50,000 per day
- **Writes:** 20,000 per day
- **Deletes:** 20,000 per day
- **Storage:** 1 GB

**Typical usage for this app:**
- 100 students × 10 questions/day = 1,000 reads/day
- 5 admins × 5 questions/day = 25 writes/day
- **Well within free tier!** ✅

## Summary

✅ Deploy security rules via CLI or Console  
✅ Create user documents with role field  
✅ Test rules with Rules Playground  
✅ Create required indexes  
✅ Monitor usage and errors  
✅ Follow best practices for data validation  

Your Firestore setup is now complete and ready for production use!
