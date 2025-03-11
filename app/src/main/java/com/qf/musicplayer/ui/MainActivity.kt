package com.qf.musicplayer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.qf.musicplayer.R

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noPlayersText: TextView
    private lateinit var showAllAppsButton: Button
    private lateinit var saveMappingButton: Button
    private lateinit var mappingHeader: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: ArrayAdapter<String>

    private var selectedPlayer: String? = null
    private var showingAllApps = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        noPlayersText = findViewById(R.id.noPlayersText)
        showAllAppsButton = findViewById(R.id.showAllAppsButton)
        saveMappingButton = findViewById(R.id.saveMappingButton)
        mappingHeader = findViewById(R.id.mappingHeader)

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
            selectedPlayer = adapter.getItem(position)
            Toast.makeText(this, "Selected: $selectedPlayer", Toast.LENGTH_SHORT).show()

            // Перехід до інтерфейсу мапінгу
            showMappingInterface()
        }

        saveMappingButton.setOnClickListener {
            saveMapping()
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
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
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

    private fun showMappingInterface() {
        listView.visibility = View.GONE
        noPlayersText.visibility = View.GONE
        showAllAppsButton.visibility = View.GONE

        mappingHeader.visibility = View.VISIBLE
        saveMappingButton.visibility = View.VISIBLE
    }

    private fun saveMapping() {
        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        sharedPreferences.edit().putString("mapped_player", selectedPlayer).apply()
        Toast.makeText(this, "Mapping saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onBackPressed() {
        if (showingAllApps) {
            findMediaPlayers()
        } else {
            super.onBackPressed()
        }
    }
}
