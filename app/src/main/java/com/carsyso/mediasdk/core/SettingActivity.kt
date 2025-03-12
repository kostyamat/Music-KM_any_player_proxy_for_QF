package com.carsyso.mediasdk.core

import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
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

    private fun findMediaPlayers(showAll: Boolean = false) {
        val players = mappingHelper.findMediaPlayers(showAll)
        if (players.isEmpty()) {
            noPlayersText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            noPlayersText.visibility = View.GONE
            listView.visibility = View.VISIBLE
            adapter.clear()
            adapter.addAll(players)
            adapter.notifyDataSetChanged()
        }
        showAllAppsButton.text = if (showAll) getString(R.string.show_media_players) else getString(R.string.show_all_apps)
        showingAllApps = showAll
        mappingHeader.visibility = View.GONE
        saveMappingButton.visibility = View.GONE
        ourComponentsLayout.visibility = View.GONE
    }

    private fun showAllApps() {
        findMediaPlayers(true)
    }

    private fun getPackageNameForPlayer(playerName: String): String? {
        return mappingHelper.getPackageNameForPlayer(playerName)
    }

    private fun showMappingInterface() {
        if (selectedPlayer == null || selectedPlayerPackageName == null) {
            return
        }
        val playerInfo = "$selectedPlayer\n($selectedPlayerPackageName)"
        selectedPlayerInfoTextView.text = playerInfo
        mappingHeader.visibility = View.VISIBLE
        ourComponentsLayout.visibility = View.VISIBLE
        saveMappingButton.visibility = View.VISIBLE

        val mainActivityName = mappingHelper.findMainActivity(selectedPlayerPackageName!!)
        val mainActivityList = mutableListOf<String>()
        mainActivityList.add(mainActivityName ?: "Not found")

        val mainActivityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mainActivityList
        )
        mainActivityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Відкладаємо встановлення адаптера до моменту, коли Spinner буде повністю готовий
        mainActivitySpinner.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mainActivitySpinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mainActivitySpinner.adapter = mainActivityAdapter
            }
        })
    }

    private fun saveMapping() {
        Toast.makeText(this, "Mapping saved", Toast.LENGTH_SHORT).show()
    }

    private fun showHelpDialog(messageResId: Int) {
        AlertDialog.Builder(this)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}