package com.qf.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.carsyso.mediasdk.core.AppPreferences

class PcmPlayerActivity : AppCompatActivity() {
    private lateinit var appPreferences: AppPreferences
    private val TAG = "PcmPlayerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)

        // Отримуємо назву пакета з ключем "mapped_player"
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val mappedPlayerPackage = sharedPreferences.getString("mapped_player", null)

        // Перевіряємо, чи збережено співставлення
        if (mappedPlayerPackage == null) {
            Log.e(TAG, "onCreate: Mapping not saved, cannot launch PcmPlayerActivity")
            finish()
            return
        }

        // Отримуємо рекомендовані компоненти, використовуючи назву пакета як ключ
        val recommendedComponents = appPreferences.getRecommendedComponents(mappedPlayerPackage)
        val pcmPlayerActivityName = recommendedComponents["pcmPlayerActivity"]

        if (pcmPlayerActivityName != null) {
            Log.d(TAG, "onCreate: Launching mapped player's PcmPlayerActivity: $mappedPlayerPackage, pcmPlayerActivity: $pcmPlayerActivityName")
            try {
                val intent = Intent().apply {
                    setClassName(mappedPlayerPackage, pcmPlayerActivityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "onCreate: Error launching mapped player's PcmPlayerActivity", e)
            }
        } else {
            Log.e(TAG, "onCreate: Mapped player or PcmPlayerActivity not found")
        }
        finish()
    }
}