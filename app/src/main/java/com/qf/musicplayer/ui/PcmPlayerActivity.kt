package com.qf.musicplayer.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class PcmPlayerActivity : Activity() {

    companion object {
        private const val TAG = "PcmProxyStub"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Redirecting to MainActivity.")

        val intentToMain = Intent(this, MainActivity::class.java)
        intentToMain.action = intent.action
        intentToMain.data = intent.data
        if (intent.extras != null) {
            intentToMain.putExtras(intent.extras!!)
        }

        intentToMain.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intentToMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intentToMain)

        // Одразу закриваємо, щоб не висіло в стеку
        finish()
    }
}