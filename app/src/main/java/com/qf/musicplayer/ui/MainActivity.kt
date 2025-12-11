package com.qf.musicplayer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxy"
        private const val DEFAULT_DELAY = 5L
        private const val ACTION_MODE_SWITCH = "android.intent.action.MODE_SWITCH"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val configPath = File(Environment.getExternalStorageDirectory(), "player.config").absolutePath
    private var targetPackage: String = "com.maxmpz.audioplayer"
    private var targetClass: String = "*"
    private var playDelay: Long = DEFAULT_DELAY

    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunchTime: Long = 0
    private var permissionsGranted: Boolean = false

    private val modeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MODE_SWITCH) {
                Log.d(TAG, "MODE button pressed - finishing proxy")
                finish()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    override fun onResume() {
        super.onResume()

        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastLaunchTime < 2000) {
            Log.d(TAG, "onResume: Skipped (Debounce, ${currentTime - lastLaunchTime}ms)")
            return
        }
        lastLaunchTime = currentTime

        Log.d(TAG, "onResume: Starting proxy logic")

        if (!checkAndRequestPermissions()) {
            Log.d(TAG, "Waiting for permissions...")
            return
        }

        proceedWithLaunch()
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            return false
        }
        permissionsGranted = true
        return true
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

    private fun proceedWithLaunch() {
        loadConfig()
        handler.postDelayed({ startPlayer() }, 500) // Small delay before starting player
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "Touch detected - finishing proxy")
            finish()
            return true
        }
        return super.onTouchEvent(event)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startPlayer() {
        if (targetPackage.isEmpty() || targetPackage == packageName) {
            Log.e(TAG, "Invalid target package")
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

            val intent: Intent? = if (targetClass == "*" || targetClass.isEmpty()) {
                packageManager.getLaunchIntentForPackage(targetPackage)
            } else {
                Intent().setComponent(ComponentName(targetPackage, targetClass))
            }

            if (intent == null) {
                Log.e(TAG, "Failed to get launch intent for $targetPackage")
                finish()
                return
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "Player launched successfully")

            // Bring our proxy activity back to the front to be "on top" for the carousel.
            handler.postDelayed({
                Log.d(TAG, "Bringing proxy back to the front.")
                val selfIntent = Intent(this, MainActivity::class.java)
                selfIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(selfIntent)
            }, 350) // A small delay to let the player launch.

            proceedAfterLaunch()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch player", e)
            finish()
        }
    }

    private fun showPlayerNotFoundDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.player_not_found_title))
            .setMessage(getString(R.string.player_not_found_message))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun proceedAfterLaunch() {
        Log.d(TAG, "Scheduling PLAY command.")
        schedulePlayCommand()
    }

    private fun schedulePlayCommand() {
        if (playDelay > 0L) {
            val totalDelay = playDelay * 100L
            Log.d(TAG, "Scheduling PLAY in ${totalDelay}ms")

            handler.postDelayed({ sendMediaPlayKey() }, totalDelay)
        } else {
            Log.d(TAG, "Play delay is 0, skipping auto-play.")
        }
    }

    private fun sendMediaPlayKey() {
        Log.d(TAG, "Sending Media Key Intent: KEYCODE_MEDIA_PLAY")
        try {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = targetPackage
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            }
            sendBroadcast(downIntent)

            Thread.sleep(50)

            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = targetPackage
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            }
            sendBroadcast(upIntent)

            Log.d(TAG, "Media key intent dispatched successfully to $targetPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending media key intent", e)
        }
    }

    private fun loadConfig() {
        if (!permissionsGranted) {
            Log.w(TAG, "Permissions not granted, using defaults.")
            return
        }
        val file = File(configPath)
        if (file.exists()) {
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
            } catch (e: Exception) {
                Log.e(TAG, "ERROR reading config file!", e)
            }
        } else {
            Log.d(TAG, "Config file not found. Using defaults.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(modeReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: Proxy destroyed")
    }
}
