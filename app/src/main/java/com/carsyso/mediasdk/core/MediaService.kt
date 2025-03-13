package com.carsyso.mediasdk.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MediaService : Service() {
    private val TAG = "MediaService"
    private lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: MediaService started")

        // Отримуємо назву пакета з ключем "mapped_player"
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val mappedPlayerPackage = sharedPreferences.getString("mapped_player", null)

        // Перевіряємо, чи збережено співставлення
        if (mappedPlayerPackage == null) {
            Log.e(TAG, "onStartCommand: Mapping not saved, cannot start MediaService")
            stopSelf()
            return START_NOT_STICKY
        }

        // Отримуємо рекомендовані компоненти, використовуючи назву пакета як ключ
        val recommendedComponents = appPreferences.getRecommendedComponents(mappedPlayerPackage)
        val mediaServiceName = recommendedComponents["mediaService"]

        if (mediaServiceName != null) {
            Log.d(TAG, "onStartCommand: Launching mapped player's MediaService: $mappedPlayerPackage, mediaService: $mediaServiceName")
            try {
                val serviceIntent = Intent().apply {
                    setClassName(mappedPlayerPackage, mediaServiceName)
                }
                startService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "onStartCommand: Error launching mapped player's MediaService", e)
            }
        } else {
            Log.e(TAG, "onStartCommand: Mapped player or MediaService not found")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: MediaService destroyed")
    }
}