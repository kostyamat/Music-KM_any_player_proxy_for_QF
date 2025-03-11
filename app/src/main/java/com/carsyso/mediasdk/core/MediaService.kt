package com.carsyso.mediasdk.core

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Фейковий сервіс, який замінює оригінальний медіасервіс.
 * Він буде отримувати інтенти і перенаправляти їх до вибраного плеєра.
 */
class MediaService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        // Цей сервіс не підтримує прив'язку, тому повертаємо null.
        return null
    }
}
