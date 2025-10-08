package com.example.vitalize

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.text.SimpleDateFormat
import java.util.*

class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var stepCount = 0

    // 🔹 Step detection params
    private var lastStepTime: Long = 0
    private val stepThreshold = 1.2f   // g-force threshold
    private val minStepInterval = 300  // ms between steps (avoid double counting)

    // 🔹 WakeLock to keep CPU running
    private var wakeLock: PowerManager.WakeLock? = null

    private val prefs by lazy { getSharedPreferences("StepsPrefs", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        createNotification()

        // Keep CPU alive even when screen is off
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vitalize:StepLock")
        wakeLock?.acquire()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            // SENSOR_DELAY_GAME gives smoother updates than SENSOR_DELAY_UI
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepCount = prefs.getInt(getTodayKey(), 0)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        saveTodaySteps()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val (x, y, z) = event.values
            val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gravity = 9.81f
            val normalized = magnitude / gravity

            val now = System.currentTimeMillis()
            if (normalized > stepThreshold && now - lastStepTime > minStepInterval) {
                stepCount++
                lastStepTime = now
                saveTodaySteps()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveTodaySteps() {
        prefs.edit().putInt(getTodayKey(), stepCount).apply()

        // 🔹 Send step updates instantly to UI
        val intent = Intent("STEP_UPDATE").putExtra("steps", stepCount)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun getTodayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private fun createNotification() {
        val channelId = "steps_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Steps Tracking", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Steps Tracker Running")
            .setContentText("Tracking your steps in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }
}
