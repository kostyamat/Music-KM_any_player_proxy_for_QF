package com.carsyso.mediasdk.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AppPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val TAG = "AppPreferences"

    fun saveRecommendedComponents(
        packageName: String,
        mainActivity: String?,
        pcmPlayerActivity: String?,
        mediaService: String?
    ) {
        val editor = sharedPreferences.edit()
        editor.putString("${packageName}_mainActivity", mainActivity)
        editor.putString("${packageName}_pcmPlayerActivity", pcmPlayerActivity)
        editor.putString("${packageName}_mediaService", mediaService)
        editor.apply()
        Log.d(TAG, "saveRecommendedComponents: $packageName")
        Log.d(TAG, "saveRecommendedComponents: mainActivity: $mainActivity")
        Log.d(TAG, "saveRecommendedComponents: pcmPlayerActivity: $pcmPlayerActivity")
        Log.d(TAG, "saveRecommendedComponents: mediaService: $mediaService")
    }

    fun getRecommendedMainActivity(packageName: String): String? {
        val mainActivity = sharedPreferences.getString("${packageName}_mainActivity", null)
        Log.d(TAG, "getRecommendedMainActivity: packageName: $packageName, mainActivity: $mainActivity")
        return mainActivity
    }

    fun getRecommendedPcmPlayerActivity(packageName: String): String? {
        val pcmPlayerActivity = sharedPreferences.getString("${packageName}_pcmPlayerActivity", null)
        Log.d(TAG, "getRecommendedPcmPlayerActivity: packageName: $packageName, pcmPlayerActivity: $pcmPlayerActivity")
        return pcmPlayerActivity
    }

    fun getRecommendedMediaService(packageName: String): String? {
        val mediaService = sharedPreferences.getString("${packageName}_mediaService", null)
        Log.d(TAG, "getRecommendedMediaService: packageName: $packageName, mediaService: $mediaService")
        return mediaService
    }

    fun getRecommendedComponents(packageName: String): Map<String, String?> {
        val components = mapOf(
            "mainActivity" to getRecommendedMainActivity(packageName),
            "pcmPlayerActivity" to getRecommendedPcmPlayerActivity(packageName),
            "mediaService" to getRecommendedMediaService(packageName)
        )
        Log.d(TAG, "getRecommendedComponents: packageName: $packageName, components: $components")
        return components
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}