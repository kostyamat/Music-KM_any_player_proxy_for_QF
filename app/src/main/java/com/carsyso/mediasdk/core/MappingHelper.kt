package com.carsyso.mediasdk.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log

class MappingHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun findMediaPlayers(showAll: Boolean = false): List<String> {
        val mediaIntent = Intent(Intent.ACTION_VIEW).apply { type = "audio/*" }
        val musicIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }

        val mediaPlayers = packageManager.queryIntentActivities(mediaIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val musicPlayers = packageManager.queryIntentActivities(musicIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val allPlayers = (mediaPlayers + musicPlayers).distinctBy { it.activityInfo.packageName }
        val playerNames = allPlayers.map { it.loadLabel(packageManager).toString() }

        if (showAll) {
            val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val allAppNames = allApps.map { it.loadLabel(packageManager).toString() }
            return allAppNames
        } else {
            return playerNames
        }
    }

    fun getPackageNameForPlayer(playerName: String): String? {
        val mediaIntent = Intent(Intent.ACTION_VIEW).apply { type = "audio/*" }
        val musicIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }

        val mediaPlayers = packageManager.queryIntentActivities(mediaIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val musicPlayers = packageManager.queryIntentActivities(musicIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val allPlayers = (mediaPlayers + musicPlayers).distinctBy { it.activityInfo.packageName }

        return allPlayers.find { it.loadLabel(packageManager).toString() == playerName }?.activityInfo?.packageName
    }

    fun findMainActivity(packageName: String): String? {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfo: ResolveInfo? = packageManager.resolveActivity(launchIntent, 0)
        return resolveInfo?.activityInfo?.name
    }
}