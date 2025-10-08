package com.example.vitalize

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vitalize.ui.main.HabitsActivity
import com.example.vitalize.ui.main.HydrateActivity
import com.example.vitalize.ui.main.MoodsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.text.SimpleDateFormat
import java.util.*

class StepsActivity : AppCompatActivity() {

    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var barChart: BarChart
    private lateinit var calendarView: CalendarView
    private lateinit var bottomNavigation: BottomNavigationView

    private val prefs by lazy { getSharedPreferences("StepsPrefs", Context.MODE_PRIVATE) }

    //  Receiver for step updates
    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STEP_UPDATE") {
                val steps = intent.getIntExtra("steps", prefs.getInt(getTodayKey(), 0))
                updateStepViews(steps)
                updateChart()
            }
        }
    }

    //  Permissions
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startStepServiceNow()
            else Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
        }

    private val activityPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startStepServiceNow()
            else Toast.makeText(this, "Activity recognition permission required", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)

        // Transparent status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.TRANSPARENT
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // UI refs
        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        barChart = findViewById(R.id.barChart)
        calendarView = findViewById(R.id.calendarView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        updateUI()
        setupChart()
        updateChart()

        //  Toggle Start/Stop buttons
        btnStart.setOnClickListener {
            startStepServiceWithPermission()
            prefs.edit().putBoolean("isServiceRunning", true).apply()   // Save running state
            updateButtonStates(true)
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, StepService::class.java))
            prefs.edit().putBoolean("isServiceRunning", false).apply()  // Save stopped state
            updateButtonStates(false)
        }

        //  Calendar popup
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
            val steps = prefs.getInt(key, 0)
            val kcal = calculateCalories(steps)

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.steps_on_date, dayOfMonth, month + 1, year))
                .setMessage("👣 $steps ${getString(R.string.steps)}\n🔥 $kcal ${getString(R.string.kcal)}")
                .setPositiveButton("OK", null)
                .show()
        }

        // Bottom Nav
        bottomNavigation.selectedItemId = R.id.nav_activity
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); true }
                R.id.nav_moods -> { startActivity(Intent(this, MoodsActivity::class.java)); true }
                R.id.nav_hydrate -> { startActivity(Intent(this, HydrateActivity::class.java)); true }
                R.id.nav_activity -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        updateChart()

        //  Restore button states when returning to screen
        val isRunning = prefs.getBoolean("isServiceRunning", false)
        updateButtonStates(isRunning)

        val filter = IntentFilter("STEP_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(stepReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver)
    }

    // ---- Helpers ----
    private fun updateButtonStates(isRunning: Boolean) {
        btnStart.isEnabled = !isRunning
        btnStop.isEnabled = isRunning
    }

    private fun startStepServiceWithPermission() {
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startStepServiceNow()
    }

    private fun startStepServiceNow() {
        val intent = Intent(this, StepService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun updateUI() {
        val steps = prefs.getInt(getTodayKey(), 0)
        updateStepViews(steps)
    }

    private fun updateStepViews(steps: Int) {
        tvSteps.text = getString(R.string.steps_format, steps)
        tvCalories.text = getString(R.string.calories_format, calculateCalories(steps))
    }

    private fun calculateCalories(steps: Int): Int {
        val caloriesPerStep = 0.04
        return (steps * caloriesPerStep).toInt()
    }

    private fun getTodayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    //chart
    private fun setupChart() {
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.description = Description().apply { text = "" }
        barChart.setFitBars(true)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
    }

    private fun updateChart() {
        val entries = ArrayList<BarEntry>()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateKey = sdf.format(cal.time)
            val steps = prefs.getInt(dateKey, 0)
            val kcal = calculateCalories(steps)
            entries.add(BarEntry((7 - i).toFloat(), kcal.toFloat()))
        }

        val dataSet = BarDataSet(entries, getString(R.string.calories_label))
        dataSet.color = ContextCompat.getColor(this, R.color.primaryBlue)
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data
        barChart.invalidate()
    }
}
