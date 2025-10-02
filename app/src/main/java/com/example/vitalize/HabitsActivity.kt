package com.example.vitalize.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vitalize.R
import com.example.vitalize.HomeActivity
import com.example.vitalize.databinding.ActivityHabitsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HabitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitsBinding
    private val habits = mutableListOf<Habit>()
    private lateinit var adapter: HabitsAdapter

    // 🔹 Persistence
    private val prefs by lazy { getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE) }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Make page fullscreen (hide purple status bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = android.graphics.Color.TRANSPARENT  // transparent status bar

        binding = ActivityHabitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Setup RecyclerView and Adapter
        adapter = HabitsAdapter(
            habits,
            onEdit = { editHabit(it) },
            onDelete = { deleteHabit(it) },
            onSave = { saveHabits() } // ✅ save immediately when checkbox toggled
        )

        binding.rvHabits.layoutManager = LinearLayoutManager(this)
        binding.rvHabits.adapter = adapter

        // 🔹 Load saved habits
        loadHabits()

        // 🔹 Add new habit button
        binding.btnAddHabit.setOnClickListener { showHabitDialog(null) }

        // 🔹 Bottom Navigation Setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_habits // ✅ Always highlight Habits

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_habits -> true // already here
                R.id.nav_activity -> true
                R.id.nav_hydrate -> true
                R.id.nav_moods -> true
                else -> false
            }
        }
    }

    // 🔹 Dialog for adding/editing habits
    private fun showHabitDialog(habit: Habit?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val etDuration = dialogView.findViewById<EditText>(R.id.etDuration)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        // Pre-fill if editing
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
                    habits.add(Habit(name, duration, hour, minute))
                } else {
                    habit.name = name
                    habit.duration = duration
                    habit.startHour = hour
                    habit.startMinute = minute
                }

                adapter.notifyDataSetChanged()
                saveHabits() // ✅ Save after adding/editing
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editHabit(habit: Habit) = showHabitDialog(habit)

    private fun deleteHabit(habit: Habit) {
        habits.remove(habit)
        adapter.notifyDataSetChanged()
        saveHabits() // ✅ Save after delete
    }

    // 🔹 Save habits to SharedPreferences
    private fun saveHabits() {
        val json = gson.toJson(habits)
        prefs.edit().putString("habits_list", json).apply()
    }

    // 🔹 Load habits from SharedPreferences
    private fun loadHabits() {
        val json = prefs.getString("habits_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            val savedHabits: MutableList<Habit> = gson.fromJson(json, type)
            habits.clear()
            habits.addAll(savedHabits)
            adapter.notifyDataSetChanged()
        }
    }
}
