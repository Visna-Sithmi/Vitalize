package com.example.vitalize.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vitalize.HomeActivity
import com.example.vitalize.R
import com.example.vitalize.StepsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class MoodEntry(val date: String, val emoji: String)

class MoodsActivity : AppCompatActivity() {

    private lateinit var tvTodayMood: TextView
    private lateinit var calendarRecycler: RecyclerView
    private lateinit var tvMostChosenMood: TextView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnShareMood: MaterialButton

    private val prefs by lazy { getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private val moods = mutableListOf<MoodEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // White status bar
        window.statusBarColor = android.graphics.Color.WHITE

        enableEdgeToEdge()
        setContentView(R.layout.activity_moods)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.moodsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        tvTodayMood = findViewById(R.id.tvTodayMood)
        calendarRecycler = findViewById(R.id.rvCalendar)
        tvMostChosenMood = findViewById(R.id.tvMostChosenMood)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnShareMood = findViewById(R.id.btnShareMood)

        // Load moods from SharedPreferences
        loadMoods()
        setupCalendar()

        //  Restore today’s mood if already saved
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMood = moods.find { it.date == today }
        if (todayMood != null) {
            tvTodayMood.text = todayMood.emoji
            prefs.edit().putString("today_mood", todayMood.emoji).apply()
        }

        // Mood buttons
        findViewById<TextView>(R.id.tvCool).setOnClickListener { saveTodayMood("😎") }
        findViewById<TextView>(R.id.tvHappy).setOnClickListener { saveTodayMood("🙂") }
        findViewById<TextView>(R.id.tvNeutral).setOnClickListener { saveTodayMood("😐") }
        findViewById<TextView>(R.id.tvSad).setOnClickListener { saveTodayMood("😢") }
        findViewById<TextView>(R.id.tvAngry).setOnClickListener { saveTodayMood("😡") }

        // Share Mood Summary
        btnShareMood.setOnClickListener {
            val summary = generateMoodSummary()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "My Mood Summary")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_moods

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_habits -> {
                    startActivity(Intent(this, HabitsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_activity -> {
                    startActivity(Intent(this, StepsActivity::class.java))
                    true
                }
                R.id.nav_hydrate -> {
                    startActivity(Intent(this, HydrateActivity::class.java))
                    true
                }
                R.id.nav_moods -> true
                else -> false
            }
        }
    }

    private fun saveTodayMood(emoji: String) {
        if (emoji.isBlank()) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        moods.removeAll { it.date == today }
        moods.add(MoodEntry(today, emoji))
        saveMoods()

        // Save direct key for HomeActivity
        prefs.edit().putString("today_mood", emoji).apply()

        tvTodayMood.text = emoji
        setupCalendar()

        Toast.makeText(this, "Mood saved for today!", Toast.LENGTH_SHORT).show()
    }

    private fun setupCalendar() {
        calendarRecycler.layoutManager = GridLayoutManager(this, 7)
        calendarRecycler.adapter = MoodsCalendarAdapter(moods)

        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        tvMonthYear.text = monthYear

        val moodCount = moods.filter { it.emoji.isNotBlank() }
            .groupingBy { it.emoji }
            .eachCount()

        if (moodCount.isNotEmpty()) {
            val maxCount = moodCount.values.maxOrNull() ?: 0
            val topMoods = moodCount.filterValues { it == maxCount }.keys
            val moodsString = topMoods.joinToString(" ")
            tvMostChosenMood.text = "Most Chosen Mood: $moodsString ($maxCount times)"
        } else {
            tvMostChosenMood.text = "No moods recorded yet"
        }
    }

    private fun saveMoods() {
        prefs.edit().putString("moods", gson.toJson(moods)).apply()
    }

    private fun loadMoods() {
        val json = prefs.getString("moods", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            moods.clear()
            val loadedMoods: MutableList<MoodEntry> = gson.fromJson(json, type)
            //  FIX: only keep non-blank emojis
            moods.addAll(loadedMoods.filter { it.emoji.isNotBlank() })
        }
    }

    private fun generateMoodSummary(): String {
        val todayDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val total = moods.size

        val moodCount = moods.filter { it.emoji.isNotBlank() }
            .groupingBy { it.emoji }
            .eachCount()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMood = moods.find { it.date == today }?.emoji ?: "Not selected yet"

        var mostChosenText = "No moods recorded yet"
        if (moodCount.isNotEmpty()) {
            val maxCount = moodCount.values.maxOrNull() ?: 0
            val topMoods = moodCount.filterValues { it == maxCount }.keys
            val moodsString = topMoods.joinToString(" ")
            mostChosenText = "$moodsString ($maxCount times)"
        }

        return buildString {
            append("Date: $todayDate\n")
            append("Today's Mood: $todayMood\n")
            append("Most Chosen Mood: $mostChosenText\n\n")
            append("Mood Breakdown (Total $total entries):\n")
            moodCount.forEach { (emoji, count) ->
                append("$emoji : $count times\n")
            }
        }
    }
}
