package com.qf.musicplayer.ui

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MediaService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("MusicProxyService", "System tried to bind MediaService. Returning null.")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}