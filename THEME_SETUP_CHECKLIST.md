# ✅ Theme System Setup Checklist

## Pre-Build Verification

Before building in Android Studio, verify these files exist:

### **New Java Files:**
- [ ] `app/src/main/java/com/example/servermasterncii/ThemeManager.java`
- [ ] `app/src/main/java/com/example/servermasterncii/SettingsActivity.java`

### **New Layout Files:**
- [ ] `app/src/main/res/layout/activity_settings.xml`

### **New Color Files:**
- [ ] `app/src/main/res/values/colors_terminal.xml`
- [ ] `app/src/main/res/values/colors_light.xml`

### **New Drawable Files:**
- [ ] `app/src/main/res/drawable/ic_settings.xml`
- [ ] `app/src/main/res/drawable/ic_arrow_back.xml`

### **Modified Files:**
- [ ] `app/src/main/res/values/themes.xml` — Contains 3 theme styles
- [ ] `app/src/main/res/layout/activity_main.xml` — Has Settings button
- [ ] `app/src/main/AndroidManifest.xml` — SettingsActivity registered

---

## Build Steps

1. **Open Android Studio**
2. **Sync Gradle** (File → Sync Project with Gradle Files)
3. **Clean Project** (Build → Clean Project)
4. **Rebuild Project** (Build → Rebuild Project)
5. **Run on Device/Emulator**

---

## Testing Steps

### **1. Initial Launch**
- [ ] App opens with Cyber Dark theme (neon green on black)
- [ ] Settings gear icon visible in top-right corner

### **2. Settings Screen**
- [ ] Tap Settings icon → SettingsActivity opens
- [ ] See 3 theme cards with color previews
- [ ] Cyber Dark card shows "● ACTIVE" badge
- [ ] Cyber Dark card has green stroke (3dp width)

### **3. Terminal Green Theme**
- [ ] Tap Terminal Green card
- [ ] App recreates with new theme
- [ ] Background is dark olive (#0D1F00)
- [ ] Accent is bright lime (#7FFF00)
- [ ] All text uses monospace font
- [ ] Tap Settings → Terminal Green shows as active

### **4. Light Grid Theme**
- [ ] Tap Light Grid card
- [ ] App recreates with new theme
- [ ] Background is white/light gray (#F5F5F5)
- [ ] Accent is cyan (#00BCD4)
- [ ] Text is dark navy (#0A0F2A)
- [ ] Status bar icons are dark (light status bar)
- [ ] Tap Settings → Light Grid shows as active

### **5. Theme Persistence**
- [ ] Select Terminal Green
- [ ] Close app completely
- [ ] Reopen app
- [ ] App opens with Terminal Green theme (not Cyber Dark)

### **6. Navigation Test**
- [ ] Select Light Grid theme
- [ ] Navigate to QuizActivity (tap a level)
- [ ] Quiz screen uses Light Grid theme
- [ ] Navigate back → MainActivity still uses Light Grid
- [ ] Open Settings → Light Grid still active

### **7. All Activities Test**
Test each activity with all 3 themes:
- [ ] MainActivity
- [ ] LoginActivity
- [ ] QuizActivity
- [ ] ResultActivity
- [ ] WorkgroupConfigActivity
- [ ] FirewallSimulatorActivity
- [ ] IPConfigSimulatorActivity
- [ ] TerminalEmulatorActivity
- [ ] StepSequencerActivity
- [ ] PermissionSimulatorActivity
- [ ] ServerMonitorActivity
- [ ] DecisionTreeActivity
- [ ] AdminDashboardActivity
- [ ] AdminMissionEditorActivity

---

## Common Issues & Fixes

### **Issue: Theme not applying**
**Fix:** Check that `ThemeManager.applyTheme(this)` is called **before** `super.onCreate()` in the activity

### **Issue: Settings button not visible**
**Fix:** 
1. Check `activity_main.xml` has `btnSettings` view
2. Verify `ic_settings.xml` exists
3. Rebuild project

### **Issue: App crashes on theme change**
**Fix:**
1. Check Logcat for error
2. Verify all color resources exist
3. Check theme parent is correct in `themes.xml`

### **Issue: Theme not persisting**
**Fix:**
1. Verify SharedPreferences name is `"server_master_prefs"`
2. Check key is `"selected_theme"`
3. Ensure `ThemeManager.setSelectedTheme()` is called before `recreate()`

### **Issue: Colors not changing**
**Fix:**
1. Use theme attributes (`?colorPrimary`) not hardcoded colors
2. Rebuild project (Clean → Rebuild)
3. Check theme resource IDs match in ThemeManager

---

## Verification Commands

### **Check if files exist:**
```bash
# Java files
ls app/src/main/java/com/example/servermasterncii/ThemeManager.java
ls app/src/main/java/com/example/servermasterncii/SettingsActivity.java

# Layout files
ls app/src/main/res/layout/activity_settings.xml

# Color files
ls app/src/main/res/values/colors_terminal.xml
ls app/src/main/res/values/colors_light.xml

# Drawable files
ls app/src/main/res/drawable/ic_settings.xml
ls app/src/main/res/drawable/ic_arrow_back.xml
```

### **Search for ThemeManager calls:**
```bash
grep -r "ThemeManager.applyTheme" app/src/main/java/
```

Should show 14 results (one for each activity).

---

## Expected Behavior Summary

| Action | Expected Result |
|--------|----------------|
| Open app | Cyber Dark theme (or last selected) |
| Tap Settings | SettingsActivity opens |
| Tap theme card | App recreates with new theme |
| Navigate to other activity | Theme persists |
| Close and reopen app | Last theme is remembered |
| Change theme in Settings | Active badge moves to new theme |

---

## Success Criteria

✅ **All 3 themes work correctly**  
✅ **Theme persists across activities**  
✅ **Theme persists after app restart**  
✅ **Settings button visible and functional**  
✅ **Active theme shows badge and stroke**  
✅ **No crashes or errors**  
✅ **All 14 activities support themes**

---

## Next Steps After Testing

1. **Test on multiple devices** (different screen sizes)
2. **Test with different Android versions** (API 24+)
3. **Consider adding more themes** (see THEME_SYSTEM_GUIDE.md)
4. **Add theme preview in onboarding** (optional)
5. **Add theme quick-switch widget** (optional)

---

**Ready to build!** 🚀

Open Android Studio, sync Gradle, and run the app to test the theme system.
