package com.mramfix.subtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId < 0) return
        if (ACTION_MARK_PAID == intent.action) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    companion object {
        const val ACTION_MARK_PAID = "com.mramfix.subtracker.action.MARK_PAID"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
