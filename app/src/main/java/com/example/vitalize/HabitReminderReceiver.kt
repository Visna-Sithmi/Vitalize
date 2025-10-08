package com.example.vitalize.ui.main

import android.app.*
import android.content.*
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vitalize.R
import com.example.vitalize.utils.NotificationStore
import java.text.SimpleDateFormat
import java.util.*

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val channelId = "habit_reminder_channel"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = "⏰ Time to do: $habitName ($time)"

        // ✅ Store log for HomeActivity
        NotificationStore.addNotification(context, message)

        // ✅ Increment badge counter
        val prefs = context.getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("notification_count", 0) + 1
        prefs.edit().putInt("notification_count", count).apply()
        context.sendBroadcast(Intent("UPDATE_NOTIFICATION_BADGE"))

        // ✅ Create notification channel (Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        // ✅ Start alarm sound
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(
                context,
                Settings.System.DEFAULT_ALARM_ALERT_URI
            )
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        // ✅ Stop action
        val stopIntent = Intent(context, HabitStopAlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Full-screen activity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitName", habitName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Build system notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Habit Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(R.drawable.ic_cansel, "Stop Alarm", stopPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
