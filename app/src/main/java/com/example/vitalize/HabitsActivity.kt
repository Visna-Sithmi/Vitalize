package com.example.vitalize.ui.main

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vitalize.R
import com.example.vitalize.HomeActivity
import com.example.vitalize.StepsActivity
import com.example.vitalize.databinding.ActivityHabitsBinding
import com.example.vitalize.ui.widget.HabitCompletionWidget
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HabitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitsBinding
    private val habits = mutableListOf<Habit>()
    private lateinit var adapter: HabitsAdapter

    // Persistence
    private val prefs by lazy { getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayDate = dateFormat.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = resources.getColor(R.color.white, theme)

        binding = ActivityHabitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView + Adapter
        adapter = HabitsAdapter(
            habits,
            onEdit = { editHabit(it) },
            onDelete = { deleteHabit(it) },
            onSave = { saveHabits() }
        )
        binding.rvHabits.layoutManager = LinearLayoutManager(this)
        binding.rvHabits.adapter = adapter

        loadHabits()

        // Swipe Gestures
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val habit = habits[pos]
                if (direction == ItemTouchHelper.LEFT) habit.status = "done"
                else if (direction == ItemTouchHelper.RIGHT) habit.status = "canceled"
                adapter.notifyItemChanged(pos)
                saveHabits()
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
                val itemView = vh.itemView
                val paint = Paint()
                if (dX > 0) { // Right = Canceled
                    paint.color = Color.DKGRAY
                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), paint
                    )
                    paint.color = Color.WHITE
                    paint.textSize = 40f
                    c.drawText("Canceled", itemView.left + 50f, itemView.top + 60f, paint)
                } else if (dX < 0) { // Left = Done
                    paint.color = Color.parseColor("#FFB6C1")
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )
                    paint.color = Color.WHITE
                    paint.textSize = 40f
                    c.drawText("Done", itemView.right - 150f, itemView.top + 60f, paint)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvHabits)

        // Add button
        binding.btnAddHabit.setOnClickListener { showHabitDialog(null) }

        // Calendar tap → show progress
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val date = "%04d-%02d-%02d".format(year, month + 1, day)
            showDayProgressDialog(date)
        }

        // Bottom Nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_habits
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_habits -> true
                R.id.nav_activity -> {
                    startActivity(Intent(this, StepsActivity::class.java))
                    true
                }
                R.id.nav_hydrate -> {
                    startActivity(Intent(this, HydrateActivity::class.java))
                    true
                }
                R.id.nav_moods -> {
                    startActivity(Intent(this, MoodsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    // Add/Edit
    private fun showHabitDialog(habit: Habit?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val etDuration = dialogView.findViewById<EditText>(R.id.etDuration)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        if (habit != null) {
            etName.setText(habit.name)
            etDuration.setText(habit.duration.toString())
            timePicker.hour = habit.startHour
            timePicker.minute = habit.startMinute
        }

        AlertDialog.Builder(this)
            .setTitle(if (habit == null) "Add Habit" else "Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString()
                val duration = etDuration.text.toString().toIntOrNull() ?: 0
                val hour = timePicker.hour
                val minute = timePicker.minute
                if (habit == null) {
                    habits.add(Habit(name, duration, hour, minute, "pending", todayDate))
                } else {
                    habit.name = name
                    habit.duration = duration
                    habit.startHour = hour
                    habit.startMinute = minute
                }
                adapter.notifyDataSetChanged()
                saveHabits()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editHabit(habit: Habit) = showHabitDialog(habit)

    private fun deleteHabit(habit: Habit) {
        habits.remove(habit)
        adapter.notifyDataSetChanged()
        saveHabits()
    }

    // Save + Schedule
    private fun saveHabits() {
        val json = gson.toJson(habits)
        prefs.edit().putString("habits_list", json).apply()
        scheduleHabitAlarms()
        saveDailyProgress()
    }

    // Calculate and save daily progress + instantly update widget
    private fun saveDailyProgress() {
        val todayHabits = habits.filter { it.date == todayDate }
        if (todayHabits.isEmpty()) return

        val doneCount = todayHabits.count { it.status == "done" }
        val percent = (doneCount * 100) / todayHabits.size

        prefs.edit().putInt("daily_progress_$todayDate", percent).apply()

        // 🔹 Trigger immediate widget refresh
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, HabitCompletionWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Call the widget’s update function
        for (id in appWidgetIds) {
            HabitCompletionWidget.updateAppWidget(this, appWidgetManager, id)
        }

        // 🔹 Send broadcast to notify system
        val intent = Intent(this, HabitCompletionWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        sendBroadcast(intent)
    }

    private fun scheduleHabitAlarms() {
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (habit in habits) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, habit.startHour)
                set(Calendar.MINUTE, habit.startMinute)
                set(Calendar.SECOND, 0)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) continue

            val intent = Intent(this, HabitReminderReceiver::class.java).apply {
                putExtra("habitName", habit.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                habit.name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmMgr.canScheduleExactAlarms()) {
                        alarmMgr.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(i)
                        Toast.makeText(
                            this,
                            "Enable exact alarms for reminders",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Exact alarm permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelAllHabitAlarms() {
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val json = prefs.getString("habits_list", "[]")
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val allHabits: MutableList<Habit> = gson.fromJson(json, type)

        for (habit in allHabits) {
            val intent = Intent(this, HabitReminderReceiver::class.java).apply {
                putExtra("habitName", habit.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                habit.name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.cancel(pendingIntent)
        }
        Toast.makeText(this, "All upcoming habit alarms canceled", Toast.LENGTH_SHORT).show()
    }

    // Load habits for today
    private fun loadHabits() {
        val json = prefs.getString("habits_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            val saved: MutableList<Habit> = gson.fromJson(json, type)
            habits.clear()
            habits.addAll(saved.filter { it.date == todayDate })
            adapter.notifyDataSetChanged()
        }
    }

    // Progress Popup
    private fun showDayProgressDialog(date: String) {
        val allHabits = gson.fromJson<MutableList<Habit>>(
            prefs.getString("habits_list", "[]"),
            object : TypeToken<MutableList<Habit>>() {}.type
        )
        val dayHabits = allHabits.filter { it.date == date }
        if (dayHabits.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No habits")
                .setMessage("No habits recorded for $date")
                .setPositiveButton("OK", null).show()
            return
        }

        val doneCount = dayHabits.count { it.status == "done" }
        val percent = (doneCount * 100) / dayHabits.size

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        val tvPercent = dialogView.findViewById<TextView>(R.id.tvPercent)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, percent)
        animator.duration = 1200
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.start()

        val textAnimator = ValueAnimator.ofInt(0, percent)
        textAnimator.duration = 1200
        textAnimator.addUpdateListener {
            val value = it.animatedValue as Int
            tvPercent.text = "$value%"
        }
        textAnimator.start()

        AlertDialog.Builder(this)
            .setTitle("Progress on $date")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }
}
