package com.qf.musicplayer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.glance.visibility
import com.qf.musicplayer.R

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noPlayersText: TextView
    private lateinit var showAllAppsButton: Button
    private lateinit var saveMappingButton: Button
    private lateinit var mappingHeader: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var ourComponentsLayout: LinearLayout

    private lateinit var mainActivityLabel: TextView
    private lateinit var pcmPlayerActivityLabel: TextView
    private lateinit var mediaServiceLabel: TextView

    private lateinit var mainActivitySpinner: Spinner
    private lateinit var pcmPlayerActivitySpinner: Spinner
    private lateinit var mediaServiceSpinner: Spinner

    private var selectedPlayer: String? = null
    private var selectedPlayerPackageName: String? = null
    private var showingAllApps = false
    private var isMappingSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        noPlayersText = findViewById(R.id.noPlayersText)
        showAllAppsButton = findViewById(R.id.showAllAppsButton)
        saveMappingButton = findViewById(R.id.saveMappingButton)
        mappingHeader = findViewById(R.id.mappingHeader)

        ourComponentsLayout = findViewById(R.id.ourComponentsLayout)

        mainActivityLabel = findViewById(R.id.mainActivityLabel)
        pcmPlayerActivityLabel = findViewById(R.id.pcmPlayerActivityLabel)
        mediaServiceLabel = findViewById(R.id.mediaServiceLabel)

        mainActivitySpinner = findViewById(R.id.mainActivitySpinner)
        pcmPlayerActivitySpinner = findViewById(R.id.pcmPlayerActivitySpinner)
        mediaServiceSpinner = findViewById(R.id.mediaServiceSpinner)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        isMappingSaved = sharedPreferences.getBoolean("is_mapping_saved", false)

        if (isMappingSaved) {
            // Мапінг збережено, переходимо до основного завдання
            Toast.makeText(this, "Mapping is saved, go to main task", Toast.LENGTH_SHORT).show()
            // Тут буде код для переходу до основного завдання
            finish()
        } else {
            // Мапінг не збережено, показуємо інтерфейс вибору плеєра
            findMediaPlayers()
        }

        showAllAppsButton.setOnClickListener {
            if (showingAllApps) {
                findMediaPlayers()
            } else {
                showAllApps()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedPlayer = adapter.getItem(position)
            selectedPlayerPackageName = getPackageNameForPlayer(selectedPlayer)
            Toast.makeText(this, "Selected: $selectedPlayer, Package: $selectedPlayerPackageName", Toast.LENGTH_SHORT).show()
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
        showAllAppsButton.visibility = View.VISIBLE
        showingAllApps = false
        mappingHeader.visibility = View.GONE
        saveMappingButton.visibility = View.GONE
        ourComponentsLayout.visibility = View.GONE
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
        showAllAppsButton.visibility = View.VISIBLE
        showingAllApps = true
        mappingHeader.visibility = View.GONE
        saveMappingButton.visibility = View.GONE
        ourComponentsLayout.visibility = View.GONE
    }

    private fun showMappingInterface() {
        listView.visibility = View.GONE
        noPlayersText.visibility = View.GONE
        showAllAppsButton.visibility = View.GONE

        mappingHeader.visibility = View.VISIBLE
        saveMappingButton.visibility = View.VISIBLE
        ourComponentsLayout.visibility = View.VISIBLE

        val activities = getActivitiesForPackage(selectedPlayerPackageName)
        val services = getServicesForPackage(selectedPlayerPackageName)

        val activityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)
        val serviceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, services)

        mainActivitySpinner.adapter = activityAdapter
        pcmPlayerActivitySpinner.adapter = activityAdapter
        mediaServiceSpinner.adapter = serviceAdapter
    }

    private fun saveMapping() {
        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        sharedPreferences.edit().putString("mapped_player", selectedPlayer)
            .putBoolean("is_mapping_saved", true).apply()
        Toast.makeText(this, "Mapping saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onBackPressed() {
        if (showingAllApps) {
            findMediaPlayers()
        } else if (mappingHeader.visibility == View.VISIBLE) {
            findMediaPlayers()
            showAllAppsButton.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

    private fun getPackageNameForPlayer(playerName: String?): String? {
        if (playerName == null) return null
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedApps = pm.queryIntentActivities(intent, 0)
        for (resolveInfo in installedApps) {
            if (resolveInfo.loadLabel(pm).toString() == playerName) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }

    private fun getActivitiesForPackage(packageName: String?): List<String> {
        if (packageName == null) return emptyList()
        val pm = packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        return packageInfo.activities?.map { it.name.substringAfterLast(".") }?.map { shortenName(it, packageName) } ?: emptyList()
    }

    private fun getServicesForPackage(packageName: String?): List<String> {
        if (packageName == null) return emptyList()
        val pm = packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        return packageInfo.services?.map { it.name.substringAfterLast(".") }?.map { shortenName(it, packageName) } ?: emptyList()
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