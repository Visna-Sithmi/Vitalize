package com.example.vitalize

import android.content.*
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vitalize.ui.main.*
import com.example.vitalize.utils.NotificationStore
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var monthYearTv: TextView
    private lateinit var calendarDaysContainer: LinearLayout
    private var calendar: Calendar = Calendar.getInstance()

    private lateinit var notificationIcon: ImageView
    private lateinit var notificationBadge: TextView

    private lateinit var chartHabits: PieChart
    private lateinit var tvHabitPercent: TextView
    private lateinit var chartHydration: PieChart
    private lateinit var tvHydrationPercent: TextView
    private lateinit var tvTodayMood: TextView
    private lateinit var tvMoodName: TextView
    private lateinit var tvStepsHome: TextView
    private lateinit var tvKcalHome: TextView
//shared pref
    private val prefsSteps by lazy { getSharedPreferences("StepsPrefs", MODE_PRIVATE) }
    private val prefsHydrate by lazy { getSharedPreferences("HydratePrefs", MODE_PRIVATE) }
    private val prefs by lazy { getSharedPreferences("VitalizePrefs", MODE_PRIVATE) }

    private val hydrationTarget = 3000 // ml target

    // 🔹 Badge update receiver
    private val badgeUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNotificationBadge()
        }
    }

    //  Step update receiver
    private val stepsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STEP_UPDATE") {
                val steps = intent.getIntExtra("steps", prefsSteps.getInt(getTodayKey(), 0))
                updateStepViews(steps)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = prefs.getString("name", "User")
        val userNameTv = findViewById<TextView>(R.id.userNameTv)
        userNameTv.text = "Hello, $userName"

        //  Drawer setup
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val menu = navigationView.menu
        val logoutItem = menu.findItem(R.id.nav_logout)
        logoutItem.icon?.setTint(getColor(R.color.logoutRed))
        logoutItem.title = Html.fromHtml(
            "<font color='${resources.getColor(R.color.logoutRed, theme)}'>Logout</font>",
            Html.FROM_HTML_MODE_LEGACY
        )
        userNameTv.setOnClickListener { drawerLayout.openDrawer(navigationView) }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    prefs.edit().putBoolean("isLoggedIn", false).apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                R.id.nav_notifications -> { //  handle Notification Manager click
                    showManageNotificationDialog()
                }

                R.id.nav_profile_settings -> {

                    startActivity(Intent(this, ProfileActivity::class.java))
                }

            }
            true
        }

        // Set Sidebar Header Username
        val headerView = navigationView.getHeaderView(0)
        val tvUserNameHeader = headerView.findViewById<TextView>(R.id.tvUserNameHeader)
        tvUserNameHeader.text = userName

        // 🔹 Calendar
        monthYearTv = findViewById(R.id.monthYearTv)
        calendarDaysContainer = findViewById(R.id.calendarDaysContainer)
        findViewById<ImageView>(R.id.prevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1); updateCalendar()
        }
        findViewById<ImageView>(R.id.nextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1); updateCalendar()
        }
        updateCalendar()

        // 🔹 Notifications (bell badge)
        notificationIcon = findViewById(R.id.notificationIcon)
        notificationBadge = findViewById(R.id.notificationBadge)
        updateNotificationBadge()
        notificationIcon.setOnClickListener { showNotificationsDialog() }

        // 🔹 Bottom nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); true }
                R.id.nav_activity -> { startActivity(Intent(this, StepsActivity::class.java)); true }
                R.id.nav_hydrate -> { startActivity(Intent(this, HydrateActivity::class.java)); true }
                R.id.nav_moods -> { startActivity(Intent(this, MoodsActivity::class.java)); true }
                else -> false
            }
        }

        //  Widgets
        chartHabits = findViewById(R.id.chartHabits)
        tvHabitPercent = findViewById(R.id.tvHabitPercent)
        chartHydration = findViewById(R.id.chartHydration)
        tvHydrationPercent = findViewById(R.id.tvHydrationPercent)
        tvTodayMood = findViewById(R.id.tvTodayMood)
        tvMoodName = findViewById(R.id.tvMoodName)
        tvStepsHome = findViewById(R.id.tvStepsHome)
        tvKcalHome = findViewById(R.id.tvKcalHome)

        //  Habits % calculation
        val gson = Gson()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val habitsJson = prefs.getString("habits_list", "[]")
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val allHabits: MutableList<Habit> = gson.fromJson(habitsJson, type)
        val todayHabits = allHabits.filter { it.date == todayDate }
        val doneCount = todayHabits.count { it.status == "done" }
        val habitsPercent = if (todayHabits.isNotEmpty()) (doneCount * 100) / todayHabits.size else 0

        //  Hydration % calculation
        val todayTotal = prefsHydrate.getInt(todayDate, 0)
        val hydrationPercent = ((todayTotal * 100) / hydrationTarget).coerceAtMost(100)

        //  Render charts
        setupDonutChart(chartHabits, habitsPercent)
        tvHabitPercent.text = "$habitsPercent%"

        setupDonutChart(chartHydration, hydrationPercent)
        tvHydrationPercent.text = "$hydrationPercent%"

        // 🔹 Mood
        val todayMood = prefs.getString("today_mood", "🙂") ?: "🙂"
        tvTodayMood.text = todayMood
        tvMoodName.text = getMoodName(todayMood)
        tvTodayMood.scaleX = 0f
        tvTodayMood.scaleY = 0f
        tvTodayMood.animate().scaleX(1f).scaleY(1f)
            .setDuration(800).setInterpolator(OvershootInterpolator()).start()

        // 🔹 Initial steps load
        val steps = prefsSteps.getInt(getTodayKey(), 0)
        updateStepViews(steps)
    }

    /**  Sidebar Manage Notifications dialog */
    private fun showManageNotificationDialog() {
        val alarmsStopped = prefs.getBoolean("alarmsStopped", false)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Notification Manager")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        val btnStop = Button(this).apply {
            text = "Stop All Alarms"
            isEnabled = !alarmsStopped
            setOnClickListener {
                stopAllAlarms()
                prefs.edit().putBoolean("alarmsStopped", true).apply()
                Toast.makeText(this@HomeActivity, "All alarms stopped", Toast.LENGTH_SHORT).show()
            }
        }

        val btnResume = Button(this).apply {
            text = "Resume All Alarms"
            isEnabled = alarmsStopped
            setOnClickListener {
                resumeAllAlarms()
                prefs.edit().putBoolean("alarmsStopped", false).apply()
                Toast.makeText(this@HomeActivity, "All alarms resumed", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnStop)
        layout.addView(btnResume)

        builder.setView(layout)
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun stopAllAlarms() {
        // TODO: Cancel all alarms with AlarmManager.cancel()
    }

    private fun resumeAllAlarms() {
        // TODO: Reschedule all alarms
    }

    private fun updateStepViews(steps: Int) {
        tvStepsHome.text = "$steps steps"
        tvKcalHome.text = "${calculateCalories(steps)} kcal"
    }

    private fun calculateCalories(steps: Int): Int {
        val caloriesPerStep = 0.04
        return (steps * caloriesPerStep).toInt()
    }

    private fun getTodayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private fun getMoodName(emoji: String): String {
        return when (emoji) {
            "😎" -> "Cool"
            "🙂" -> "Happy"
            "😐" -> "Neutral"
            "😢" -> "Sad"
            "😡" -> "Angry"
            else -> "Unknown"
        }
    }

    /** Donut chart setup (gradient + remainder) */
    private fun setupDonutChart(chart: PieChart, percent: Int) {
        val entries = listOf(
            PieEntry(percent.toFloat(), "Completed"),
            PieEntry((100 - percent).toFloat(), "Remaining")
        )
        val dataSet = PieDataSet(entries, "").apply {
            setDrawValues(false)
            sliceSpace = 0f
            selectionShift = 0f
            colors = listOf(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        chart.apply {
            data = PieData(dataSet)
            isRotationEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isHighlightPerTapEnabled = false
            holeRadius = 70f
            transparentCircleRadius = 75f
            setHoleColor(getColor(R.color.white))

            renderer = SingleGradientPieRenderer(
                this,
                intArrayOf(getColor(R.color.splashstartColor), getColor(R.color.splashendColor)),
                Color.parseColor("#E0E0E0")
            )

            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    /** Custom renderer for gradient ring */
    private class SingleGradientPieRenderer(
        private val chart: PieChart,
        private val gradientColors: IntArray,
        private val remainderColor: Int
    ) : PieChartRenderer(chart, chart.animator, chart.viewPortHandler) {
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
        }
        override fun drawDataSet(c: Canvas, dataSet: IPieDataSet) {
            val phaseY = mAnimator.phaseY
            val center = chart.centerCircleBox
            val radius = chart.radius
            val stroke = radius * 0.18f
            ringPaint.strokeWidth = stroke

            val arcRect = RectF(
                center.x - radius + stroke / 2f,
                center.y - radius + stroke / 2f,
                center.x + radius - stroke / 2f,
                center.y + radius - stroke / 2f
            )

            val drawAngles = chart.drawAngles
            var startAngle = chart.rotationAngle

            if (dataSet.entryCount > 0 && drawAngles.isNotEmpty()) {
                val sweep = drawAngles[0] * phaseY
                val shader = SweepGradient(center.x, center.y, gradientColors, null)
                val m = Matrix()
                m.postRotate(startAngle, center.x, center.y)
                shader.setLocalMatrix(m)
                ringPaint.shader = shader
                c.drawArc(arcRect, startAngle, sweep, false, ringPaint)
                startAngle += sweep
            }
            if (dataSet.entryCount > 1 && drawAngles.size > 1) {
                val sweep = drawAngles[1] * phaseY
                ringPaint.shader = null
                ringPaint.color = remainderColor
                c.drawArc(arcRect, startAngle, sweep, false, ringPaint)
            }
        }
    }

    private fun updateCalendar() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        monthYearTv.text = dateFormat.format(calendar.time)
        calendarDaysContainer.removeAllViews()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        for (day in 1..daysInMonth) {
            val dayLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val dayView = TextView(this).apply {
                text = day.toString()
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                if (day == today &&
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
                ) {
                    setBackgroundResource(R.drawable.bg_today)
                    setTextColor(resources.getColor(android.R.color.white, theme))
                } else {
                    setTextColor(resources.getColor(R.color.black, theme))
                }
            }
            dayLayout.addView(dayView)
            calendarDaysContainer.addView(dayLayout)
        }
    }

    private fun updateNotificationBadge() {
        val count = prefs.getInt("notification_count", 0)
        if (count > 0) {
            notificationBadge.text = count.toString()
            notificationBadge.visibility = TextView.VISIBLE
        } else {
            notificationBadge.visibility = TextView.GONE
        }
    }

    /**  UPDATED to use NotificationStore */
    private fun showNotificationsDialog() {
        val logs = NotificationStore.getNotifications(this)
        if (logs.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Notifications")
                .setMessage("No notifications yet.")
                .setPositiveButton("OK", null).show()
        } else {
            val items = logs.toTypedArray()
            AlertDialog.Builder(this).setTitle("Notifications")
                .setItems(items, null)
                .setPositiveButton("Clear All") { _, _ ->
                    NotificationStore.clearNotifications(this)
                    prefs.edit().putInt("notification_count", 0).apply()
                    updateNotificationBadge()
                }
                .setNegativeButton("Close", null).show()

            // reset badge count after viewing
            prefs.edit().putInt("notification_count", 0).apply()
            updateNotificationBadge()
        }
    }

    override fun onResume() {
        super.onResume()
        // 🔹 Register receivers
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(stepsReceiver, IntentFilter("STEP_UPDATE"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(badgeUpdateReceiver, IntentFilter("UPDATE_NOTIFICATION_BADGE"),
                Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(badgeUpdateReceiver, IntentFilter("UPDATE_NOTIFICATION_BADGE"))
        }

        refreshHabitsProgress()
        refreshHydrationProgress()
        updateNotificationBadge()

        // refresh steps instantly
        val steps = prefsSteps.getInt(getTodayKey(), 0)
        updateStepViews(steps)
    }

    override fun onPause() {
        super.onPause()
        // 🔹 Unregister receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepsReceiver)
        unregisterReceiver(badgeUpdateReceiver)
    }

    private fun refreshHabitsProgress() {
        val gson = Gson()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val habitsJson = prefs.getString("habits_list", "[]")
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val allHabits: MutableList<Habit> = gson.fromJson(habitsJson, type)
        val todayHabits = allHabits.filter { it.date == todayDate }
        val doneCount = todayHabits.count { it.status == "done" }
        val habitsPercent = if (todayHabits.isNotEmpty()) (doneCount * 100) / todayHabits.size else 0
        setupDonutChart(chartHabits, habitsPercent)
        tvHabitPercent.text = "$habitsPercent%"
    }

    private fun refreshHydrationProgress() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val todayTotal = prefsHydrate.getInt(todayDate, 0)
        val hydrationPercent = ((todayTotal * 100) / hydrationTarget).coerceAtMost(100)
        setupDonutChart(chartHydration, hydrationPercent)
        tvHydrationPercent.text = "$hydrationPercent%"
    }
}
