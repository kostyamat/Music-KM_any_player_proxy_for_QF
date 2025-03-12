package com.carsyso.mediasdk.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class MappingHelper(private val context: Context) {

    private val TAG = "MappingHelper"
    // Додаємо список пакетів, які потрібно ігнорувати
    private val ignoredPackages = listOf(
        "com.qf.musicplayer", // Основний пакет нашої програми
        "com.carsyso.mediasdk.core" // Пакет з налаштуваннями (com.carsyso.mediasdk.core.SettingActivity)
    )

    fun getPackageNameForPlayer(playerName: String?): String? {
        if (playerName == null) return null
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps = pm.queryIntentActivities(intent, 0)
        for (resolveInfo in installedApps) {
            // Додаємо перевірку на ігноровані пакети
            if (resolveInfo.loadLabel(pm).toString() == playerName && !ignoredPackages.contains(resolveInfo.activityInfo.packageName)) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }

    fun getActivitiesForPackage(packageName: String?): List<String> {
        if (packageName == null || ignoredPackages.contains(packageName)) return emptyList() // Додаємо перевірку на ігноровані пакети
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        return packageInfo.activities?.map { it.name.substringAfterLast(".").trim() }
            ?.map { shortenName(it, packageName) } ?: emptyList()
    }

    fun getServicesForPackage(packageName: String?): List<String> {
        if (packageName == null || ignoredPackages.contains(packageName)) return emptyList() // Додаємо перевірку на ігноровані пакети
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        return packageInfo.services?.map { it.name.substringAfterLast(".").trim() }
            ?.map { shortenName(it, packageName) } ?: emptyList()
    }

    fun findRecommendedMainActivity(packageName: String?): String? {
        Log.d(TAG, "findRecommendedMainActivity() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null // Додаємо перевірку на ігноровані пакети
        val activities = getActivitiesForPackage(packageName)
        val mainActivities = activities.filter { it.contains("MAIN", ignoreCase = true) && !it.contains("VIDEO", ignoreCase = true) }
        val musicAudioActivities = activities.filter { (it.contains("MUSIC", ignoreCase = true) || it.contains("AUDIO", ignoreCase = true)) && !it.contains("VIDEO", ignoreCase = true) }

        return when {
            mainActivities.size == 1 -> {
                Log.d(TAG, "findRecommendedMainActivity(): returning ${mainActivities[0]}")
                mainActivities[0]
            }
            musicAudioActivities.size == 1 -> {
                Log.d(TAG, "findRecommendedMainActivity(): returning ${musicAudioActivities[0]} because it contains 'MUSIC' or 'AUDIO'")
                musicAudioActivities[0]
            }
            else -> {
                Log.d(TAG, "findRecommendedMainActivity(): no unique activity found")
                null
            }
        }
    }

    fun findRecommendedPcmPlayerActivity(packageName: String?): String? {
        Log.d(TAG, "findRecommendedPcmPlayerActivity() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null // Додаємо перевірку на ігноровані пакети
        val activities = getActivitiesForPackage(packageName)
        val playerActivities = activities.filter {
            it.contains("PLAYER", ignoreCase = true) ||
                    it.contains("PLAYBACK", ignoreCase = true) ||
                    it.contains("MUSIC", ignoreCase = true) ||
                    it.contains("AUDIO", ignoreCase = true) ||
                    it.contains("CONTROL", ignoreCase = true) ||
                    it.contains("MEDIA", ignoreCase = true)
        }.filter { !it.contains("VIDEO", ignoreCase = true) }

        val prioritizedActivities = listOf("PlayerActivity", "PlaybackActivity", "AudioPlayerActivity", "MusicActivity", "AudioActivity", "ExpandedControlsActivity", "PlaylistManagerActivity")
        val prioritizedPlayerActivities = playerActivities.filter { prioritizedActivities.contains(it) }

        return when {
            prioritizedPlayerActivities.size == 1 -> {
                Log.d(TAG, "findRecommendedPcmPlayerActivity(): returning ${prioritizedPlayerActivities[0]}")
                prioritizedPlayerActivities[0]
            }
            playerActivities.size == 1 -> {
                Log.d(TAG, "findRecommendedPcmPlayerActivity(): returning ${playerActivities[0]}")
                playerActivities[0]
            }
            else -> {
                Log.d(TAG, "findRecommendedPcmPlayerActivity(): no unique activity found")
                null
            }
        }
    }

    fun findRecommendedMediaService(packageName: String?): String? {
        Log.d(TAG, "findRecommendedMediaService() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null // Додаємо перевірку на ігноровані пакети
        val services = getServicesForPackage(packageName)
        val mediaServices = services.filter {
            it.contains("PLAY", ignoreCase = true) ||
                    it.contains("PLAYBACK", ignoreCase = true) ||
                    it.contains("MUSIC", ignoreCase = true) ||
                    it.contains("AUDIO", ignoreCase = true) ||
                    it.contains("MEDIA", ignoreCase = true)
        }.filter { !it.contains("VIDEO", ignoreCase = true) }

        return when {
            mediaServices.size == 1 -> {
                Log.d(TAG, "findRecommendedMediaService(): returning ${mediaServices[0]}")
                mediaServices[0]
            }
            mediaServices.any { it.contains("MUSIC", ignoreCase = true) || it.contains("AUDIO", ignoreCase = true) } -> {
                val musicAudioService = mediaServices.firstOrNull { it.contains("MUSIC", ignoreCase = true) || it.contains("AUDIO", ignoreCase = true) }
                Log.d(TAG, "findRecommendedMediaService(): returning $musicAudioService because it contains 'MUSIC' or 'AUDIO'")
                musicAudioService
            }
            else -> {
                Log.d(TAG, "findRecommendedMediaService(): no unique service found")
                null
            }
        }
    }

    private fun shortenName(name: String, packageName: String): String {
        // Видаляємо лише назву пакета, якщо вона є на початку імені
        return if (name.startsWith(packageName)) {
            name.substring(packageName.length).trim()
        } else {
            name
        }
    }
}