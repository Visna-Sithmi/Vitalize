package com.example.vitalize.ui.main

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vitalize.HomeActivity
import com.example.vitalize.R
import com.example.vitalize.StepsActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class HydrateActivity : AppCompatActivity() {

    private lateinit var etWaterAmount: EditText
    private lateinit var btnAddWater: MaterialButton
    private lateinit var tvTodayTotal: TextView
    private lateinit var barChart: BarChart
    private lateinit var calendarView: CalendarView
    private lateinit var rvTodayHistory: RecyclerView

    // Reminder
    private lateinit var etReminderMinutes: EditText
    private lateinit var btnSetReminder: MaterialButton
    private lateinit var btnStopAlarm: MaterialButton
    private lateinit var tvCountdown: TextView
    private lateinit var tvNextReminder: TextView
    private var countDownTimer: CountDownTimer? = null

    private val prefs by lazy { getSharedPreferences("HydratePrefs", MODE_PRIVATE) }
    private val history = mutableListOf<String>()

    private var todayTotal = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val todayDate = dateFormat.format(Date())

    private val PREF_REMINDER_END = "reminder_end_time"
    private val PREF_INTERVAL_MINUTES = "reminder_interval"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydrate)

        //  Status bar handling
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // 🔗 Views
        etWaterAmount = findViewById(R.id.etWaterAmount)
        btnAddWater = findViewById(R.id.btnAddWater)
        tvTodayTotal = findViewById(R.id.tvTodayTotal)
        barChart = findViewById(R.id.barChart)
        calendarView = findViewById(R.id.calendarView)
        rvTodayHistory = findViewById(R.id.rvTodayHistory)

        etReminderMinutes = findViewById(R.id.etReminderHours) // reuse id
        btnSetReminder = findViewById(R.id.btnSetReminder)
        btnStopAlarm = findViewById(R.id.btnStopAlarm)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvNextReminder = findViewById(R.id.tvNextReminder)

        rvTodayHistory.layoutManager = LinearLayoutManager(this)
        rvTodayHistory.adapter = HistoryAdapter(history)

        // Load data
        loadTodayTotal()
        loadTodayHistory()
        setupChart()

        // Restore countdown if reminder was running
        val savedEnd = prefs.getLong(PREF_REMINDER_END, 0L)
        if (savedEnd > System.currentTimeMillis()) {
            val remaining = savedEnd - System.currentTimeMillis()
            resumeCountdown(remaining)
            showNextReminder(savedEnd)
        } else {
            tvCountdown.text = "No reminder running"
        }

        //  Add water entry
        btnAddWater.setOnClickListener {
            val input = etWaterAmount.text.toString()
            if (input.isNotEmpty()) {
                val amount = input.toInt()
                todayTotal += amount
                saveTodayTotal(todayTotal)

                val entry = "$amount ml at ${timeFormat.format(Date())}"
                history.add(entry)
                saveTodayHistory(history)

                tvTodayTotal.text = getString(R.string.today_water, todayTotal)
                etWaterAmount.text.clear()
                rvTodayHistory.adapter?.notifyDataSetChanged()
                updateChart()
            } else {
                Toast.makeText(this, getString(R.string.enter_water), Toast.LENGTH_SHORT).show()
            }
        }

        //  Calendar view
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            if (selectedDate == todayDate) {
                Toast.makeText(this, "Showing today's history", Toast.LENGTH_SHORT).show()
            } else {
                val total = prefs.getInt(selectedDate, 0)
                AlertDialog.Builder(this)
                    .setTitle("Water Intake on $selectedDate")
                    .setMessage("Total: $total ml")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        //  Start Reminder
        btnSetReminder.setOnClickListener {
            val minutes = etReminderMinutes.text.toString().toIntOrNull()
            if (minutes != null && minutes > 0) {
                startRepeatingReminder(minutes)
                Toast.makeText(this, "Reminder every $minutes minutes", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
            }
        }

        //  Stop Alarm
        btnStopAlarm.setOnClickListener {
            stopAlarm()
            Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show()
        }

        //  Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_hydrate
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); true }
                R.id.nav_activity -> { startActivity(Intent(this, StepsActivity::class.java)); true }
                R.id.nav_hydrate -> true
                R.id.nav_moods -> { startActivity(Intent(this, MoodsActivity::class.java)); true }
                else -> false
            }
        }
    }

    // Save/Load totals
    private fun loadTodayTotal() {
        todayTotal = prefs.getInt(todayDate, 0)
        tvTodayTotal.text = getString(R.string.today_water, todayTotal)
    }
    private fun saveTodayTotal(amount: Int) {
        prefs.edit().putInt(todayDate, amount).apply()
    }

    private fun loadTodayHistory() {
        val historySet = prefs.getStringSet("${todayDate}_history", emptySet())?.toMutableSet() ?: mutableSetOf()
        history.clear()
        history.addAll(historySet)
        rvTodayHistory.adapter?.notifyDataSetChanged()
    }
    private fun saveTodayHistory(historyList: List<String>) {
        prefs.edit().putStringSet("${todayDate}_history", historyList.toSet()).apply()
    }

    // Chart
    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.setDrawGridLines(false)
        xAxis.granularity = 1f
        updateChart()
    }

    private fun updateChart() {
        val entries = ArrayList<BarEntry>()
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            val value = prefs.getInt(date, 0)
            entries.add(BarEntry((7 - i).toFloat(), value.toFloat()))
        }
        val dataSet = BarDataSet(entries, "Water (ml)")
        dataSet.color = getColor(R.color.primaryBlue)
        barChart.data = BarData(dataSet)
        barChart.invalidate()
    }

    //  Start repeating reminder with exact alarms
    private fun startRepeatingReminder(minutes: Int) {
        val intervalMillis = minutes * 60 * 1000L
        val firstTrigger = System.currentTimeMillis() + intervalMillis

        // Exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmMgr.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(this, "Enable exact alarms for reminders", Toast.LENGTH_LONG).show()
                return
            }
        }

        prefs.edit().putLong(PREF_REMINDER_END, firstTrigger).apply()
        prefs.edit().putInt(PREF_INTERVAL_MINUTES, minutes).apply()

        resumeCountdown(intervalMillis)
        showNextReminder(firstTrigger)

        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel previous alarms before scheduling new one
        alarmMgr.cancel(pendingIntent)

        alarmMgr.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            firstTrigger,
            pendingIntent
        )
    }

    // Resume countdown
    private fun resumeCountdown(millis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(ms: Long) {
                val h = (ms / 1000) / 3600
                val m = ((ms / 1000) % 3600) / 60
                val s = (ms / 1000) % 60
                tvCountdown.text = String.format("%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() {
                tvCountdown.text = "Waiting for alarm..."
            }
        }.start()
    }

    // Show next reminder time
    private fun showNextReminder(triggerTime: Long) {
        val timeStr = timeFormat.format(Date(triggerTime))
        tvNextReminder.text = "Next reminder at $timeStr"
    }

    //  Stop Alarm
    private fun stopAlarm() {
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.cancel(pendingIntent)

        countDownTimer?.cancel()
        tvCountdown.text = "Reminder stopped"
        tvNextReminder.text = ""

        ReminderReceiver.mediaPlayer?.stop()
        ReminderReceiver.mediaPlayer?.release()
        ReminderReceiver.mediaPlayer = null

        NotificationManagerCompat.from(this).cancelAll()
    }

    // Adapter
    inner class HistoryAdapter(private val items: List<String>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        inner class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                textSize = 16f
                setPadding(24, 16, 24, 16)
            }
            return ViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, pos: Int) { holder.textView.text = items[pos] }
        override fun getItemCount() = items.size
    }
}
