package com.qf.musicplayer.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    }

    private val configPath = File(Environment.getExternalStorageDirectory(), "player.config").absolutePath
    private var targetPackage: String = "com.maxmpz.audioplayer"
    private var targetClass: String = "*"
    private var playDelay: Long = DEFAULT_DELAY

    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunchTime: Long = 0
    private var playerLaunched: Boolean = false

    // BroadcastReceiver для відловлювання Mode кнопки
    private val modeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MODE_SWITCH) {
                Log.d(TAG, "MODE button pressed - going to background")
                moveTaskToBack(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Робимо вікно ПРОЗОРИМ і поверх всього
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or // НЕ перехоплюємо тачі (вони йдуть до плеєра)
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // НЕ забираємо фокус
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Реєструємо receiver для Mode кнопки
        try {
            val filter = IntentFilter(ACTION_MODE_SWITCH)
            registerReceiver(modeReceiver, filter)
            Log.d(TAG, "Mode receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register mode receiver", e)
        }

        Log.d(TAG, "onCreate: Transparent Proxy Window Created")
    }

    override fun onResume() {
        super.onResume()

        // Захист від зациклення
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastLaunchTime < 3000) {
            Log.d(TAG, "onResume: Skipped (Debounce active)")
            return
        }
        lastLaunchTime = currentTime
        playerLaunched = false

        Log.d(TAG, "onResume: Starting transparent overlay")
        loadConfig()

        // ЛОГІКА:
        // 1. Запускаємо плеєр відразу
        // 2. Залишаємось поверх нього прозорим шаром
        // 3. Система бачить com.qf.musicplayer в топі ✅
        // 4. Якщо юзер клікає або натискає Mode - йдемо у фон

        handler.postDelayed({
            if (!playerLaunched) {
                startPlayer()
            }
        }, 500) // Невелика затримка для ініціалізації
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Overlay paused")
        handler.removeCallbacksAndMessages(null)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Якщо юзер клікнув по екрану - йдемо у фон, щоб він міг взаємодіяти з плеєром
        if (event?.action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "Touch detected - going to background")

            // Прибираємо прапорці, щоб тепер плеєр міг отримувати тачі
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )

            moveTaskToBack(true)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun startPlayer() {
        if (targetPackage.isEmpty() || targetPackage == packageName) {
            Log.e(TAG, "Invalid target package")
            return
        }

        try {
            Log.d(TAG, "Launching Target: $targetPackage")
            playerLaunched = true

            val intent = Intent()
            if (targetClass == "*" || targetClass.isEmpty()) {
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                intent.component = launchIntent?.component
            } else {
                intent.component = ComponentName(targetPackage, targetClass)
            }

            if (intent.component != null) {
                // ВАЖЛИВО: БЕЗ FLAG_ACTIVITY_NEW_TASK
                // Щоб плеєр відкрився "під" нашим прозорим вікном
                startActivity(intent)

                // Плануємо натискання Play
                schedulePlayCommand()

                Log.d(TAG, "Player launched, staying on top as transparent overlay")
            } else {
                Log.e(TAG, "Target Intent is null")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch player", e)
        }
    }

    private fun schedulePlayCommand() {
        if (playDelay > 0L) {
            Log.d(TAG, "Scheduling PLAY in ${playDelay * 100}ms")

            handler.postDelayed({
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
            }, playDelay * 100L)
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        Log.d(TAG, "Sending Media Key: $keyCode")
        try {
            val time = SystemClock.uptimeMillis()
            val down = KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0)

            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(down)
            am.dispatchMediaKeyEvent(up)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending media key", e)
        }
    }

    private fun loadConfig() {
        val file = File(configPath)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { br ->
                    targetPackage = br.readLine()?.trim() ?: ""
                    targetClass = br.readLine()?.trim() ?: ""
                    val d = br.readLine()?.trim()
                    playDelay = d?.toLongOrNull() ?: DEFAULT_DELAY
                }
                Log.d(TAG, "Config: pkg=$targetPackage, cls=$targetClass, delay=${playDelay * 100}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Config load error", e)
            }
        } else {
            Log.d(TAG, "Config not found, using defaults")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(modeReceiver)
            Log.d(TAG, "Mode receiver unregistered")
        } catch (e: Exception) {
            // Receiver вже був відреєстрований
        }
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: Transparent overlay destroyed")
    }
}