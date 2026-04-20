# ⚡ Quick Setup Commands

## 1️⃣ Generate Facebook Key Hash

```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```
**Password:** `android`

**OR** run your app and check Logcat filter: `FB_KEY_HASH`

---

## 2️⃣ Generate SHA-1 for Firebase

```bash
./gradlew signingReport
```

Copy the SHA1 from the output under `Variant: debug`.

---

## 3️⃣ Where to Paste

### Facebook Key Hash:
1. [developers.facebook.com](https://developers.facebook.com)
2. Your App → Settings → Basic → Android Platform
3. **Package Name:** `com.example.servermasterncii`
4. **Key Hashes:** Paste the hash from command 1

### SHA-1:
1. [Firebase Console](https://console.firebase.google.com)
2. Project Settings → Your apps → Android app
3. Click **Add fingerprint** → Paste SHA1

---

## 4️⃣ Verify Configuration

Run your app and check Logcat:

```bash
# Filter by AUTH_CONFIG to see configuration status
adb logcat -s AUTH_CONFIG

# Filter by FB_KEY_HASH to see your key hash
adb logcat -s FB_KEY_HASH

# Filter by FB_LOGIN to see Facebook login flow
adb logcat -s FB_LOGIN
```

---

## 5️⃣ Test Login

1. Click **Facebook** button
2. Check Logcat for `✅ Facebook login successful`
3. Should navigate to MainActivity

---

## 🔧 Your Current Configuration

| Setting | Value |
|---------|-------|
| **Package Name** | `com.example.servermasterncii` |
| **Facebook App ID** | `3599678083515365` |
| **Facebook Client Token** | `4f8084b0c2e608dc862be19ee8f59668` |
| **Google Web Client ID** | `97665851127-0gnqoton7j6aeiqoiof0ebbltgsvh9jj.apps.googleusercontent.com` |
| **Firebase Project** | `server-master-ncii` |

---

## ✅ Checklist

- [ ] Generate Facebook key hash
- [ ] Add key hash to Meta Developer Console
- [ ] Generate SHA-1
- [ ] Add SHA-1 to Firebase Console
- [ ] Enable Google Sign-In in Firebase
- [ ] Enable Facebook Login in Firebase (with App ID and Secret)
- [ ] Add OAuth redirect URI to Meta Console
- [ ] Rebuild app
- [ ] Test on physical device

---

## 🐛 Quick Fixes

**"No Android key hashes configured"**
→ Add key hash to Meta Console, wait 5-10 minutes

**Error 190: Invalid OAuth token**
→ Verify Facebook App ID and Client Token in `strings.xml`

**Google Sign-In fails**
→ Add SHA-1 to Firebase, download new `google-services.json`

**"default_web_client_id not found"**
→ Already fixed in `strings.xml`!

---

**Need detailed help?** See `FACEBOOK_LOGIN_SETUP.md`
