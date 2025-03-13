package com.qf.musicplayer.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.carsyso.mediasdk.core.AppPreferences
import com.carsyso.mediasdk.core.SettingActivity

class MainActivity : AppCompatActivity() {
    private lateinit var appPreferences: AppPreferences
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)

        // Отримуємо назву пакета з ключем "mapped_player"
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val mappedPlayerPackage = sharedPreferences.getString("mapped_player", null)

        // Перевіряємо, чи збережено співставлення
        if (mappedPlayerPackage == null) {
            Log.d(TAG, "onCreate: Mapping not saved, starting SettingActivity")
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Отримуємо рекомендовані компоненти, використовуючи назву пакета як ключ
        val recommendedComponents = appPreferences.getRecommendedComponents(mappedPlayerPackage)
        val mainActivityName = recommendedComponents["mainActivity"]

        if (mainActivityName != null) {
            Log.d(TAG, "onCreate: Launching mapped player: $mappedPlayerPackage, mainActivity: $mainActivityName")
            try {
                val intent = Intent().apply {
                    setClassName(mappedPlayerPackage, mainActivityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "onCreate: Error launching mapped player", e)
            }
        } else {
            Log.e(TAG, "onCreate: Mapped player or main activity not found")
        }
        finish()
    }
}