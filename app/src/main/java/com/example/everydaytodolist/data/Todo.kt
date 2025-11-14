package com.example.everydaytodolist.data

import java.util.Calendar //DO NOT USE THE ANDROID IMPORT, it breaks the unit tests and I am _not_ mocking out Calendar, that's dumb
import java.time.LocalTime
import java.util.Date

//TODO Interface for different types of Todos
class Todo(
    var title: String = "New Todo", //TODO Get this into a string resource
    var frequencyInDays: Int = 1,
    var alarmTime: LocalTime = LocalTime.of(9,0)
) {
    private var nextOccurrence: Calendar = calculateNextOccurrence(from = Calendar.getInstance())
    private var lastOccurrence: Calendar = {
        val calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Set lastOccurrence to yesterday to avoid it looking like it was completed today until explicitly marked as such
        calendar!!
    }()


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

    fun completedToday(): Boolean {
        val today = Calendar.getInstance()
        return (today.get(Calendar.DAY_OF_YEAR) == lastOccurrence.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == lastOccurrence.get(Calendar.YEAR))
    }

    fun markCompleted() {
        timesSnoozedSinceLastCompletion = 0
        lastOccurrence = Calendar.getInstance()
        lastOccurrence.isLenient = true
        nextOccurrence = calculateNextOccurrence(from = lastOccurrence)
    }

    fun snooze(snoozeLength: Int = 1) {
        timesSnoozedSinceLastCompletion++
        nextOccurrence = calculateNextOccurrence(from = nextOccurrence, snoozeLength = snoozeLength)
    }

    private fun calculateNextOccurrence(
        from: Calendar = lastOccurrence,
        snoozeLength: Int = frequencyInDays
    ): Calendar {
        val calendar = from.clone() as Calendar
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, snoozeLength)
        calendar.set(Calendar.HOUR_OF_DAY, alarmTime.hour)
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