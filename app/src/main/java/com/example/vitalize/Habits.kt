package com.example.vitalize.ui.main

data class Habit(
    var name: String,
    var duration: Int,
    var startHour: Int,
    var startMinute: Int,
    var status: String = "pending", // pending, done, canceled
    var date: String = "" // format yyyy-MM-dd
)
