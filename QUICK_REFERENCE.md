# Quick Reference — Admin Question System

## 🎯 Core Files

```
app/src/main/java/com/example/servermasterncii/
├── admin/
│   ├── AdminQuestion.java          ✅ Model (10 fields)
│   └── AdminViewModel.java         ✅ ViewModel (CRUD + LiveData)
└── QuestionLoader.java             ✅ Hybrid loader (local + Firestore)

firestore.rules                     ✅ Security rules
```

## 📋 AdminQuestion Fields

```java
String id;                  // Firestore doc ID (null for new)
String questionText;        // Question text
List<String> choices;       // 4 choices
String correctAnswer;       // Correct answer string
String chapterId;           // e.g., "chapter_1"
String missionId;           // e.g., "mission_1_1"
String difficulty;          // "easy" | "medium" | "hard"
String explanation;         // Optional
boolean published;          // true = visible to students
String createdBy;           // Firebase Auth UID
```

## 🔧 AdminViewModel Methods

### Called by AddQuestionActivity
```java
// Create new question
viewModel.publishQuestion(AdminQuestion q);

// Update existing question
viewModel.updateQuestion(AdminQuestion q);

// Observe UI state
viewModel.getIsLoading().observe(this, isLoading -> ...);
viewModel.getError().observe(this, error -> ...);
viewModel.getSuccessMessage().observe(this, message -> ...);
```

### Called by QuestionManagerActivity
```java
// Load all questions
viewModel.loadQuestions();

// Load by mission
viewModel.loadQuestionsByMission("chapter_1", "mission_1_1");

// Delete question
viewModel.deleteQuestion(questionId);

// Toggle publish status
viewModel.togglePublish(questionId, true);

// Observe questions list
viewModel.getQuestions().observe(this, questions -> ...);
```

## 🎮 QuestionLoader (Student Side)

```java
// Load questions for quiz
QuestionLoader.loadForMission(context, chapterId, missionId, 
    new QuestionLoader.OnQuestionsLoadedListener() {
        @Override
        public void onLoaded(List<QuestionLoader.Question> questions) {
            // questions contains merged local + Firestore
            startQuiz(questions);
        }

        @Override
        public void onError(String errorMessage) {
            showError(errorMessage);
        }
    });
```

## 🗺️ Mission Mapping

```
mission_1_1 → SC_1.1_*
mission_1_2 → SC_1.2_*
mission_2_1 → SC_2.1_*
```

## 🔐 Security Rules Summary

```javascript
// Students
- Read: published questions only
- Write: none

// Admins
- Read: all questions (including drafts)
- Write: full CRUD access
```

## 📊 Firestore Document

```javascript
questions/{auto-id}
{
  questionText: String,
  choices: Array<String>,      // 4 items
  correctAnswer: String,
  chapterId: String,
  missionId: String,
  difficulty: String,
  explanation: String,
  published: Boolean,
  createdBy: String,
  createdAt: Timestamp,
  updatedAt: Timestamp         // Only on updates
}
```

## 🚀 Quick Deploy

```bash
# Deploy security rules
firebase deploy --only firestore:rules

# Create admin user in Firestore Console
Collection: users
Document ID: [USER_UID_FROM_AUTH]
Fields: { role: "admin", email: "admin@example.com" }
```

## ✅ Testing Checklist

**Admin:**
- [ ] Create question (published)
- [ ] Create question (draft)
- [ ] Edit question
- [ ] Delete question
- [ ] Toggle publish

**Student:**
- [ ] Load questions (sees published only)
- [ ] Verify local + Firestore merged
- [ ] Verify drafts hidden
- [ ] Verify shuffling works

## 🐛 Common Issues

**"Permission Denied"**
→ Check user role in Firestore users collection

**"Questions not loading"**
→ Check Firestore indexes (console will prompt)

**"Correct answer not working"**
→ Verify correctAnswer matches a choice exactly

**"Cannot resolve symbol"**
→ Rebuild project (Build → Rebuild Project)

## 📞 Support

See detailed documentation:
- `COMPLETE_ADMIN_QUESTION_SYSTEM.md` — Full system docs
- `INTEGRATION_GUIDE.md` — Integration steps
- `FIRESTORE_SETUP_GUIDE.md` — Firestore setup

---

**All files complete and ready to use!** 🎉
