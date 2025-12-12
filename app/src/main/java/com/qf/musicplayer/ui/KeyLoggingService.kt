package com.qf.musicplayer.ui

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyLoggingService : AccessibilityService() {

    companion object {
        private const val TAG = "PcmKeyLoggingService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "Key pressed: ${event.keyCode}")
        }
        return false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Key Logging Service Connected!")
    }
}