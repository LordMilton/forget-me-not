package com.example.everydaytodolist.data

import android.icu.util.Calendar
import java.time.LocalTime
import java.util.Date

//TODO Interface for different types of Todos
class Todo(
    var title: String = "New Todo", //TODO Get this into a string resource
    var frequencyInDays: Int = 1,
    var alarmTime: LocalTime = LocalTime.of(9,0)
) {
    private var nextOccurrence: Calendar = calculateNextOccurrence(from = Calendar.getInstance())
    private var lastOccurrence: Calendar = Calendar.getInstance()
    var timesSnoozedSinceLastCompletion: Int = 0
        private set(value) {
            field = value
        }

    fun getLastOccurrenceTime(): Date {
        return lastOccurrence.time
    }
    fun getNextOccurrenceTime(): Date {
        return nextOccurrence.time
    }

    fun markCompleted() {
        timesSnoozedSinceLastCompletion = 0
        lastOccurrence = Calendar.getInstance()
        nextOccurrence = calculateNextOccurrence()
    }

    fun snooze(snoozeLength: Int = 1) {
        timesSnoozedSinceLastCompletion++
        nextOccurrence = calculateNextOccurrence(snoozeLength = snoozeLength)
    }

    private fun calculateNextOccurrence(
        from: Calendar = nextOccurrence,
        snoozeLength: Int = frequencyInDays
    ): Calendar {
        val calendar = from
        calendar.add(Calendar.DAY_OF_YEAR, snoozeLength)
        calendar.set(Calendar.HOUR, alarmTime.hour)
        calendar.set(Calendar.MINUTE, alarmTime.minute)
        calendar.set(Calendar.SECOND, 0)
        return calendar
    }

    companion object Factory {
        fun copy(other: Todo): Todo {
            var todoCopy = Todo(other.title, other.frequencyInDays, other.alarmTime)
            todoCopy.lastOccurrence = other.lastOccurrence
            todoCopy.nextOccurrence = todoCopy.calculateNextOccurrence(todoCopy.lastOccurrence)
            todoCopy.timesSnoozedSinceLastCompletion = other.timesSnoozedSinceLastCompletion
            return todoCopy
        }
    }
}