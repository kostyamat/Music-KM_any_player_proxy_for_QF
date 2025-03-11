package com.qf.musicplayer.ui

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "CloneMusicPlayerPrefs"
    private const val KEY_SELECTED_PLAYER = "selected_player"

    fun savePlayer(context: Context, packageName: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_PLAYER, packageName).apply()
    }

    fun getSavedPlayer(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_PLAYER, null)
    }
}
