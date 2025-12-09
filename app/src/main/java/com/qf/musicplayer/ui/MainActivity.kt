package com.qf.musicplayer.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxy"
        private const val DEFAULT_DELAY = 5L
    }

    private val configPath = File(Environment.getExternalStorageDirectory(), "player.config").absolutePath
    private var targetPackage: String = "com.maxmpz.audioplayer"
    private var targetClass: String = "*"
    private var playDelay: Long = DEFAULT_DELAY

    private val handler = Handler(Looper.getMainLooper())

    // Змінна для захисту від багаторазового запуску (Debounce)
    private var lastLaunchTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Встановлюємо прапорці, щоб вікно точно створилось і система його побачила
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "onCreate: Proxy Window Created")
    }

    override fun onResume() {
        super.onResume()

        // --- ЗАХИСТ ВІД ЗАЦИКЛЕННЯ ---
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastLaunchTime < 3000) {
            // Якщо минуло менше 3 секунд з минулого запуску - ігноруємо.
            Log.d(TAG, "onResume: Skipped (Debounce active)")
            return
        }
        lastLaunchTime = currentTime
        // ------------------------------

        Log.d(TAG, "onResume: Processing Launch Logic")
        loadConfig()

        // КРОК 1: Ми нічого не робимо перші 500 мс.
        // Ми просто показуємо чорний екран. Це дає каруселі час зрозуміти:
        // "Ага, додаток com.qf.musicplayer.ui успішно відкрився і намалювався".

        handler.postDelayed({
            startPlayer()
        }, 500) // Затримка пів секунди перед відкриттям Spotify
    }

    private fun startPlayer() {
        if (targetPackage.isEmpty() || targetPackage == packageName) return

        try {
            Log.d(TAG, "Launching Target: $targetPackage")
            val intent = Intent()
            if (targetClass == "*" || targetClass.isEmpty()) {
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                intent.component = launchIntent?.component
            } else {
                intent.component = ComponentName(targetPackage, targetClass)
            }

            if (intent.component != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

                // Плануємо натискання Play
                schedulePlayCommand()
            } else {
                Log.e(TAG, "Target Intent is null")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch player", e)
        }
    }

    private fun schedulePlayCommand() {
        if (playDelay > 0L) {
            // Важливо: playDelay відраховується вже ПІСЛЯ запуску Spotify
            Log.d(TAG, "Scheduling PLAY in $playDelay sec")

            // Використовуємо окремий Handler або той самий, але переконаємось що він свіжий
            handler.postDelayed({
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)

                // Опціонально: Тільки тепер можна згорнути наше вікно, якщо воно ще висить
                // moveTaskToBack(true)
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
            } catch (e: Exception) { }
        }
    }
}