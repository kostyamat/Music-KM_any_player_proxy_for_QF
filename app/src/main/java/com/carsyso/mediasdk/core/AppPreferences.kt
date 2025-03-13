package com.carsyso.mediasdk.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val TAG = "AppPreferences"

    fun saveRecommendedComponents(
        packageName: String,
        mainActivity: String?,
        pcmPlayerActivity: String?,
        mediaService: String?
    ) {
        val editor = sharedPreferences.edit()
        editor.putString("$packageName:mainActivity", mainActivity)
        editor.putString("$packageName:pcmPlayerActivity", pcmPlayerActivity)
        editor.putString("$packageName:mediaService", mediaService)
        editor.apply()
        Log.d(TAG, "saveRecommendedComponents: packageName: $packageName, mainActivity: $mainActivity, pcmPlayerActivity: $pcmPlayerActivity, mediaService: $mediaService")
    }

    fun getRecommendedComponents(packageName: String): Map<String, String?> {
        val components = mutableMapOf<String, String?>()
        components["mainActivity"] = getRecommendedMainActivity(packageName)
        components["pcmPlayerActivity"] = getRecommendedPcmPlayerActivity(packageName)
        components["mediaService"] = getRecommendedMediaService(packageName)
        Log.d(TAG, "getRecommendedComponents: packageName: $packageName, components: $components")
        return components
    }

    fun getRecommendedMainActivity(packageName: String): String? {
        Log.d(TAG, "getRecommendedMainActivity: packageName: $packageName, mainActivity: ${sharedPreferences.getString("$packageName:mainActivity", null)}")
        return sharedPreferences.getString("$packageName:mainActivity", null)
    }

    fun getRecommendedPcmPlayerActivity(packageName: String): String? {
        Log.d(TAG, "getRecommendedPcmPlayerActivity: packageName: $packageName, pcmPlayerActivity: ${sharedPreferences.getString("$packageName:pcmPlayerActivity", null)}")
        return sharedPreferences.getString("$packageName:pcmPlayerActivity", null)
    }

    fun getRecommendedMediaService(packageName: String): String? {
        Log.d(TAG, "getRecommendedMediaService: packageName: $packageName, mediaService: ${sharedPreferences.getString("$packageName:mediaService", null)}")
        return sharedPreferences.getString("$packageName:mediaService", null)
    }
}