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
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxy"
        private const val DEFAULT_PLAYER_PACKAGE = "com.maxmpz.audioplayer"
        private const val DEFAULT_PLAYER_CLASS = "*"
        private const val DEFAULT_DELAY = 5L
        private const val ACTION_MODE_SWITCH = "android.intent.action.MODE_SWITCH"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val MEDIA_KEY_EVENT_DELAY = 50L
        private const val DEBOUNCE_DELAY_MS = 2000L
        private const val START_PLAYER_DELAY_MS = 500L
        private const val REORDER_TO_FRONT_DELAY_MS = 350L
        private const val CONFIG_FILE_NAME = "player.config"
        private const val CAROUSEL_REFERRER = "com.qf.framework"
    }

    // --- State ---
    private val configPath = File(Environment.getExternalStorageDirectory(), CONFIG_FILE_NAME).absolutePath
    private var targetPackage: String = DEFAULT_PLAYER_PACKAGE
    private var targetClass: String = DEFAULT_PLAYER_CLASS
    private var playDelay: Long = DEFAULT_DELAY
    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunchTime: Long = 0
    private var permissionsGranted: Boolean = false
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

        updateLaunchSource(intent)
        logLaunchIntent(intent)

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
        setIntent(intent) // Update the activity's intent
        updateLaunchSource(intent)
        logLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (isDebouncing()) return

        Log.d(TAG, "onResume: Starting proxy logic")

        if (!checkAndRequestPermissions()) {
            Log.d(TAG, "Waiting for permissions...")
            return
        }

        proceedWithLaunch()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(modeReceiver)
        } catch (_: Exception) {
            // Already unregistered or failed to register
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            permissionsGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (permissionsGranted) {
                Log.d(TAG, "Permission GRANTED by user")
            } else {
                Log.e(TAG, "Permission DENIED by user, using defaults")
            }
            proceedWithLaunch()
        }
    }

    // --- Private Logic Flow ---

    private fun proceedWithLaunch() {
        loadConfig()
        handler.postDelayed({ startPlayer() }, START_PLAYER_DELAY_MS)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startPlayer() {
        if (targetPackage.isEmpty() || targetPackage == packageName) {
            Log.e(TAG, "Invalid target package specified")
            finish()
            return
        }

        if (!isPackageInstalled(targetPackage)) {
            Log.e(TAG, "Target player not installed: $targetPackage")
            showPlayerNotFoundDialog()
            return
        }

        try {
            Log.d(TAG, "Launching Target: $targetPackage")

            val launchIntent = getLaunchIntentForPlayer()
            if (launchIntent == null) {
                Log.e(TAG, "Failed to get launch intent for $targetPackage")
                finish()
                return
            }

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            Log.d(TAG, "Player launched successfully")

            if (isCarouselLaunch) {
                Log.d(TAG, "Carousel launch: Bringing proxy to front.")
                bringProxyToFront()
                proceedAfterLaunch()
            } else {
                Log.d(TAG, "Direct launch: Finishing proxy.")
                proceedAfterLaunch() // Schedule play key
                finish() // And exit
            }

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to launch activity for player. Is targetClass '$targetClass' correct?", e)
            finish()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while launching player.", e)
            finish()
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

    private fun proceedAfterLaunch() {
        Log.d(TAG, "Scheduling PLAY command.")
        schedulePlayCommand()
    }

    private fun schedulePlayCommand() {
        if (playDelay <= 0L) {
            Log.d(TAG, "Play delay is 0, skipping auto-play.")
            return
        }

        val totalDelay = playDelay * 100L
        Log.d(TAG, "Scheduling PLAY in ${totalDelay}ms")
        handler.postDelayed({ sendMediaPlayKey() }, totalDelay)
    }

    private fun sendMediaPlayKey() {
        if (MediaService.isPlayerActive(targetPackage, this)) {
            Log.d(TAG, "Music is already active in the target player, not sending PLAY key.")
            return
        }

        Log.d(TAG, "Sending Media Key Intent: KEYCODE_MEDIA_PLAY")
        try {
            sendBroadcast(createMediaKeyIntent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            Log.d(TAG, "Media key DOWN dispatched to $targetPackage")

            handler.postDelayed({
                sendBroadcast(createMediaKeyIntent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                Log.d(TAG, "Media key UP dispatched to $targetPackage")
            }, MEDIA_KEY_EVENT_DELAY)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending media key intent", e)
        }
    }

    // --- Utility Methods ---
    private fun updateLaunchSource(intent: Intent?) {
        // Only update the launch source if it's a MAIN action.
        // This prevents the REORDER_TO_FRONT intent from overwriting it.
        if (intent?.action == Intent.ACTION_MAIN) {
            isCarouselLaunch = referrer?.host == CAROUSEL_REFERRER
            Log.d(TAG, "Launch source updated. IsCarousel: $isCarouselLaunch, Referrer: ${referrer?.host}")
        }
    }

    private fun logLaunchIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(TAG, "--- LAUNCH INTENT (null) ---")
            return
        }
        Log.d(TAG, "--- LAUNCH INTENT ---")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Categories: ${intent.categories}")
        Log.d(TAG, "Flags: ${intent.flags}")
        Log.d(TAG, "Component: ${intent.component}")
        Log.d(TAG, "Referrer: ${referrer}")
        intent.extras?.let {
            for (key in it.keySet()) {
                Log.d(TAG, "Extra: $key = ${it.get(key)}")
            }
        }
        Log.d(TAG, "---------------------")
    }

    private fun loadConfig() {
        if (!permissionsGranted) {
            Log.w(TAG, "Read permission not granted, using defaults.")
            return
        }
        val file = File(configPath)
        if (!file.exists()) {
            Log.d(TAG, "Config file not found. Using defaults.")
            return
        }

        try {
            BufferedReader(FileReader(file)).use { br ->
                val pkg = br.readLine()?.trim()
                val cls = br.readLine()?.trim()
                val d = br.readLine()?.trim()

                if (!pkg.isNullOrEmpty()) targetPackage = pkg
                if (!cls.isNullOrEmpty()) targetClass = cls
                playDelay = d?.toLongOrNull() ?: DEFAULT_DELAY
            }
            Log.d(TAG, "Config loaded: pkg=$targetPackage, cls=$targetClass, delay=${playDelay * 100}ms")
        } catch (e: IOException) {
            Log.e(TAG, "I/O error reading config file.", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error reading config file.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error reading config file.", e)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            return false
        }
        permissionsGranted = true
        return true
    }

    private fun isDebouncing(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastLaunchTime < DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "onResume: Skipped (Debounce, ${currentTime - lastLaunchTime}ms)")
            return true
        }
        lastLaunchTime = currentTime
        return false
    }

    private fun getLaunchIntentForPlayer(): Intent? {
        return if (targetClass == "*" || targetClass.isEmpty()) {
            packageManager.getLaunchIntentForPackage(targetPackage)
        } else {
            Intent().setComponent(ComponentName(targetPackage, targetClass))
        }
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
        if (isFinishing || isDestroyed) {
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.player_not_found_title))
            .setMessage(getString(R.string.player_not_found_message))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun isFirstLaunchWithMissingPermissions(): Boolean {
        return intent.action == Intent.ACTION_MAIN && (!isNotificationListenerEnabled(this) || !isAccessibilityServiceEnabled(this))
    }

    private fun openSystemSettingsForPermissions() {
        Log.d(TAG, "Required services not enabled. Opening settings...")
        val intent = if (!isNotificationListenerEnabled(this)) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        } else {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
        startActivity(intent)
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, MediaService::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, KeyLoggingService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServicesSetting?.contains(expectedComponentName.flattenToString()) == true
    }
}
