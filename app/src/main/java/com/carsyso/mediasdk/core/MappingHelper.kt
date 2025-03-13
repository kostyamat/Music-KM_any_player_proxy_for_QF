package com.carsyso.mediasdk.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class MappingHelper(private val context: Context) {

    private val TAG = "MappingHelper"
    private val ignoredPackages = listOf(
        "com.qf.musicplayer",
        "com.carsyso.mediasdk.core"
    )

    fun getPackageNameForPlayer(playerName: String?): String? {
        if (playerName == null) return null
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps = pm.queryIntentActivities(intent, 0)
        for (resolveInfo in installedApps) {
            if (resolveInfo.loadLabel(pm).toString() == playerName && !ignoredPackages.contains(resolveInfo.activityInfo.packageName)) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }

    fun getActivitiesForPackage(packageName: String?): List<String> {
        if (packageName == null || ignoredPackages.contains(packageName)) return emptyList()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        return packageInfo.activities?.map { it.name } ?: emptyList()
    }

    fun getServicesForPackage(packageName: String?): List<String> {
        if (packageName == null || ignoredPackages.contains(packageName)) return emptyList()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        return packageInfo.services?.map { it.name } ?: emptyList()
    }

    fun findRecommendedMainActivity(packageName: String?): String? {
        Log.d(TAG, "findRecommendedMainActivity() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null
        return getMainActivityName(context, packageName)
    }

    fun findRecommendedPcmPlayerActivity(packageName: String?): String? {
        Log.d(TAG, "findRecommendedPcmPlayerActivity() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null
        val activities = getActivitiesForPackage(packageName)
        val matchedActivity = findActivityByIntent(packageName, Intent.ACTION_VIEW, "audio/*")
        return matchedActivity ?: findBestMatch(activities, prioritizedCombinations, "Activity")
    }

    fun findRecommendedMediaService(packageName: String?): String? {
        Log.d(TAG, "findRecommendedMediaService() called with packageName: $packageName")
        if (packageName == null || ignoredPackages.contains(packageName)) return null
        val services = getServicesForPackage(packageName)
        val matchedService = findServiceByIntent(packageName, "com.qf.musicplayer.MediaService")
        return matchedService ?: findBestMatch(services, prioritizedCombinations, "Service")
    }

    private fun findActivityByIntent(packageName: String, action: String, mimeType: String): String? {
        val pm = context.packageManager
        val intent = Intent(action).apply { type = mimeType }
        val resolvedActivities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolvedActivities.firstOrNull { it.activityInfo.packageName == packageName }?.activityInfo?.name
    }

    private fun findServiceByIntent(packageName: String, expectedAction: String): String? {
        val pm = context.packageManager
        val intent = Intent(expectedAction)
        val resolvedServices = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolvedServices.firstOrNull { it.serviceInfo.packageName == packageName }?.serviceInfo?.name
    }

    private fun findBestMatch(items: List<String>, priorityMap: Map<List<String>, Int>, suffix: String): String? {
        val scores = mutableMapOf<String, Int>()
        for (item in items) {
            var score = 0
            for ((keywords, priority) in priorityMap) {
                if (keywords.all { item.contains(it, ignoreCase = true) }) {
                    score += priority
                }
            }
            if (item.contains(suffix, ignoreCase = true)) {
                score += 5 // Невеликий бонус, якщо назва містить "Activity" або "Service"
            }
            scores[item] = score
        }
        return scores.maxByOrNull { it.value }?.key ?: items.firstOrNull()
    }

    private val prioritizedCombinations = mapOf(
        listOf("Playback") to 50,
        listOf("Music", "Playback") to 40,
        listOf("Audio", "Playback") to 40,
        listOf("Song") to 25,
        listOf("Play") to 25,
        listOf("Music") to 20,
        listOf("Audio") to 20
    )
}

fun getMainActivityName(context: Context, packageName: String): String? {
    try {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        return intent?.component?.className
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}
