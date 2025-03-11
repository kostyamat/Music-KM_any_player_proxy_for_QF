package com.qf.musicplayer.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qf.musicplayer.R

/**
 * MappingActivity відображає простий інтерфейс співставлення компонентів,
 * де користувач може побачити список компонентів (зараз статично заданих)
 * та натиснути кнопку для збереження налаштувань.
 */
class MappingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapping)

        val lvMapping = findViewById<ListView>(R.id.lvMapping)
        // Для прикладу, статично задамо назви компонентів, які треба співставити
        val mappingItems = listOf("MainActivity", "PcmPlayerActivity", "MediaService")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mappingItems)
        lvMapping.adapter = adapter

        val btnSaveMapping = findViewById<Button>(R.id.btnSaveMapping)
        btnSaveMapping.setOnClickListener {
            Toast.makeText(this, "Налаштування збережено!", Toast.LENGTH_SHORT).show()
            // Тут можна додати логіку збереження та переходу далі
        }
    }
}
