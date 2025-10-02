package com.example.vitalize.ui.main

data class Habit(
    var name: String,
    var duration: Int,
    var startHour: Int,
    var startMinute: Int,
    var isCompleted: Boolean = false // ✅ default false
)
