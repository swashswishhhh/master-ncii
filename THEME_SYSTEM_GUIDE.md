# 🎨 Server Master NC II — Theme System Guide

## Overview

Your app now has a complete 3-theme system with a Theme Selector screen:

1. **Cyber Dark** (default) — Neon green (#39FF7F) on deep black (#0A0F0A)
2. **Terminal Green** — Bright lime (#7FFF00) on dark olive (#0D1F00) with monospace fonts
3. **Light Grid** — Cyan (#00BCD4) on white (#F5F5F5)

---

## 📁 Files Created/Modified

### **New Files:**

1. **`app/src/main/java/com/example/servermasterncii/ThemeManager.java`**
   - Singleton class for managing theme selection
   - Persists theme to SharedPreferences
   - Provides `applyTheme(Activity)` method

2. **`app/src/main/java/com/example/servermasterncii/SettingsActivity.java`**
   - Theme selector screen with 3 preview cards
   - Shows active theme with colored stroke and badge
   - Recreates activity on theme change

3. **`app/src/main/res/layout/activity_settings.xml`**
   - Material3 layout with theme preview cards
   - Color swatches showing each theme's palette
   - Responsive card design with descriptions

4. **`app/src/main/res/values/colors_terminal.xml`**
   - Terminal Green theme color palette

5. **`app/src/main/res/values/colors_light.xml`**
   - Light Grid theme color palette

6. **`app/src/main/res/drawable/ic_settings.xml`**
   - Settings gear icon (Material Design)

7. **`app/src/main/res/drawable/ic_arrow_back.xml`**
   - Back arrow icon for toolbar

### **Modified Files:**

1. **`app/src/main/res/values/themes.xml`**
   - Added 3 complete theme styles:
     - `Theme.ServerMasterNCII.CyberDark`
     - `Theme.ServerMasterNCII.TerminalGreen`
     - `Theme.ServerMasterNCII.LightGrid`

2. **`app/src/main/res/layout/activity_main.xml`**
   - Added Settings button to header

3. **`app/src/main/AndroidManifest.xml`**
   - Registered SettingsActivity

4. **All Activity files** (14 activities):
   - Added `ThemeManager.applyTheme(this);` before `super.onCreate()`
   - Activities updated:
     - MainActivity
     - LoginActivity
     - QuizActivity
     - ResultActivity
     - WorkgroupConfigActivity
     - FirewallSimulatorActivity
     - IPConfigSimulatorActivity
     - TerminalEmulatorActivity
     - StepSequencerActivity
     - PermissionSimulatorActivity
     - ServerMonitorActivity
     - DecisionTreeActivity
     - AdminDashboardActivity
     - AdminMissionEditorActivity
     - SettingsActivity

---

## 🚀 How It Works

### **1. Theme Application Flow**

```
User taps theme card in SettingsActivity
    ↓
ThemeManager.setSelectedTheme() saves to SharedPreferences
    ↓
Activity.recreate() restarts the activity
    ↓
onCreate() calls ThemeManager.applyTheme(this) BEFORE super.onCreate()
    ↓
setTheme() applies the selected theme resource
    ↓
All views inherit the new theme colors
```

### **2. SharedPreferences Storage**

- **Preferences Name:** `server_master_prefs` (same as existing app prefs)
- **Key:** `selected_theme`
- **Values:** `"cyber"`, `"terminal"`, or `"light"`
- **Default:** `"cyber"`

### **3. Theme Resource Mapping**

| Theme ID | Resource ID | Description |
|----------|-------------|-------------|
| `cyber` | `R.style.Theme_ServerMasterNCII_CyberDark` | Neon green on black |
| `terminal` | `R.style.Theme_ServerMasterNCII_TerminalGreen` | Lime on olive + monospace |
| `light` | `R.style.Theme_ServerMasterNCII_LightGrid` | Cyan on white |

---

## 🎨 Theme Color Palettes

### **Cyber Dark (Default)**
```xml
Background:     #0A0F0A (cyber_bg_deepest)
Surface:        #1A1A2E (cyber_bg_surface)
Card:           #16213E (cyber_bg_card)
Primary:        #39FF7F (cyber_neon_green)
Text Primary:   #EAEAEA (cyber_text_primary)
Text Secondary: #667788 (cyber_text_secondary)
```

### **Terminal Green**
```xml
Background:     #0D1F00 (terminal_bg_deepest)
Surface:        #152D00 (terminal_bg_surface)
Card:           #1A3600 (terminal_bg_card)
Primary:        #7FFF00 (terminal_accent)
Text Primary:   #D0FFB0 (terminal_text_primary)
Text Secondary: #88AA66 (terminal_text_secondary)
Font:           monospace (system-wide)
```

### **Light Grid**
```xml
Background:     #F5F5F5 (light_bg_deepest)
Surface:        #FAFAFA (light_bg_surface)
Card:           #FFFFFF (light_bg_card)
Primary:        #00BCD4 (light_accent)
Text Primary:   #0A0F2A (light_text_primary)
Text Secondary: #5A6B8C (light_text_secondary)
```

---

## 📝 Usage Instructions

### **For Users:**

1. Open the app
2. Tap the **Settings** gear icon in the top-right corner
3. Tap any theme card to apply it
4. The app will restart with the new theme

### **For Developers:**

#### **Adding ThemeManager to a New Activity:**

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    // Apply theme BEFORE super.onCreate()
    ThemeManager.applyTheme(this);
    super.onCreate(savedInstanceState);
    
    // Rest of your onCreate code...
}
```

**⚠️ CRITICAL:** `ThemeManager.applyTheme(this)` **MUST** be called **BEFORE** `super.onCreate()`. Otherwise, the theme won't be applied correctly.

#### **Getting the Current Theme:**

```java
ThemeManager themeManager = ThemeManager.getInstance();
String currentTheme = themeManager.getSelectedTheme(context);
// Returns: "cyber", "terminal", or "light"
```

#### **Programmatically Changing Theme:**

```java
ThemeManager themeManager = ThemeManager.getInstance();
themeManager.setSelectedTheme(context, ThemeManager.THEME_TERMINAL);
recreate(); // Restart activity to apply
```

#### **Getting Theme Display Info:**

```java
String displayName = ThemeManager.getThemeDisplayName("cyber");
// Returns: "Cyber Dark"

String description = ThemeManager.getThemeDescription("terminal");
// Returns: "Bright lime on dark olive — retro terminal vibes with monospace"
```

---

## 🔧 Technical Details

### **Material3 Theme Attributes Used:**

- `colorPrimary` — Primary brand color (accent)
- `colorOnPrimary` — Text/icons on primary color
- `colorPrimaryContainer` — Dimmed primary for backgrounds
- `colorOnPrimaryContainer` — Text on primary container
- `android:colorBackground` — Main background color
- `colorSurface` — Surface color (cards, sheets)
- `colorOnSurface` — Text on surface
- `colorSurfaceVariant` — Variant surface color
- `colorOnSurfaceVariant` — Text on surface variant
- `android:statusBarColor` — Status bar color
- `android:navigationBarColor` — Navigation bar color
- `android:windowLightStatusBar` — Light/dark status bar icons
- `android:windowLightNavigationBar` — Light/dark nav bar icons

### **Terminal Green Special Feature:**

The Terminal Green theme applies monospace fonts system-wide:

```xml
<item name="fontFamily">monospace</item>
<item name="android:fontFamily">monospace</item>
```

This gives the entire app a retro terminal aesthetic when this theme is active.

---

## 🎯 MainActivity Integration

The Settings button was added to the MainActivity header:

```xml
<!-- Settings Button -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnSettings"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="48dp"
    android:layout_height="48dp"
    app:icon="@drawable/ic_settings"
    app:iconTint="@color/cyber_neon_green_bright" />
```

And wired in Java:

```java
private void setupSettingsButton() {
    binding.btnSettings.setOnClickListener(v -> {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    });
}
```

---

## 🐛 Troubleshooting

### **Theme not applying:**
- Ensure `ThemeManager.applyTheme(this)` is called **before** `super.onCreate()`
- Check that the activity is registered in AndroidManifest.xml
- Verify SharedPreferences key is correct: `"selected_theme"`

### **Colors not changing:**
- Make sure you're using theme attributes (`?colorPrimary`) not hardcoded colors
- Check that the theme resource IDs match in ThemeManager
- Rebuild the project (Build → Clean Project → Rebuild Project)

### **Settings button not visible:**
- Check that `activity_main.xml` has the `btnSettings` view
- Verify the icon drawable exists: `ic_settings.xml`
- Check ViewBinding is enabled in build.gradle

### **App crashes on theme change:**
- Check Logcat for errors
- Verify all color resources exist in colors.xml files
- Ensure theme parent is correct (Material3.DayNight.NoActionBar or Material3.Light.NoActionBar)

---

## 📊 Testing Checklist

- [ ] Open app → Default theme is Cyber Dark
- [ ] Tap Settings button → SettingsActivity opens
- [ ] Cyber Dark card shows "● ACTIVE" badge and colored stroke
- [ ] Tap Terminal Green → App recreates with lime/olive colors and monospace fonts
- [ ] Tap Settings again → Terminal Green card is now active
- [ ] Tap Light Grid → App recreates with cyan/white colors
- [ ] Navigate to different activities → Theme persists
- [ ] Close and reopen app → Last selected theme is remembered
- [ ] Test all 14 activities with each theme
- [ ] Check status bar and navigation bar colors match theme

---

## 🎨 Customization Guide

### **Adding a New Theme:**

1. **Create color palette** in `colors_mytheme.xml`:
```xml
<color name="mytheme_bg_deepest">#RRGGBB</color>
<color name="mytheme_accent">#RRGGBB</color>
<!-- etc. -->
```

2. **Add theme style** in `themes.xml`:
```xml
<style name="Theme.ServerMasterNCII.MyTheme" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/mytheme_accent</item>
    <!-- etc. -->
</style>
```

3. **Add constant** in `ThemeManager.java`:
```java
public static final String THEME_MYTHEME = "mytheme";
```

4. **Update `getThemeResourceId()`**:
```java
case THEME_MYTHEME:
    return R.style.Theme_ServerMasterNCII_MyTheme;
```

5. **Add card** in `activity_settings.xml`

6. **Wire click handler** in `SettingsActivity.java`

---

## 📚 Resources

- [Material Design 3 Color System](https://m3.material.io/styles/color/overview)
- [Android Theme Documentation](https://developer.android.com/develop/ui/views/theming/themes)
- [Material3 Components](https://m3.material.io/components)

---

**Created:** April 20, 2026  
**App Version:** 1.0  
**Material3 Version:** Latest  
**Min SDK:** 24  
**Target SDK:** 34
