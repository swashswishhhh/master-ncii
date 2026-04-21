# 🚀 Quick Start Guide - Admin Features

Get your admin panel up and running in 5 minutes!

## ⚡ Prerequisites

- ✅ Android Studio project is open
- ✅ Firebase project is configured
- ✅ `google-services.json` is in place
- ✅ App builds successfully

## 📋 5-Minute Setup

### Step 1: Deploy Security Rules (2 minutes)

**Option A: Firebase Console (Easiest)**
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database** → **Rules** tab
4. Copy everything from `firestore.rules` file in your project
5. Paste into the rules editor
6. Click **Publish**

**Option B: Firebase CLI**
```bash
firebase deploy --only firestore:rules
```

### Step 2: Build and Run App (1 minute)

```bash
# In Android Studio
1. Click "Sync Project with Gradle Files"
2. Click "Run" (Shift+F10)
3. Wait for app to install on device/emulator
```

### Step 3: Create First Admin User (2 minutes)

1. **Sign in to the app** with your Google account
2. **Open Firebase Console** → **Firestore Database**
3. **Find your user**:
   - Collection: `users`
   - Document ID: Your UID (long string)
4. **Edit the document**:
   - Click the document
   - Find the `role` field
   - Change value to: `admin`
   - Click **Update**

**Example:**
```
users/ABC123XYZ456
  ├─ email: "your.email@gmail.com"
  ├─ displayName: "Your Name"
  ├─ role: "admin"  ← Change this!
  └─ createdAt: April 20, 2026
```

### Step 4: Test Admin Access (30 seconds)

1. **Sign out** from the app
2. **Sign in** again with the same account
3. You should see **Admin Dashboard** instead of the student view
4. Tap **"Total Questions"** card to open Question Manager

## 🎉 You're Done!

You now have full admin access. Try adding your first question:

1. In Question Manager, tap the **+** button
2. Fill in the form:
   - Question: "What is a server?"
   - Choice 1: "A computer that provides services"
   - Choice 2: "A type of software"
   - Choice 3: "A network cable"
   - Choice 4: "A monitor"
   - Correct Answer: Select "Choice 1"
   - Category: Select "1.1"
3. Tap **"Save Question"**

## 🔍 Verify It Works

### Check Admin Dashboard
- ✅ Shows "Welcome, [Your Name]"
- ✅ Displays total users count
- ✅ Displays total questions count
- ✅ Has "Sign Out" button

### Check Question Manager
- ✅ Lists all questions
- ✅ Shows question text, category, and answer
- ✅ Has edit and delete buttons
- ✅ Has + FAB button

### Check Security
1. Sign out
2. Sign in with a **different** Google account (non-admin)
3. Should go to **MainActivity** (student view)
4. Should **NOT** see admin features

## 🐛 Troubleshooting

### "Permission denied" error
**Problem**: Security rules not deployed or role not set

**Solution**:
```bash
# Check Firestore rules are deployed
firebase deploy --only firestore:rules

# Verify role in Firebase Console
users/{your-uid}/role = "admin"
```

### Still redirected to student dashboard
**Problem**: Role not updated or cached

**Solution**:
1. Force stop the app
2. Clear app data (Settings → Apps → Server Master → Clear Data)
3. Sign in again

### Questions not loading
**Problem**: Internet connection or Firestore rules

**Solution**:
1. Check device has internet
2. Check Logcat: `adb logcat | grep AdminViewModel`
3. Verify Firestore rules allow read access

## 📱 Quick Commands

### View Logs
```bash
# Filter by admin-related logs
adb logcat | grep -E "AUTH_ROLE|AdminViewModel|AdminDashboard"
```

### Clear App Data
```bash
adb shell pm clear com.example.servermasterncii
```

### Reinstall App
```bash
./gradlew installDebug
```

## 📚 Next Steps

Once everything works:

1. **Read Full Documentation**
   - `ADMIN_SETUP_GUIDE.md` - Complete setup guide
   - `ROLE_CHECK_IMPLEMENTATION.md` - Technical details
   - `ADMIN_IMPLEMENTATION_SUMMARY.md` - Overview

2. **Add More Questions**
   - Use Question Manager to populate your database
   - Organize by category (1.1, 1.2, etc.)

3. **Test Student Experience**
   - Sign in with non-admin account
   - Take quizzes with your new questions

4. **Customize**
   - Modify layouts in `res/layout/`
   - Adjust colors and themes
   - Add more features

## ✅ Success Checklist

- [ ] Firestore rules deployed
- [ ] Admin role assigned to your account
- [ ] App builds without errors
- [ ] Admin dashboard loads
- [ ] Can add a test question
- [ ] Can edit the test question
- [ ] Can delete the test question
- [ ] Non-admin account goes to student view

## 🎯 Common First-Time Issues

### Issue 1: "AdminDashboardActivity not found"
**Cause**: Gradle not synced

**Fix**: Click "Sync Project with Gradle Files" in Android Studio

### Issue 2: Layout files not found
**Cause**: Resources not generated

**Fix**: Build → Clean Project → Rebuild Project

### Issue 3: Firebase connection error
**Cause**: `google-services.json` not configured

**Fix**: Download latest from Firebase Console → Project Settings

## 💡 Pro Tips

1. **Use Logcat Filter**: Create a filter for `AUTH_ROLE` to see role detection
2. **Test on Real Device**: Emulators sometimes have Firebase issues
3. **Keep Firebase Console Open**: Monitor Firestore changes in real-time
4. **Use Multiple Accounts**: Test both admin and student flows

## 📞 Need Help?

1. Check the error in Logcat
2. Review `ADMIN_SETUP_GUIDE.md` for detailed troubleshooting
3. Verify all files are created (see `ADMIN_IMPLEMENTATION_SUMMARY.md`)
4. Check Firebase Console for Firestore errors

## 🎓 What You Just Built

- ✅ Role-based authentication system
- ✅ Admin dashboard with statistics
- ✅ Question management system (CRUD)
- ✅ Secure Firestore rules
- ✅ Automatic user role assignment

**Total Time**: ~5 minutes  
**Difficulty**: Easy  
**Result**: Production-ready admin panel

---

**Ready to go?** Follow the 4 steps above and you'll be managing questions in no time! 🚀
