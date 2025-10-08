package com.example.vitalize.utils

import android.content.Context

object NotificationStore {
    private const val PREFS = "NotificationPrefs"
    private const val KEY_LIST = "notifications"

    fun addNotification(context: Context, message: String) {
        //open shared pref file
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_LIST, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add("${System.currentTimeMillis()}|$message")
        prefs.edit().putStringSet(KEY_LIST, set).apply()
    }

    fun getNotifications(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_LIST, emptySet())
            ?.map { it.split("|")[1] }
            ?.reversed()
            ?: emptyList()
    }
    fun clearNotifications(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LIST).apply()
    }
}
