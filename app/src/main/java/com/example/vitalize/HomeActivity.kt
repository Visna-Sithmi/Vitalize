package com.example.vitalize

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.vitalize.ui.main.HabitsActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var monthYearTv: TextView
    private lateinit var calendarDaysContainer: LinearLayout
    private var calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("VitalizePrefs", MODE_PRIVATE)
        val userName = prefs.getString("name", "User")

        val userNameTv = findViewById<TextView>(R.id.userNameTv)
        userNameTv.text = "Hello, $userName"

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // 🔹 Make Logout red
        val menu = navigationView.menu
        val logoutItem = menu.findItem(R.id.nav_logout)
        logoutItem.icon?.setTint(getColor(R.color.logoutRed))
        logoutItem.title = Html.fromHtml(
            "<font color='${resources.getColor(R.color.logoutRed, theme)}'>Logout</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        // Open sidebar
        userNameTv.setOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }

        // Sidebar clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    prefs.edit().putBoolean("isLoggedIn", false).apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            true
        }

        // 🔹 Calendar Setup
        monthYearTv = findViewById(R.id.monthYearTv)
        calendarDaysContainer = findViewById(R.id.calendarDaysContainer)

        val prevMonthBtn = findViewById<ImageView>(R.id.prevMonth)
        val nextMonthBtn = findViewById<ImageView>(R.id.nextMonth)

        updateCalendar()

        prevMonthBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextMonthBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // 🔹 Bottom Navigation Setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // ✅ Always highlight Home when on this screen
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // already here
                R.id.nav_habits -> {
                    startActivity(Intent(this, HabitsActivity::class.java))
                    true
                }
                R.id.nav_activity -> {
                    // TODO: add Activity screen
                    true
                }
                R.id.nav_hydrate -> {
                    // TODO: add Hydrate screen
                    true
                }
                R.id.nav_moods -> {
                    // TODO: add Moods screen
                    true
                }
                else -> false
            }
        }
    }

    private fun updateCalendar() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        monthYearTv.text = dateFormat.format(calendar.time)

        // Clear old views
        calendarDaysContainer.removeAllViews()

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // 🔹 Add day numbers horizontally
        for (day in 1..daysInMonth) {
            val dayLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val dayView = TextView(this).apply {
                text = day.toString()
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER

                // ✅ Only highlight today
                if (day == today &&
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
                ) {
                    setBackgroundResource(R.drawable.bg_today) // circle background
                    setTextColor(resources.getColor(android.R.color.white, theme))
                } else {
                    setTextColor(resources.getColor(R.color.black, theme))
                }
            }

            dayLayout.addView(dayView)
            calendarDaysContainer.addView(dayLayout)
        }
    }
}
