package com.qf.musicplayer.ui

import android.content.Intent
import android.content.ComponentName
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qf.musicplayer.R

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listView = findViewById(R.id.listView)  // Ідентифікатор вашого ListView

        val pm = packageManager

        // 1. Пошук аудіоплеєрів через CATEGORY_APP_MUSIC
        val musicIntent = Intent(Intent.ACTION_MAIN, null)
        musicIntent.addCategory(Intent.CATEGORY_APP_MUSIC)
        val musicPlayers: MutableList<ResolveInfo> = pm.queryIntentActivities(musicIntent, 0).toMutableList()

        // 2. Пошук сумісних програм через ACTION_VIEW з типом audio/*
        val audioIntent = Intent(Intent.ACTION_VIEW).apply {
            type = "audio/*"
        }
        val audioPlayers = pm.queryIntentActivities(audioIntent, 0)

        // Об'єднання списків, уникнення дублікатів
        for (res in audioPlayers) {
            // Перевірка, чи вже додано цей плеєр
            val alreadyAdded = musicPlayers.any {
                it.activityInfo.packageName == res.activityInfo.packageName &&
                        it.activityInfo.name == res.activityInfo.name
            }
            if (!alreadyAdded) {
                musicPlayers.add(res)
            }
        }

        // Формування списку назв знайдених плеєрів для відображення
        val playerNames = musicPlayers.map { it.loadLabel(pm).toString() }

        // Встановлення адаптера для ListView
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, playerNames)
        listView.adapter = adapter

        // Обробка вибору плеєра з списку
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedResolveInfo = musicPlayers[position]
            // Намір для запуску вибраного плеєра
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(
                    selectedResolveInfo.activityInfo.packageName,
                    selectedResolveInfo.activityInfo.name
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Не вдалося відкрити додаток", Toast.LENGTH_SHORT).show()
            }
        }
    }
}