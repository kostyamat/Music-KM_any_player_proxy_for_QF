package com.qf.musicplayer.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import com.qf.musicplayer.ui.R

class SettingsActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxySettings"
        // Use the same preference file as MainActivity to share settings
        private const val PREFS_NAME = "PlayerProxyPrefs"
        private const val PREF_TARGET_PACKAGE = "targetPackage"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        showPlayerSelectionDialog()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun showPlayerSelectionDialog() {
        val musicApps = getInstalledMusicPlayers()
        if (musicApps.isEmpty()) {
            // Handle case where no music players are installed
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.player_not_found_title))
                .setMessage(getString(R.string.player_not_found_message))
                .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
            return
        }

        val appNames = musicApps.map { it.loadLabel(packageManager) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_player_title))
            .setItems(appNames) { _, which ->
                val selectedPackage = musicApps[which].activityInfo.packageName
                Log.d(TAG, "Player selected: $selectedPackage")
                prefs.edit().putString(PREF_TARGET_PACKAGE, selectedPackage).apply()
                finish() // Close settings after selection
            }
            .setOnCancelListener {
                Log.d(TAG, "Player selection cancelled.")
                finish()
            }
            .show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledMusicPlayers(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        return packageManager.queryIntentActivities(intent, 0)
    }
}
