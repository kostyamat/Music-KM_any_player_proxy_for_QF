package com.carsyso.mediasdk.core

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.glance.visibility
import com.qf.musicplayer.R

class SettingActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noPlayersText: TextView
    private lateinit var showAllAppsButton: Button
    private lateinit var saveMappingButton: Button
    private lateinit var mappingHeader: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var mappingHelper: MappingHelper // Додаємо змінну для MappingHelper
    private lateinit var selectedPlayerInfoTextView: TextView

    private lateinit var ourComponentsLayout: LinearLayout

    private lateinit var mainActivityLabel: TextView
    private lateinit var pcmPlayerActivityLabel: TextView
    private lateinit var mediaServiceLabel: TextView

    private lateinit var mainActivitySpinner: Spinner
    private lateinit var pcmPlayerActivitySpinner: Spinner
    private lateinit var mediaServiceSpinner: Spinner

    private lateinit var helpMainActivity: ImageView
    private lateinit var helpPcmPlayerActivity: ImageView
    private lateinit var helpMediaService: ImageView

    private var selectedPlayer: String? = null
    private var selectedPlayerPackageName: String? = null
    private var showingAllApps = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        listView = findViewById(R.id.listView)
        noPlayersText = findViewById(R.id.noPlayersText)
        showAllAppsButton = findViewById(R.id.showAllAppsButton)
        saveMappingButton = findViewById(R.id.saveMappingButton)
        mappingHeader = findViewById(R.id.mappingHeader)
        selectedPlayerInfoTextView = findViewById(R.id.selectedPlayerInfo)
        // Перемістили цей рядок сюди
        selectedPlayerInfoTextView.text = getString(R.string.select_player)

        ourComponentsLayout = findViewById(R.id.ourComponentsLayout)

        mainActivityLabel = findViewById(R.id.mainActivityLabel)
        pcmPlayerActivityLabel = findViewById(R.id.pcmPlayerActivityLabel)
        mediaServiceLabel = findViewById(R.id.mediaServiceLabel)

        mainActivitySpinner = findViewById(R.id.mainActivitySpinner)
        pcmPlayerActivitySpinner = findViewById(R.id.pcmPlayerActivitySpinner)
        mediaServiceSpinner = findViewById(R.id.mediaServiceSpinner)

        helpMainActivity = findViewById(R.id.helpMainActivity)
        helpPcmPlayerActivity = findViewById(R.id.helpPcmPlayerActivity)
        helpMediaService = findViewById(R.id.helpMediaService)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        mappingHelper = MappingHelper(this) // Ініціалізуємо MappingHelper

        // Завжди показуємо інтерфейс вибору плеєра
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
            selectedPlayerPackageName = getPackageNameForPlayer(selectedPlayer)
            Toast.makeText(this, "Selected: $selectedPlayer, Package: $selectedPlayerPackageName", Toast.LENGTH_SHORT).show()
            showMappingInterface()
        }

        saveMappingButton.setOnClickListener {
            saveMapping()
        }

        // Додаємо обробники кліків для знаків питання
        helpMainActivity.setOnClickListener {
            showHelpDialog(R.string.help_main_activity)
        }

        helpPcmPlayerActivity.setOnClickListener {
            showHelpDialog(R.string.help_pcm_player_activity)
        }

        helpMediaService.setOnClickListener {
            showHelpDialog(R.string.help_media_service)
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

        // Використовуємо методи з MappingHelper
        val activities = mappingHelper.getActivitiesForPackage(selectedPlayerPackageName)
        val services = mappingHelper.getServicesForPackage(selectedPlayerPackageName)

        val activityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)
        val serviceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, services)

        mainActivitySpinner.adapter = activityAdapter
        pcmPlayerActivitySpinner.adapter = activityAdapter
        mediaServiceSpinner.adapter = serviceAdapter

        // Виводимо інформацію про плеєр
        val playerInfo = "Плеєр: ${selectedPlayer}\nПакет: ${selectedPlayerPackageName}"
        selectedPlayerInfoTextView.text = playerInfo
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

    // Використовуємо методи з MappingHelper
    private fun getPackageNameForPlayer(playerName: String?): String? {
        return mappingHelper.getPackageNameForPlayer(playerName)
    }

    private fun showHelpDialog(messageResId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Підказка")
            .setMessage(getString(messageResId))
            .setPositiveButton("OK", null)
            .show()
    }
}