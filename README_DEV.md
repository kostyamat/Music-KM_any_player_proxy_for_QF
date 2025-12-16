# Music-KM: A Technical Guide for Advanced Android Integration

**To future developers (and myself):** This document is a detailed, step-by-step guide explaining the core, non-obvious techniques used in this project. It is designed to be a direct instruction manual for recreating this functionality in a similar environment, especially addressing the common pitfalls that can occur.

---

## Part 1: The "Dual-App" Structure (Proxy + Settings)

**Goal:** Create two separate launcher icons for a single application: one for the main (invisible) proxy logic, and another for a visible settings screen. 

**The Common Pitfall:** The system can get confused and launch the main proxy activity when you click the settings icon if the `Intent`s are not distinct enough. Simply declaring two `LAUNCHER` activities is not enough to be robust.

Here is the meticulous, step-by-step way to do it correctly.

### Step 1: Declare Two Distinct Activities in `AndroidManifest.xml`

This is the most critical step. You must declare both `MainActivity` and `SettingsActivity` with their own `<activity>` blocks. The key is how you differentiate them for the Android launcher.

```xml
<application ...>

    <!-- ACTIVITY 1: THE INVISIBLE PROXY -->
    <activity
        android:name=".ui.MainActivity"
        android:exported="true"
        android:label="@string/app_name" 
        android:launchMode="singleTask"
        android:theme="@android:style/Theme.Translucent.NoTitleBar"> 
        
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>

        <intent-filter>
             <action android:name="android.intent.action.MAIN" />
             <category android:name="android.intent.category.APP_MUSIC" />
        </intent-filter>
    </activity>


    <!-- ACTIVITY 2: THE VISIBLE SETTINGS -->
    <activity
        android:name=".settings.SettingsActivity"
        android:exported="true"
        android:label="@string/settings_activity_label"
        android:theme="@android:style/Theme.DeviceDefault"> 

        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

</application>
```

**Why This Works:** Although both have `ACTION_MAIN` and `CATEGORY_LAUNCHER`, the system treats them as two separate launchable components because they point to two different classes (`.ui.MainActivity` and `.settings.SettingsActivity`). The launcher creates two shortcuts, each with an `Intent` that explicitly names the `ComponentName` to launch.

### Step 2: Enforce a Strict Separation of Concerns in Code

Your activities must have clearly defined, non-overlapping roles.

-   **`MainActivity` MUST NOT contain any UI logic for settings.** Its only job is to proxy. If it needs settings, it must launch the *other* activity.
-   **`SettingsActivity` MUST NOT contain any proxy logic.** Its only job is to present choices and save them to `SharedPreferences`.

**Example `MainActivity.onCreate()` skeleton:**
```kotlin

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    prefs = getSharedPreferences("PlayerProxyPrefs", Context.MODE_PRIVATE)
    val selectedPlayer = prefs.getString("targetPackage", null)

    if (selectedPlayer == null) {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
        finish() 
        return
    }

    // ... continue with normal proxy logic ...
}
```

This structure makes it impossible for the system to get confused. The proxy is just a gatekeeper; if the gate is locked (no config), it sends the user to the keymaster (`SettingsActivity`) and disappears.

---

## Part 2: The "Transparent Cloak" & Reliably Detecting the Carousel

**Goal:** Launch the real player but trick the system carousel into thinking *we* are the foreground app, so it stops its UI flow.

**The Common Pitfall:** Applying the "cloak" logic on every launch, or failing to identify the carousel correctly, leads to bizarre behavior.

### Step 1: Find the Carousel's "Fingerprint"

You cannot guess; you must gather evidence. The key is to log every detail of the incoming `Intent` that launches your `MainActivity`.

**Add this diagnostic function to `MainActivity`:**
```kotlin
private fun logLaunchIntent(intent: Intent?) {
    if (intent == null) return
    Log.d("ProxyLogic", "--- LAUNCH INTENT ANALYSIS ---")
    Log.d("ProxyLogic", "Action: ${intent.action}")
    Log.d("ProxyLogic", "Referrer: ${referrer}") 
    // ... log other details like categories, flags, extras ...
}
```
Call this from both `onCreate()` and `onNewIntent()`.

### Step 2: Analyze the Logs

1.  **Launch from Carousel:** Press the "Mode" button. Look at the logcat output.
2.  **Launch from Icon:** Tap your app's main icon. Look at the logs again.

In our case, we found that the **`Referrer`** was the key. 
    -   Carousel Launch: `Referrer: android-app://com.qf.framework`
    -   Icon Launch: `Referrer: android-app://com.android.launcher3`

The host of the referrer URI (`com.qf.framework`) is the unique fingerprint of the carousel.

### Step 3: Implement the Conditional Logic

Now, use this fingerprint to control the flow.

```kotlin
// In MainActivity.kt
private var isCarouselLaunch = false
private const val CAROUSEL_REFERRER_HOST = "com.qf.framework"

private fun updateLaunchSource(intent: Intent?) {
    if (intent?.action == Intent.ACTION_MAIN) {
        isCarouselLaunch = (referrer?.host == CAROUSEL_REFERRER_HOST)
    }
}

private fun startPlayer() {
    // ... launch the real player ...
    startActivity(launchIntent)

    if (isCarouselLaunch) {
        // Apply the "transparent cloak"
        handler.postDelayed({
            val selfIntent = Intent(this, MainActivity::class.java)
            selfIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(selfIntent)
        }, 350) 
    } else {
        // Just finish
        finish()
    }
}
```

---

## Part 3: Requesting Special Permissions (The Right Way)

**Goal:** Guide the user to grant the `NotificationListenerService` permission.

**The Common Pitfall:** Attempting to request this permission using `requestPermissions()`. This will fail. It's a "special" permission that requires redirecting the user to a system screen.

### Step 1: Check if Your Service is Already Enabled

```kotlin
private fun isNotificationListenerEnabled(context: Context): Boolean {
    val componentName = ComponentName(context, MediaService::class.java)
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(componentName.flattenToString()) == true
}
```

### Step 2: Redirect the User to the Correct Settings Screen

If the check returns `false`, send the user to the right place.

```kotlin
if (!isNotificationListenerEnabled(this)) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    startActivity(intent)
    finish()
    return
}
```

---

## Part 4: Handling the "Uncloak"

**Goal:** Allow the user to dismiss the transparent proxy window.

### Mechanism 1: Touching the Screen

Since our `MainActivity` is the top-most window, it receives all touch events.

```kotlin
override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event?.action == MotionEvent.ACTION_DOWN) {
        finish() 
        return true 
    }
    return super.onTouchEvent(event)
}
```

### Mechanism 2: Responding to a System Event

Many head units send a `Broadcast` when a button is pressed.

1.  **Define the Action:** `private const val ACTION_FINISH_PROXY = "android.intent.action.MODE_SWITCH"`
2.  **Create a `BroadcastReceiver`:**
    ```kotlin
    private val finishProxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FINISH_PROXY) {
                finish()
            }
        }
    }
    ```
3.  **Register and Unregister:** Register the receiver in `onCreate()` and unregister it in `onDestroy()` to prevent memory leaks.

---

## Part 5: System-Level Integration

To function as a true system component, the app uses:

-   **`android:sharedUserId="android.uid.system"`:** This attribute in the `AndroidManifest.xml` allows the app, when installed as a system app on a rooted ROM, to run with the same privileges as the Android OS. This is key for automatically getting special permissions.
