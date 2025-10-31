package com.example.everydaytodolist.data

import android.icu.util.Calendar
import java.time.LocalTime
import java.util.Date

class Todo(
    var title: String,
    var frequencyInDays: Int,
    var alarmTime: LocalTime
) {

    val calendar: Calendar = Calendar.getInstance()
    var lastOccurrence: Date = calendar.time
        private set(value) {
            field = value
        }

    companion object Factory {
        fun copy(other: Todo): Todo {
            var todoCopy = Todo(other.title, other.frequencyInDays, other.alarmTime)
            todoCopy.lastOccurrence = other.lastOccurrence
            return todoCopy
        }
    }
}