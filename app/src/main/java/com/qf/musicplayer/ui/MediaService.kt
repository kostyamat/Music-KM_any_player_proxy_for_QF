package com.qf.musicplayer.ui

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaService : NotificationListenerService() {

    companion object {
        private const val TAG = "PcmMediaService"
        private var activeControllers = emptyList<MediaController>()

        fun isPlayerActive(packageName: String, context: Context): Boolean {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, MediaService::class.java)
            try {
                activeControllers = sessionManager.getActiveSessions(componentName)
                return activeControllers.any { it.packageName == packageName && it.playbackState?.state == PlaybackState.STATE_PLAYING }
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to get active sessions, permission missing?", e)
                return false
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Service Connected!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener Service Disconnected!")
    }
}