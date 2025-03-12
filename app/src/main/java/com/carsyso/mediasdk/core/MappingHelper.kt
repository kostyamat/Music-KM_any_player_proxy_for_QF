package com.carsyso.mediasdk.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class MappingHelper(private val context: Context) {

    fun getPackageNameForPlayer(playerName: String?): String? {
        if (playerName == null) return null
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps = pm.queryIntentActivities(intent, 0)
        for (resolveInfo in installedApps) {
            if (resolveInfo.loadLabel(pm).toString() == playerName) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }

    fun getActivitiesForPackage(packageName: String?): List<String> {
        if (packageName == null) return emptyList()
        val pm= context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        return packageInfo.activities?.map { it.name.substringAfterLast(".").trim() }?.map { shortenName(it, packageName) } ?: emptyList()
    }

    fun getServicesForPackage(packageName: String?): List<String> {
        if (packageName == null) return emptyList()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        return packageInfo.services?.map { it.name.substringAfterLast(".").trim() }?.map { shortenName(it, packageName) } ?: emptyList()
    }

    private fun shortenName(name: String, packageName: String?): String {
        if (packageName == null) return name
        return if (name.startsWith(packageName)) {
            name.substring(packageName.length).trimStart('.')
        } else {
            name
        }
    }
}