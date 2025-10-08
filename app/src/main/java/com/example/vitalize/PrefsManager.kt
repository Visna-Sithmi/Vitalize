package com.example.vitalize

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREF_NAME = "vitalize_prefs"
    private const val KEY_COMPLETION_PREFIX = "completion_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Save today’s habit completion percentage (0–100)
    fun saveDailyCompletion(context: Context, date: String, percent: Int) {
        getPrefs(context).edit().putInt(KEY_COMPLETION_PREFIX + date, percent).apply()
    }

    // Get today’s completion % (default = 0)
    fun getDailyCompletion(context: Context, date: String): Int {
        return getPrefs(context).getInt(KEY_COMPLETION_PREFIX + date, 0)
    }
}
