package com.qf.musicplayer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.qf.musicplayer.R
import android.content.pm.ApplicationInfo


class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noPlayersText: TextView
    private lateinit var showAllAppsButton: Button
    private var showingAllApps = false
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        noPlayersText = findViewById(R.id.noPlayersText)
        showAllAppsButton = findViewById(R.id.showAllAppsButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        findMediaPlayers()

        showAllAppsButton.setOnClickListener {
            if (showingAllApps) {
                findMediaPlayers()
            } else {
                showAllApps()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedApp = adapter.getItem(position)
            Toast.makeText(this, "Selected: $selectedApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findMediaPlayers() {
        val pm = packageManager
        val mediaIntent = Intent(Intent.ACTION_VIEW).apply { type = "audio/*" }
        val musicIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }

        val mediaPlayers = pm.queryIntentActivities(mediaIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val musicPlayers = pm.queryIntentActivities(musicIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val allPlayers = (mediaPlayers + musicPlayers).distinctBy { it.activityInfo.packageName }
        val playerNames = allPlayers.map { it.loadLabel(pm).toString() }

        if (playerNames.isNotEmpty()) {
            adapter.clear()
            adapter.addAll(playerNames)
            adapter.notifyDataSetChanged()
            listView.visibility = View.VISIBLE
            noPlayersText.visibility = View.GONE
        } else {
            listView.visibility = View.GONE
            noPlayersText.visibility = View.VISIBLE
        }

        showAllAppsButton.text = getString(R.string.show_all_apps)
        showingAllApps = false
    }

    private fun showAllApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps = pm.queryIntentActivities(intent, 0)

        val userApps = installedApps.filter {
            val appInfo = it.activityInfo.applicationInfo
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 // Відсіюємо системні
        }

        val appNames = userApps.map { it.loadLabel(pm).toString() }

        if (appNames.isNotEmpty()) {
            adapter.clear()
            adapter.addAll(appNames)
            adapter.notifyDataSetChanged()
            listView.visibility = View.VISIBLE
            noPlayersText.visibility = View.GONE
        } else {
            listView.visibility = View.GONE
            noPlayersText.visibility = View.VISIBLE
        }

        showAllAppsButton.text = getString(R.string.show_media_players)
        showingAllApps = true
    }


    override fun onBackPressed() {
        if (showingAllApps) {
            findMediaPlayers()
        } else {
            super.onBackPressed()
        }
    }
}