package com.example.everydaytodolist.data

import android.icu.util.Calendar
import java.time.LocalTime
import java.util.Date

class Todo(
    var title: String = "New Todo", //TODO Get this into a string resource
    var frequencyInDays: Int = 1,
    var alarmTime: LocalTime = LocalTime.of(9,0)
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