# Server Master NC II - Complete App Context

## 📱 App Overview

**Server Master NC II** is an Android educational gamification app designed to teach Computer Systems Servicing (CSS) National Certificate II curriculum through interactive quizzes, simulators, and hands-on missions.

### Core Concept
- **Target Audience**: Students preparing for CSS NC II certification in the Philippines
- **Learning Approach**: Gamified learning with a "Saga Map" progression system
- **Content**: 21 levels across 3 chapters covering networking, server administration, and maintenance
- **Platform**: Native Android app (Java)

---

## 🏗️ Architecture Overview

### Technology Stack
- **Language**: Java
- **UI Framework**: ViewBinding, Material Design 3
- **Database**: Room (SQLite) for offline-first storage
- **Backend**: Firebase (Authentication, Firestore)
- **Animations**: Lottie
- **Architecture Pattern**: MVVM (ViewModel + LiveData)

### Key Dependencies
```gradle
// Core
implementation 'androidx.appcompat:appcompat:1.x.x'
implementation 'com.google.android.material:material:1.x.x'

// Room Database
implementation 'androidx.room:room-runtime:2.x.x'
annotationProcessor 'androidx.room:room-compiler:2.x.x'

// Firebase
implementation platform('com.google.firebase:firebase-bom:34.12.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'

// Social Auth
implementation 'com.google.android.gms:play-services-auth:21.2.0'
implementation 'com.facebook.android:facebook-login:17.0.0'

// UI
implementation 'com.airbnb.android:lottie:6.7.1'

// ViewModel + LiveData
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
```

---

## 📂 Project Structure

```
app/src/main/
├── java/com/example/servermasterncii/
│   ├── admin/                          # Admin panel features
│   │   ├── AdminDashboardActivity.java # Admin hub
│   │   ├── QuestionManagerActivity.java # CRUD for questions
│   │   ├── AddQuestionActivity.java    # Add/edit question form
│   │   ├── AdminViewModel.java         # Firestore operations
│   │   ├── QuestionAdapter.java        # RecyclerView adapter
│   │   ├── db/                         # Room DAOs for admin
│   │   │   ├── AdminMissionDao.java
│   │   │   └── AdminQuestionDao.java
│   │   └── model/                      # Admin data models
│   │       ├── AdminMission.java
│   │       └── AdminQuestion.java
│   │
│   ├── db/                             # Database layer
│   │   ├── AppDatabase.java            # Room database singleton
│   │   └── QuestionDao.java            # DAO for questions
│   │
│   ├── model/                          # Data models
│   │   ├── Question.java               # Quiz question entity
│   │   └── Level.java                  # Saga Map level
│   │
│   ├── LoginActivity.java              # Firebase auth entry point
│   ├── LoginViewModel.java             # Auth state management
│   ├── MainActivity.java               # Saga Map (level selector)
│   ├── QuizActivity.java               # Quiz gameplay
│   ├── ResultActivity.java             # Quiz results screen
│   ├── LevelAdapter.java               # RecyclerView for levels
│   │
│   ├── FirewallSimulatorActivity.java  # Interactive firewall config
│   ├── IPConfigSimulatorActivity.java  # Interactive IP addressing
│   ├── TerminalEmulatorActivity.java   # CMD simulator
│   ├── WorkgroupConfigActivity.java    # Workgroup setup
│   ├── PermissionSimulatorActivity.java
│   ├── ServerMonitorActivity.java
│   ├── DecisionTreeActivity.java
│   ├── StepSequencerActivity.java
│   │
│   ├── QuestionLoader.java             # Loads questions from Room
│   ├── RankManager.java                # User progression system
│   ├── ThemeManager.java               # Dark/light theme
│   └── SettingsActivity.java           # App settings
│
├── res/
│   ├── layout/                         # XML layouts
│   ├── drawable/                       # Icons, backgrounds
│   ├── values/                         # Strings, colors, themes
│   └── ...
│
└── assets/
    ├── questions.json                  # Question bank (1000+ lines)
    ├── chapters.json                   # Chapter metadata
    └── anim_*.json                     # Lottie animations
```

---

## 🎮 Core Features

### 1. Authentication System (LoginActivity)

**Supported Methods:**
- Google Sign-In (Firebase Auth)
- Facebook Login (Facebook SDK)
- Guest Mode (Anonymous Firebase Auth)

**Role-Based Routing:**
```
User signs in
    ↓
Check Firestore: users/{uid}
    ↓
role == "admin" → AdminDashboardActivity
role == "student" → MainActivity
isAnonymous → MainActivity (guest)
```

**Firestore User Document:**
```json
{
  "email": "user@example.com",
  "displayName": "John Doe",
  "role": "student",  // or "admin"
  "createdAt": Timestamp
}
```

### 2. Saga Map (MainActivity)

**Purpose**: Main hub showing all 21 levels across 3 chapters

**Features:**
- 3-tab bottom navigation (Chapter 1, 2, 3)
- 2-column grid of mission cards
- Progress tracking (0-100% per level)
- Sequential unlocking (must complete previous level with ≥70%)
- Hall of Fame header (total skill points, overall mastery %)
- Lottie animations for each level

**Level Structure:**
```
Chapter 1: User Access (Levels 1.1 - 1.5)
  1.1 - P2P Networks
  1.2 - Workgroups
  1.3 - Firewalls (Interactive UI)
  1.4 - IP Addressing (Interactive UI)
  1.5 - CMD Basics (Interactive CMD)

Chapter 2: Server Setup (Levels 2.1 - 2.12)
  2.1 - Server Roles
  2.2 - Windows Server
  2.3 - Active Directory
  2.4 - DNS Server
  2.5 - DHCP Server
  2.6 - File Server
  2.7 - Print Server
  2.8 - Group Policy
  2.9 - Backup Server
  2.10 - Remote Desktop (Interactive UI)
  2.11 - Server Security
  2.12 - Monitoring

Chapter 3: Maintenance (Levels 3.1 - 3.4)
  3.1 - Troubleshooting (Interactive CMD)
  3.2 - Updates & Patches
  3.3 - Documentation
  3.4 - Decommission
```

**Persistence:**
- Uses SharedPreferences (`server_master_prefs`)
- Keys: `score_{levelId}`, `total_{levelId}`, `unlocked_{levelId}`

### 3. Quiz System (QuizActivity)

**Question Types:**
1. **static** - Standard multiple choice (4 options)
2. **true_false** - True/False questions (2 options)
3. **interactive_cmd** - Launches TerminalEmulatorActivity
4. **interactive_ui** - Launches simulator (Firewall, IP Config, Workgroup)

**GameRouter Logic:**
```java
switch (question.type) {
    case "interactive_cmd":
        launch TerminalEmulatorActivity
        break;
    case "interactive_ui":
        if (category == "Firewall Config")
            launch FirewallSimulatorActivity
        else if (category == "IP Config")
            launch IPConfigSimulatorActivity
        else if (category == "Workgroup Config")
            launch WorkgroupConfigActivity
        break;
    default:
        display question in QuizActivity
}
```

**Features:**
- 30-second timer per question
- Integrity system (100 → 0, -20 per mistake)
- Skip button (costs integrity)
- Auto-advance after answer
- Missed questions tracking for review

**Completion Flow:**
```
Quiz ends
    ↓
Score >= 70% AND level has simulator gate?
    ├─ YES → Launch simulator (1.3, 1.4, 1.5)
    │         ↓
    │       Simulator result → ResultActivity
    └─ NO → ResultActivity directly
```

### 4. Interactive Simulators

#### FirewallSimulatorActivity
- Simulates Windows Firewall "Allow an app" dialog
- Task: Enable "File and Printer Sharing" for Private + Public
- Validation: Both checkboxes must be checked
- Success: Lottie shield animation + system chime

#### IPConfigSimulatorActivity
- Simulates IPv4 Properties dialog
- Task: Assign first usable IP on 192.168.1.0/24 network
- Answer: 192.168.1.1 with subnet mask 255.255.255.0
- Validation: Checks IP format and correctness

#### TerminalEmulatorActivity
- Simulates Windows Command Prompt
- Supports commands: ipconfig, ping, dir, cd, cls, help, exit
- Tracks command history
- Success: Complete required commands

### 5. Admin Panel

**Access Control:**
- Only users with `role: "admin"` in Firestore can access
- Enforced by Firestore security rules

**AdminDashboardActivity:**
- Statistics cards (total users, total questions)
- Navigation to Question Manager
- Sign out functionality

**QuestionManagerActivity:**
- Lists all questions from Firestore
- Edit/delete buttons per question
- FAB to add new questions
- Auto-refresh on resume

**AddQuestionActivity:**
- Form to add/edit questions
- Fields: question text, 4 choices, correct answer, category
- Validation before save
- Supports 21 categories (all level IDs)

**AdminViewModel:**
- Handles all Firestore CRUD operations
- LiveData for reactive UI updates
- Error handling and loading states

---

## 💾 Data Layer

### Room Database (AppDatabase)

**Entities:**
1. **Question** - Quiz questions (loaded from questions.json on first run)
2. **AdminMission** - Admin-created missions
3. **AdminQuestion** - Questions within admin missions

**Key Methods:**
```java
// AppDatabase.java
public static List<Question> parseQuestionsFromJson(Context context)
public static String deriveLearningOutcome(String stringId)

// QuestionDao.java
@Query("SELECT * FROM questions WHERE learningOutcome = :lo")
List<Question> getByLearningOutcome(String lo);
```

### Firestore Collections

#### users
```
users/{uid}
  ├─ email: string
  ├─ displayName: string
  ├─ role: "admin" | "student"
  └─ createdAt: Timestamp
```

#### questions (Admin-managed)
```
questions/{questionId}
  ├─ questionText: string
  ├─ choices: array[4] of strings
  ├─ correctAnswer: string
  ├─ category: string (e.g., "1.1", "2.3")
  ├─ createdAt: Timestamp
  └─ updatedAt: Timestamp
```

### questions.json Structure

```json
[
  {
    "id": "SC_1.1_Q1",
    "type": "static",
    "category": "P2P Fundamentals",
    "question": "Which of the following best describes a peer-to-peer (P2P) network?",
    "options": [
      "A network where a central server manages all resources",
      "A network where each device acts as both client and server",
      "A network limited only to internet file sharing",
      "A network that cannot share printers or files"
    ],
    "answer_index": 1,
    "explanation": "In P2P, there is no boss. Every computer has equal authority..."
  }
]
```

**ID Format:** `SC_{chapter}.{section}_Q{number}`
- Example: `SC_1.1_Q1` → Learning Outcome "1.1"
- Parsed by `deriveLearningOutcome()` to map to Saga Map levels

---

## 🔐 Security (Firestore Rules)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if isOwner(userId) && request.resource.data.role == 'student';
      allow update: if isOwner(userId) && request.resource.data.role == resource.data.role;
      allow update: if isAdmin();
      allow delete: if isAdmin();
    }
    
    // Questions collection (admin-only write)
    match /questions/{questionId} {
      allow read: if isAuthenticated();
      allow create, update, delete: if isAdmin();
    }
  }
}
```

---

## 🎨 UI/UX Design

### Theme System (ThemeManager)
- Dark mode (default)
- Light mode
- Cyber-terminal aesthetic
- Neon green accents (#39FF7F)

### Color Palette
```xml
<!-- Cyber Theme -->
<color name="cyber_neon_green">#39FF7F</color>
<color name="cyber_neon_green_bright">#00FF7F</color>
<color name="cyber_dark_bg">#0A0E0D</color>
<color name="cyber_card_bg">#0F2A1A</color>

<!-- Quiz Colors -->
<color name="quiz_correct">#4CAF50</color>
<color name="quiz_wrong">#FF3B3B</color>
```

### Key UI Components
- **Material Cards** - Elevated cards with neon borders
- **Lottie Animations** - 20+ custom animations for levels
- **Progress Bars** - Linear indicators for level progress
- **Bottom Navigation** - Chapter switcher
- **RecyclerView** - Grid layout for levels, list for questions

---

## 🔄 User Flows

### Student Flow
```
1. Login (Google/Facebook/Guest)
   ↓
2. MainActivity (Saga Map)
   ↓
3. Tap unlocked level
   ↓
4. QuizActivity
   ├─ Answer static questions
   ├─ Complete interactive simulators
   └─ View integrity bar
   ↓
5. ResultActivity
   ├─ Score display
   ├─ Missed questions review
   └─ Rank badge
   ↓
6. Return to MainActivity (progress updated)
```

### Admin Flow
```
1. Login with admin account
   ↓
2. AdminDashboardActivity
   ↓
3. Tap "Question Manager"
   ↓
4. QuestionManagerActivity
   ├─ View all questions
   ├─ Edit question → AddQuestionActivity
   ├─ Delete question (with confirmation)
   └─ Add new question → AddQuestionActivity
   ↓
5. Changes sync to Firestore immediately
```

---

## 📊 Key Algorithms

### Level Unlocking Logic
```java
private boolean isLevelUnlocked(String levelId) {
    if ("1.1".equals(levelId)) return true;  // First level always unlocked
    
    if (prefs.getBoolean(KEY_UNLOCKED + levelId, false)) return true;
    
    String previousLevelId = getPreviousLevelId(levelId);
    if (previousLevelId != null) {
        int prevScore = prefs.getInt(KEY_SCORE + previousLevelId, 0);
        int prevTotal = prefs.getInt(KEY_TOTAL + previousLevelId, 0);
        if (prevTotal > 0) {
            double pct = prevScore / (double) prevTotal;
            if (pct >= 0.70) {  // 70% threshold
                prefs.edit().putBoolean(KEY_UNLOCKED + levelId, true).apply();
                return true;
            }
        }
    }
    return false;
}
```

### Integrity System
```java
// Initial: 100
// Wrong answer: -20
// Skip: -20
// Time out: -20

private void updateIntegrityBar() {
    if (currentIntegrity >= 80) {
        status = "OPTIMAL";   // Green
    } else if (currentIntegrity >= 50) {
        status = "STABLE";    // Green
    } else if (currentIntegrity >= 20) {
        status = "DEGRADED";  // Amber
    } else {
        status = "CRITICAL";  // Red
    }
}
```

### Question Shuffling
```java
List<Question> staticQs = all.stream()
    .filter(Question::isStaticQuestion)
    .collect(Collectors.toList());
List<Question> interactiveQs = all.stream()
    .filter(Question::isInteractive)
    .collect(Collectors.toList());

Collections.shuffle(staticQs);  // Randomize static questions

List<Question> combined = new ArrayList<>();
combined.addAll(staticQs);
combined.addAll(interactiveQs);  // Interactive always at end
```

---

## 🐛 Common Issues & Solutions

### Issue: Admin always redirected to student dashboard
**Solution:** Check Firestore document has `role: "admin"` (case-sensitive)

### Issue: Questions not loading
**Solution:** 
1. Verify questions.json is in assets/
2. Check Room database initialization
3. Clear app data and reinstall

### Issue: Facebook login fails
**Solution:**
1. Verify Facebook App ID in strings.xml
2. Add key hash to Meta Developer Console
3. Check AndroidManifest.xml has Facebook activities

### Issue: Simulator not awarding credit
**Solution:**
1. Verify `EXTRA_TASK_COMPLETED` is set to true
2. Check ActivityResultLauncher is registered before onCreate
3. Ensure setResult(RESULT_OK) is called before finish()

---

## 🚀 Build & Run

### Prerequisites
1. Android Studio Arctic Fox or later
2. JDK 17
3. Android SDK 34
4. Firebase project configured
5. google-services.json in app/

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

### Firebase Setup
1. Create Firebase project at console.firebase.google.com
2. Add Android app with package name: `com.example.servermasterncii`
3. Download google-services.json → app/
4. Enable Authentication (Google, Facebook, Anonymous)
5. Enable Firestore Database
6. Deploy security rules from firestore.rules

---

## 📈 Future Enhancements

### Planned Features
1. **User Manager** - Admin can view/manage all users
2. **Analytics Dashboard** - Track question performance, user engagement
3. **Bulk Import** - Import questions from CSV/JSON
4. **Image Support** - Add images to questions
5. **Leaderboard** - Global ranking system
6. **Achievements** - Badges for milestones
7. **Offline Mode** - Full offline quiz support
8. **Multi-language** - Support for Filipino/Tagalog

### Technical Debt
1. Migrate to Kotlin
2. Implement Dependency Injection (Hilt)
3. Add unit tests (JUnit, Mockito)
4. Add UI tests (Espresso)
5. Implement Paging 3 for question lists
6. Add Crashlytics for error tracking

---

## 📚 Learning Resources

### CSS NC II Curriculum
- **LO1**: Install and configure computer systems (Chapters 1-2)
- **LO2**: Set up computer networks (Chapter 1)
- **LO3**: Set up computer servers (Chapter 2)
- **LO4**: Maintain and repair computer systems and networks (Chapter 3)

### Key Concepts Covered
- Peer-to-Peer vs Client-Server
- Workgroups vs Domains
- Windows Firewall configuration
- IPv4 addressing and subnetting
- Active Directory basics
- DNS and DHCP services
- File and Print servers
- Group Policy management
- Server monitoring and troubleshooting

---

## 🤝 Contributing Guidelines

### Code Style
- Follow Android Java conventions
- Use ViewBinding (no findViewById)
- Prefer composition over inheritance
- Write self-documenting code with clear variable names
- Add JavaDoc comments for public methods

### Git Workflow
1. Create feature branch from main
2. Make changes with descriptive commits
3. Test thoroughly on physical device
4. Submit pull request with description

### Testing Checklist
- [ ] App builds without errors
- [ ] All authentication methods work
- [ ] Quiz gameplay is smooth
- [ ] Simulators validate correctly
- [ ] Admin panel CRUD operations work
- [ ] Progress persists across sessions
- [ ] No crashes on rotation
- [ ] Works on Android 7.0+ (API 24+)

---

## 📞 Support & Contact

### Documentation Files
- `ADMIN_SETUP_GUIDE.md` - Admin panel setup instructions
- `ADMIN_IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- `ADMIN_ROLE_BASED_AUTH_COMPLETE.md` - Authentication flow documentation

### Debugging
```bash
# View logs
adb logcat | grep "AUTH_ROLE\|QuizActivity\|GameRouter"

# Clear app data
adb shell pm clear com.example.servermasterncii

# Check Firestore rules
firebase deploy --only firestore:rules
```

---

## 📄 License

This project is an educational tool for CSS NC II certification preparation.

---

**Last Updated**: April 2026  
**Version**: 1.0  
**Minimum Android Version**: 7.0 (API 24)  
**Target Android Version**: 14 (API 34)
