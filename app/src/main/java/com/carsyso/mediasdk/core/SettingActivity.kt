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
import com.qf.musicplayer.R
import android.util.Log
import androidx.compose.ui.semantics.text
import androidx.glance.visibility

class SettingActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noPlayersText: TextView
    private lateinit var showAllAppsButton: Button
    private lateinit var saveMappingButton: Button
    private lateinit var mappingHeader: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var mappingHelper: MappingHelper
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

    private var recommendedMainActivity: String? = null
    private var recommendedPcmPlayerActivity: String? = null
    private var recommendedMediaService: String? = null

    private val TAG = "SettingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        listView = findViewById(R.id.listView)
        noPlayersText = findViewById(R.id.noPlayersText)
        showAllAppsButton = findViewById(R.id.showAllAppsButton)
        saveMappingButton = findViewById(R.id.saveMappingButton)
        mappingHeader = findViewById(R.id.mappingHeader)
        selectedPlayerInfoTextView = findViewById(R.id.selectedPlayerInfo)
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
        mappingHelper = MappingHelper(this)

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
            Log.d(TAG, "User selected player: $selectedPlayer, Package: $selectedPlayerPackageName")
            Toast.makeText(this, "Selected: $selectedPlayer, Package: $selectedPlayerPackageName", Toast.LENGTH_SHORT).show()
            showMappingInterface()
        }

        saveMappingButton.setOnClickListener {
            saveMapping()
        }

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

        // Отримуємо пакетне ім'я нашої програми-клона
        val ourPackageName = packageName

        // Фільтруємо список, виключаючи нашу програму-клон
        val filteredPlayers = allPlayers.filter { it.activityInfo.packageName != ourPackageName }

        val playerNames = filteredPlayers.map { it.loadLabel(pm).toString() }

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

        // Отримуємо пакетне ім'я нашої програми-клона
        val ourPackageName = packageName

        // Фільтруємо список, виключаючи нашу програму-клон
        val filteredApps = installedApps.filter { it.activityInfo.packageName != ourPackageName }

        val userApps = filteredApps.filter {
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

        // MainActivity
        recommendedMainActivity = mappingHelper.findRecommendedMainActivity(selectedPlayerPackageName)
        val allActivities = mappingHelper.getActivitiesForPackage(selectedPlayerPackageName)
        val uniqueMainActivities = mutableListOf<String>()
        uniqueMainActivities.add("Вибрати вручну")
        if (recommendedMainActivity != null) {
            uniqueMainActivities.add(recommendedMainActivity!!)
        }
        uniqueMainActivities.addAll(allActivities.filter { it != recommendedMainActivity })
        val mainActivityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uniqueMainActivities)
        mainActivitySpinner.adapter = mainActivityAdapter
        val recommendedMainActivityIndex = uniqueMainActivities.indexOf(recommendedMainActivity)
        if (recommendedMainActivityIndex > 0) {
            mainActivitySpinner.setSelection(recommendedMainActivityIndex)
            Log.d(TAG, "showMappingInterface(): MainActivity set automatically: $recommendedMainActivity")
        }
        Log.d(TAG, "showMappingInterface(): All Activities: $allActivities")

        mainActivitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedActivity = parent.getItemAtPosition(position).toString()
                val isAutomatic = selectedActivity == recommendedMainActivity
                Log.d(TAG, "User selected MainActivity: $selectedActivity (Automatic: $isAutomatic)")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // PcmPlayerActivity
        recommendedPcmPlayerActivity = mappingHelper.findRecommendedPcmPlayerActivity(selectedPlayerPackageName)
        val uniquePcmActivities = mutableListOf<String>()
        uniquePcmActivities.add("Вибрати вручну")
        if (recommendedPcmPlayerActivity != null) {
            uniquePcmActivities.add(recommendedPcmPlayerActivity!!)
        }
        uniquePcmActivities.addAll(allActivities.filter { it != recommendedPcmPlayerActivity })
        val pcmActivityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uniquePcmActivities)
        pcmPlayerActivitySpinner.adapter = pcmActivityAdapter
        val recommendedPcmPlayerActivityIndex = uniquePcmActivities.indexOf(recommendedPcmPlayerActivity)
        if (recommendedPcmPlayerActivityIndex > 0) {
            pcmPlayerActivitySpinner.setSelection(recommendedPcmPlayerActivityIndex)
            Log.d(TAG, "showMappingInterface(): PcmPlayerActivity set automatically: $recommendedPcmPlayerActivity")
        }
        Log.d(TAG, "showMappingInterface(): All Pcm Activities: $uniquePcmActivities")

        pcmPlayerActivitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedActivity = parent.getItemAtPosition(position).toString()
                val isAutomatic = selectedActivity == recommendedPcmPlayerActivity
                Log.d(TAG, "User selected PcmPlayerActivity: $selectedActivity (Automatic: $isAutomatic)")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // MediaService
        recommendedMediaService = mappingHelper.findRecommendedMediaService(selectedPlayerPackageName)
        val allMediaServices = mappingHelper.getServicesForPackage(selectedPlayerPackageName)
        val uniqueMediaServices = mutableListOf<String>()
        uniqueMediaServices.add("Вибрати вручну")
        if (recommendedMediaService != null) {
            uniqueMediaServices.add(recommendedMediaService!!)
        }
        uniqueMediaServices.addAll(allMediaServices.filter { it != recommendedMediaService })
        val serviceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uniqueMediaServices)
        mediaServiceSpinner.adapter = serviceAdapter
        val recommendedMediaServiceIndex = uniqueMediaServices.indexOf(recommendedMediaService)
        if (recommendedMediaServiceIndex > 0) {
            mediaServiceSpinner.setSelection(recommendedMediaServiceIndex)
            Log.d(TAG, "showMappingInterface(): MediaService set automatically: $recommendedMediaService")
        }
        Log.d(TAG, "showMappingInterface(): All Media Services: $uniqueMediaServices")

        mediaServiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedService = parent.getItemAtPosition(position).toString()
                val isAutomatic = selectedService == recommendedMediaService
                Log.d(TAG, "User selected MediaService: $selectedService (Automatic: $isAutomatic)")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        val playerInfo = "Плеєр: ${selectedPlayer}\nПакет: ${selectedPlayerPackageName}"
        selectedPlayerInfoTextView.text = playerInfo
    }

    private fun saveMapping() {
        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        sharedPreferences.edit().putString("mapped_player", selectedPlayer)
            .putBoolean("is_mapping_saved", true).apply()
        Log.d(TAG, "saveMapping(): Mapping saved for player: $selectedPlayer")
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