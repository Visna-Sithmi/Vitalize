package com.example.vitalize.ui.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.vitalize.utils.NotificationStore
import java.text.SimpleDateFormat
import java.util.*

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ✅ Stop alarm sound
        ReminderReceiver.mediaPlayer?.stop()
        ReminderReceiver.mediaPlayer?.release()
        ReminderReceiver.mediaPlayer = null

        // ✅ Cancel notifications
        NotificationManagerCompat.from(context).cancelAll()


        // ✅ Get saved interval
        val prefs = context.getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("reminder_interval", 0)

        if (intervalMinutes > 0) {
            val nextTrigger = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)
            prefs.edit().putLong("reminder_end_time", nextTrigger).apply()

            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, ReminderReceiver::class.java)
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmMgr.canScheduleExactAlarms()) {
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTrigger,
                        nextPendingIntent
                    )
                } else {
                    alarmMgr.set(
                        AlarmManager.RTC_WAKEUP,
                        nextTrigger,
                        nextPendingIntent
                    )
                }
            } else {
                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger,
                    nextPendingIntent
                )
            }

            // ✅ Tell HydrateActivity to restart countdown
            val restartIntent = Intent("RESTART_COUNTDOWN")
            restartIntent.putExtra("nextTrigger", nextTrigger)
            context.sendBroadcast(restartIntent)
        }
    }
}
