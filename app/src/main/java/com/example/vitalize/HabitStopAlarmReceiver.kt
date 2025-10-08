package com.example.vitalize.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class HabitStopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        HabitReminderReceiver.mediaPlayer?.stop()
        HabitReminderReceiver.mediaPlayer?.release()
        HabitReminderReceiver.mediaPlayer = null

        // Cancel any active notifications
        NotificationManagerCompat.from(context).cancelAll()


    }
}
