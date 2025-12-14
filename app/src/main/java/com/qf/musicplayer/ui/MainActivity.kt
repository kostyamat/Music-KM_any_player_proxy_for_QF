package com.qf.musicplayer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import com.qf.musicplayer.settings.SettingsActivity

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxy"
        private const val DEFAULT_DELAY = 5L
        private const val ACTION_MODE_SWITCH = "android.intent.action.MODE_SWITCH"
        private const val MEDIA_KEY_EVENT_DELAY = 50L
        private const val DEBOUNCE_DELAY_MS = 2000L
        private const val START_PLAYER_DELAY_MS = 500L
        private const val REORDER_TO_FRONT_DELAY_MS = 350L
        private const val CAROUSEL_REFERRER = "com.qf.framework"

        private const val PREFS_NAME = "PlayerProxyPrefs"
        private const val PREF_TARGET_PACKAGE = "targetPackage"
    }

    // --- State ---
    private lateinit var prefs: SharedPreferences
    private var targetPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunchTime: Long = 0
    private var isCarouselLaunch = false

    private val modeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MODE_SWITCH) {
                Log.d(TAG, "MODE button pressed - finishing proxy")
                finish()
            }
        }
    }

    // --- Lifecycle Methods ---

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        targetPackage = prefs.getString(PREF_TARGET_PACKAGE, null)

        updateLaunchSource(intent)

        if (targetPackage == null) {
            Log.d(TAG, "No target player configured. Launching Settings.")
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        if (isFirstLaunchWithMissingPermissions()) {
            openSystemSettingsForPermissions()
            finish()
            return
        }

        setShowWhenLocked(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try {
            val filter = IntentFilter(ACTION_MODE_SWITCH)
            registerReceiver(modeReceiver, filter)
            Log.d(TAG, "Mode receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register mode receiver", e)
        }

        Log.d(TAG, "onCreate: Transparent Proxy Created")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateLaunchSource(intent)
    }

    override fun onResume() {
        super.onResume()

        if (targetPackage == null) return // Already handling this in onCreate

        if (isDebouncing()) return

        Log.d(TAG, "onResume: Starting proxy logic")
        proceedWithLaunch()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(modeReceiver)
        } catch (_: Exception) {
            // Already unregistered
        }
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: Proxy destroyed")
    }

    // --- Event/Callback Overrides ---

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "Touch detected - finishing proxy")
            finish()
            return true
        }
        return super.onTouchEvent(event)
    }

    // --- Private Logic Flow ---

    private fun proceedWithLaunch() {
        handler.postDelayed({ startPlayer() }, START_PLAYER_DELAY_MS)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startPlayer() {
        val currentTarget = targetPackage ?: return

        if (currentTarget.isEmpty() || currentTarget == packageName) {
            Log.e(TAG, "Invalid target package specified")
            finish()
            return
        }

        if (!isPackageInstalled(currentTarget)) {
            Log.e(TAG, "Target player not installed: $currentTarget")
            showPlayerNotFoundDialog()
            return
        }

        try {
            Log.d(TAG, "Launching Target: $currentTarget")

            val launchIntent = packageManager.getLaunchIntentForPackage(currentTarget)
            if (launchIntent == null) {
                Log.e(TAG, "Failed to get launch intent for $currentTarget")
                finish()
                return
            }

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            Log.d(TAG, "Player launched successfully")

            if (isCarouselLaunch) {
                Log.d(TAG, "Carousel launch: Bringing proxy to front.")
                bringProxyToFront()
                schedulePlayCommand()
            } else {
                Log.d(TAG, "Direct launch: Scheduling play key and preparing to finish.")
                schedulePlayCommand()
            }

        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred while launching player.", e)
            finish()
        }
    }

    private fun bringProxyToFront() {
        handler.postDelayed({
            Log.d(TAG, "Bringing proxy back to the front.")
            val selfIntent = Intent(this, MainActivity::class.java)
            selfIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(selfIntent)
        }, REORDER_TO_FRONT_DELAY_MS)
    }

    private fun schedulePlayCommand() {
        val totalDelay = DEFAULT_DELAY * 100L
        Log.d(TAG, "Scheduling PLAY in ${totalDelay}ms")
        handler.postDelayed({ sendMediaPlayKey() }, totalDelay)
    }

    private fun sendMediaPlayKey() {
        val currentTarget = targetPackage ?: return

        if (MediaService.isPlayerActive(currentTarget, this)) {
            Log.d(TAG, "Music is already active in the target player, not sending PLAY key.")
            if (!isCarouselLaunch) {
                Log.d(TAG, "Direct launch: Music already playing, finishing proxy.")
                finish()
            }
            return
        }

        Log.d(TAG, "Sending Media Key Intent: KEYCODE_MEDIA_PLAY")
        try {
            sendBroadcast(createMediaKeyIntent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            Log.d(TAG, "Media key DOWN dispatched to $currentTarget")

            handler.postDelayed({
                sendBroadcast(createMediaKeyIntent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                Log.d(TAG, "Media key UP dispatched to $currentTarget")
                if (!isCarouselLaunch) {
                    Log.d(TAG, "Direct launch: Play key sent, finishing proxy.")
                    finish()
                }
            }, MEDIA_KEY_EVENT_DELAY)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending media key intent", e)
            if (!isCarouselLaunch) {
                finish()
            }
        }
    }

    // --- Utility Methods ---

    private fun updateLaunchSource(intent: Intent?) {
        if (intent?.action == Intent.ACTION_MAIN) {
            isCarouselLaunch = referrer?.host == CAROUSEL_REFERRER
        }
    }

    private fun isDebouncing(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastLaunchTime < DEBOUNCE_DELAY_MS) {
            return true
        }
        lastLaunchTime = currentTime
        return false
    }

    private fun createMediaKeyIntent(action: Int, keyCode: Int): Intent {
        return Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            `package` = targetPackage
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, keyCode))
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showPlayerNotFoundDialog() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.player_not_found_title))
            .setMessage(getString(R.string.player_not_found_message))
            .setPositiveButton(getString(R.string.open_settings_button)) { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun isFirstLaunchWithMissingPermissions(): Boolean {
        return intent.action == Intent.ACTION_MAIN && !isNotificationListenerEnabled(this)
    }

    private fun openSystemSettingsForPermissions() {
        Log.d(TAG, "Required Notification Listener service not enabled. Opening settings...")
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, MediaService::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }
}
