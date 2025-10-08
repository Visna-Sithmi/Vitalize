package com.example.vitalize.ui.main

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.vitalize.R

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Force show above lock screen and wake device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setContentView(R.layout.activity_alarm)

        val habitName = intent.getStringExtra("habitName") ?: "Hydration Reminder"
        val motivation = intent.getStringExtra("motivation") ?: "Stay healthy and consistent ✨"

        val tvHabitName = findViewById<TextView>(R.id.tvHabitName)
        val tvMotivation = findViewById<TextView>(R.id.tvMotivation)
        val btnStopAlarm = findViewById<Button>(R.id.btnStopAlarm)

        // Set texts dynamically
        tvHabitName.text = "⏰ $habitName"
        tvMotivation.text = motivation

        // ✅ Stop button action
        btnStopAlarm.setOnClickListener {
            // 1️⃣ Stop both MediaPlayers safely
            try {
                ReminderReceiver.mediaPlayer?.stop()
                ReminderReceiver.mediaPlayer?.release()
                ReminderReceiver.mediaPlayer = null
            } catch (_: Exception) {}

            try {
                HabitReminderReceiver.mediaPlayer?.stop()
                HabitReminderReceiver.mediaPlayer?.release()
                HabitReminderReceiver.mediaPlayer = null
            } catch (_: Exception) {}

            // 2️⃣ Cancel all notifications
            NotificationManagerCompat.from(this).cancelAll()

            // 3️⃣ Send broadcast so receivers can log "Alarm stopped"
            sendBroadcast(android.content.Intent(this, StopAlarmReceiver::class.java))
            sendBroadcast(android.content.Intent(this, HabitStopAlarmReceiver::class.java))

            // 4️⃣ Close activity
            finish()
        }
    }
}
