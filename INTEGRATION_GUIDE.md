# Integration Guide — Hybrid Question System

## Quick Start

This guide shows how to integrate the hybrid question system into your existing app.

## Step 1: Update AndroidManifest.xml

Add the AddQuestionActivity declaration:

```xml
<activity
    android:name=".admin.AddQuestionActivity"
    android:theme="@style/Theme.ServerMasterNCII"
    android:parentActivityName=".admin.AdminDashboardActivity" />
```

## Step 2: Update AdminDashboardActivity

Add a button to launch AddQuestionActivity:

```java
// In AdminDashboardActivity.java
binding.btnAddQuestion.setOnClickListener(v -> {
    Intent intent = new Intent(this, AddQuestionActivity.class);
    startActivity(intent);
});
```

Add to `activity_admin_dashboard.xml`:

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_add_question"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="ADD QUESTION"
    android:textColor="#0A0A0F"
    app:backgroundTint="#00F5FF"
    app:cornerRadius="4dp" />
```

## Step 3: Update QuizActivity

Replace the old question loading with the new hybrid loader:

### Before:
```java
List<Question> questions = QuestionLoader.loadForLevel(this, levelId);
startQuiz(questions);
```

### After:
```java
// Extract chapter and mission IDs from intent
String chapterId = getIntent().getStringExtra("chapterId");
String missionId = getIntent().getStringExtra("missionId");

// Show loading
setLoading(true);

// Load questions using hybrid loader
QuestionLoader.loadForMission(this, chapterId, missionId, questions -> {
    setLoading(false);
    if (questions.isEmpty()) {
        showError("No questions available for this mission");
        finish();
    } else {
        startQuiz(questions);
    }
});
```

## Step 4: Update MainActivity / LevelAdapter

Update the Level model to include chapter and mission IDs:

```java
// In Level.java
public class Level {
    private String id;
    private String title;
    private String chapterId;    // NEW
    private String missionId;    // NEW
    
    // Add getters/setters for chapterId and missionId
}
```

Update the level click handler to pass these IDs:

```java
// In MainActivity.java or LevelAdapter.java
Intent intent = new Intent(context, QuizActivity.class);
intent.putExtra("chapterId", level.getChapterId());
intent.putExtra("missionId", level.getMissionId());
context.startActivity(intent);
```

## Step 5: Map Existing Levels to Missions

Update your level data to include chapter/mission IDs:

```java
// Example mapping
levels.add(new Level("1.1", "P2P Fundamentals", "chapter_1", "mission_1_1"));
levels.add(new Level("1.2", "Workgroup Basics", "chapter_1", "mission_1_2"));
levels.add(new Level("1.3", "Windows Firewall", "chapter_1", "mission_1_3"));
levels.add(new Level("1.4", "IP Addressing", "chapter_1", "mission_1_4"));
levels.add(new Level("2.1", "Server Setup", "chapter_2", "mission_2_1"));
levels.add(new Level("2.2", "Server Roles", "chapter_2", "mission_2_2"));
levels.add(new Level("2.3", "Post-Deployment", "chapter_2", "mission_2_3"));
```

## Step 6: Deploy Firestore Rules

1. Copy `firestore.rules` to your Firebase project root
2. Deploy using Firebase CLI:
   ```bash
   firebase deploy --only firestore:rules
   ```

Or manually in Firebase Console:
1. Go to Firestore → Rules
2. Copy contents of `firestore.rules`
3. Click "Publish"

## Step 7: Test the Integration

### Test as Admin:
1. Login with admin account
2. Go to Admin Dashboard
3. Click "Add Question"
4. Fill out form and publish
5. Verify question appears in Firestore Console

### Test as Student:
1. Login with student account
2. Select a mission
3. Start quiz
4. Verify questions from both sources appear
5. Answer questions and verify scoring works

## Optional: Update Question Count

Update the admin dashboard to show total questions from both sources:

```java
// In AdminViewModel.java
public void loadStats() {
    // Load local question count
    int localCount = countLocalQuestions(context);
    
    // Load Firestore question count
    db.collection("questions")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            int firestoreCount = querySnapshot.size();
            int totalCount = localCount + firestoreCount;
            totalQuestions.setValue(totalCount);
        });
}

private int countLocalQuestions(Context context) {
    try {
        InputStream is = context.getAssets().open("questions.json");
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String json = new String(buffer, StandardCharsets.UTF_8);
        JSONArray array = new JSONArray(json);
        return array.length();
    } catch (Exception e) {
        return 0;
    }
}
```

## Troubleshooting

### Issue: Questions not loading
**Solution:** Check Logcat for errors. Common causes:
- Firestore security rules not deployed
- User role not set in Firestore
- Network connectivity issues

### Issue: Correct answer not working
**Solution:** Verify the correctAnswer string matches the choice exactly (case-sensitive).

### Issue: App crashes on quiz start
**Solution:** Ensure chapterId and missionId are passed in the intent.

### Issue: Admin can't create questions
**Solution:** 
1. Verify user role is "admin" in Firestore users collection
2. Check Firestore security rules are deployed
3. Check Firebase Authentication is working

## Complete Example: QuizActivity Update

Here's a complete example of updating QuizActivity:

```java
public class QuizActivity extends AppCompatActivity {
    
    private String chapterId;
    private String missionId;
    private List<Question> questions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get chapter and mission IDs from intent
        chapterId = getIntent().getStringExtra("chapterId");
        missionId = getIntent().getStringExtra("missionId");
        
        if (chapterId == null || missionId == null) {
            Toast.makeText(this, "Invalid mission data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        loadQuestions();
    }
    
    private void loadQuestions() {
        setLoading(true);
        
        QuestionLoader.loadForMission(this, chapterId, missionId, loadedQuestions -> {
            setLoading(false);
            
            if (loadedQuestions == null || loadedQuestions.isEmpty()) {
                Toast.makeText(this, "No questions available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            this.questions = loadedQuestions;
            startQuiz();
        });
    }
    
    private void startQuiz() {
        // Your existing quiz logic here
        // questions list now contains merged questions from both sources
    }
}
```

## Summary

After completing these steps:

✅ Admin can create questions via AddQuestionActivity  
✅ Questions are saved to Firestore  
✅ Students see merged questions from both sources  
✅ Security rules enforce role-based access  
✅ Existing local questions continue to work  

The integration is complete and the hybrid system is fully functional!
