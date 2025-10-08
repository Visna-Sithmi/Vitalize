package com.example.vitalize.ui.main

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.example.vitalize.R
import com.example.vitalize.utils.NotificationStore
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        private const val PREF_INTERVAL_MINUTES = "reminder_interval"
        private const val PREF_REMINDER_END = "reminder_end_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
       //load shared pref
        val prefs = context.getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE)

        val channelId = "hydrate_reminder_channel"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = "Time to drink water! 💧 ($time)"

        // Save notification log + badge
        val logs = prefs.getStringSet("notifications", mutableSetOf())!!.toMutableSet()
        logs.add(message)
        prefs.edit {
            putStringSet("notifications", logs)
            putInt("notification_count", prefs.getInt("notification_count", 0) + 1)
        }
        NotificationStore.addNotification(context, message)

        //  Create NotificationChannel (Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders to drink water" }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        //  Start alarm sound (loop until stopped)
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(
                context,
                Settings.System.DEFAULT_ALARM_ALERT_URI
            )
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        //  Stop action in notification
        val stopIntent = Intent(context, StopAlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  Full-screen AlarmActivity intent
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitName", "Hydration Reminder")
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  Build notification with full-screen intent
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Hydration Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 🚀 must be MAX
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true) // stays until user stops
            .addAction(R.drawable.ic_cansel, "Stop Alarm", stopPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }

        //  DO NOT call startActivity() directly here – let fullScreenIntent handle it

        //  Reschedule next alarm if interval is set
        val intervalMinutes = prefs.getInt(PREF_INTERVAL_MINUTES, 0)
        if (intervalMinutes > 0) {
            val intervalMillis = intervalMinutes * 60 * 1000L
            val nextTrigger = System.currentTimeMillis() + intervalMillis

            prefs.edit { putLong(PREF_REMINDER_END, nextTrigger) }

            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, ReminderReceiver::class.java)
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 0, nextIntent,
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
                    val intentSettings =
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    context.startActivity(intentSettings)
                }
            } else {
                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger,
                    nextPendingIntent
                )
            }
        }
    }
}
