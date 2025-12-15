package com.example.everydaytodolist.data

import java.util.Calendar //DO NOT USE THE ANDROID IMPORT, it breaks the unit tests and I am _not_ mocking out Calendar, that's dumb
import java.time.LocalTime
import java.util.Date

//TODO Interface for different types of Todos
class Todo(
    var title: String = defaultName,
    frequencyInDays: Int = defaultFrequency,
    alarmTime: LocalTime = defaultAlarmTime,
    private val uniqueId: Int = getNextUniqueId()
) {

    // Reading constructor: primarily used when reading in todos from save file
    constructor(
        title: String,
        frequencyInDays: Int,
        alarmTime: LocalTime,
        uniqueId: Int,
        lastOccurrence: Calendar,
        nextOccurrence: Calendar,
        timesSnoozedSinceLastCompletions: Int
    ) : this(title, frequencyInDays, alarmTime, uniqueId) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = nextOccurrence
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletions
    }

    var frequencyInDays = frequencyInDays
        set(value) {
            val oldField = field
            field = value
            if(field != oldField) {
                // If frequencyInDays gets changed, reset the nextOccurrence timing based on the new frequency
                // Leaves snooze coloring alone, but will be calculated as if it hasn't been snoozed (seemed most logical at the time)
                nextOccurrence = calculateNextOccurrence()
            }
        }
    var alarmTime = alarmTime
        set(value) {
            val oldField = field
            field = value
            if(field != oldField) {
                // If alarmTime gets changed, fix the nextOccurrence timing based on the new alarm time
                nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
            }
        }
    private var lastOccurrence: Calendar = {
        val calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, -frequencyInDays) // Set lastOccurrence to yesterday to avoid it looking like it was completed today until explicitly marked as such
        calendar
    }()
    private var nextOccurrence: Calendar = calculateNextOccurrence(from = lastOccurrence)

    var timesSnoozedSinceLastCompletion: Int = 0
        private set(value) {
            field = value
        }

    init {
        if(lastUniqueId < uniqueId) lastUniqueId = uniqueId
    }

    fun getUniqueId(): Int {
        return uniqueId
    }
    fun getLastOccurrenceTime(): Date {
        return lastOccurrence.time
    }
    fun getNextOccurrenceTime(): Date {
        return nextOccurrence.time
    }

    fun dueToday(): Boolean {
        val today = Calendar.getInstance()
        val dueToday = nextOccurrence.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                nextOccurrence.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        return dueToday
    }

    fun dueBeforeToday(): Boolean {
        val midnightToday = Calendar.getInstance()
        midnightToday.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextOccurrence < midnightToday
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

    fun snoozeUntil(calendarDate: Calendar, matchAlarmTime: Boolean = true) {
        timesSnoozedSinceLastCompletion++
        nextOccurrence = calendarDate
        if(matchAlarmTime) {
            nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        }
    }

    private fun calculateNextOccurrence(
        from: Calendar = lastOccurrence,
        snoozeLength: Int = frequencyInDays
    ): Calendar {
        var calendar = from.clone() as Calendar
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, snoozeLength)
        // If the nextOccurrence ends up being in the past, bump it forward to today
        val today = Calendar.getInstance().apply{
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if(today.after(calendar)) {
            calendar.apply{
                set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
                set(Calendar.YEAR, today.get(Calendar.YEAR))
            }
        }
        calendar = setCalendarToAlarmTime(calendar)
        return calendar
    }

    private fun setCalendarToAlarmTime(calendar: Calendar): Calendar {
        val calendar = calendar.clone() as Calendar
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, alarmTime.hour)
            set(Calendar.MINUTE, alarmTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar
    }

    override fun toString(): String {
        return "Todo(title='$title', " +
                "frequencyInDays=$frequencyInDays, " +
                "alarmTime=$alarmTime, " +
                "uniqueId=$uniqueId, " +
                "nextOccurrence=${nextOccurrence.timeInMillis}, " +
                "lastOccurrence=${lastOccurrence.timeInMillis}, " +
                "timesSnoozedSinceLastCompletion=$timesSnoozedSinceLastCompletion)"
    }


    companion object Factory {
        private var lastUniqueId = 1

        val defaultName = "New Todo" //TODO Get this into a string resource
        val defaultFrequency = 1
        val defaultAlarmTime = LocalTime.of(9,0)

        fun getNextUniqueId(): Int{
            return ++lastUniqueId
        }

        fun copy(other: Todo): Todo { //TODO Should probably change this by making Todo Cloneable and implementing this as clone()
            val todoCopy = Todo(other.title, other.frequencyInDays, other.alarmTime, other.getUniqueId())
            todoCopy.lastOccurrence = other.lastOccurrence
            todoCopy.nextOccurrence = other.nextOccurrence
            todoCopy.timesSnoozedSinceLastCompletion = other.timesSnoozedSinceLastCompletion
            return todoCopy
        }
    }
}