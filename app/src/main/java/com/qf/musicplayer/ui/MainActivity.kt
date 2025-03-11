package com.qf.musicplayer.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.qf.musicplayer.R
import android.content.SharedPreferences
import com.carsyso.mediasdk.core.SettingActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting) // Створити пустий layout
        sharedPreferences = getSharedPreferences("player_mapping", MODE_PRIVATE)
        val isMappingSaved = sharedPreferences.getBoolean("is_mapping_saved", false)
        if (!isMappingSaved) {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
        // Тут буде логіка проксі
    }
}