package com.example.everydaytodolist.data

import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import kotlin.text.removeSurrounding
import kotlin.text.toInt
import kotlin.text.toLong

//TODO Interface for different types of Todos
class DailyTodo(
    override val title: String = ITodo.defaultName,
    override val frequency: Int = ITodo.defaultFrequency,
    override val alarmTime: LocalTime = ITodo.defaultAlarmTime,
    override val uniqueId: Int = ITodo.getNextUniqueId()
): ITodo {

    // copy constructor: helps mimic underlying data from another TodoObject
    constructor(
        title: String,
        frequency: Int,
        alarmTime: LocalTime,
        uniqueId: Int,
        lastOccurrence: Calendar,
        nextOccurrence: Calendar,
        timesSnoozedSinceLastCompletion: Int
    ) : this(title, frequency, alarmTime, uniqueId) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletion
        ITodo.bumpLastUniqueId(uniqueId)
    }

    // copy constructor: helps mimic underlying data from another TodoObject but calculates a new nextOccurrence
    //   instead of copying, good for when certain data impacting nextOccurrence would change between the original TodoObject
    //   and this
    constructor(
        title: String,
        frequency: Int,
        alarmTime: LocalTime,
        uniqueId: Int,
        lastOccurrence: Calendar,
        timesSnoozedSinceLastCompletions: Int
    ) : this(title, frequency, alarmTime, uniqueId) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = calculateNextOccurrence()
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletions
        ITodo.bumpLastUniqueId(uniqueId)
    }

    private var lastOccurrence: Calendar = {
        val calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, -frequency) // Set lastOccurrence to yesterday to avoid it looking like it was completed today until explicitly marked as such
        calendar
    }()
    override fun getLastOccurrence(): Date {
        return lastOccurrence.time
    }
    private var nextOccurrence: Calendar = calculateNextOccurrence(from = lastOccurrence)
    override fun getNextOccurrence(): Date {
        return nextOccurrence.time
    }

    private var timesSnoozedSinceLastCompletion: Int = 0
    override fun getTimesSnoozedSinceLastCompletion(): Int {
        return timesSnoozedSinceLastCompletion
    }

    override fun markCompleted() {
        timesSnoozedSinceLastCompletion = 0
        lastOccurrence = Calendar.getInstance()
        lastOccurrence.isLenient = true
        nextOccurrence = calculateNextOccurrence(from = lastOccurrence)
    }

    override fun snooze(snoozeLength: Int) {
        timesSnoozedSinceLastCompletion++
        nextOccurrence = calculateNextOccurrence(from = nextOccurrence, snoozeLength = snoozeLength)
    }

    override fun snoozeUntil(calendarDate: Calendar, matchAlarmTime: Boolean) {
        timesSnoozedSinceLastCompletion++
        nextOccurrence = calendarDate
        if(matchAlarmTime) {
            nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        }
    }

    override fun dueToday(): Boolean {
        val today = Calendar.getInstance()
        val dueToday = nextOccurrence.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                nextOccurrence.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        return dueToday
    }

    override fun dueBeforeToday(): Boolean {
        val midnightToday = Calendar.getInstance()
        midnightToday.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextOccurrence < midnightToday
    }

    override fun completedToday(): Boolean {
        val today = Calendar.getInstance()
        return (today.get(Calendar.DAY_OF_YEAR) == lastOccurrence.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == lastOccurrence.get(Calendar.YEAR))
    }

    private fun calculateNextOccurrence(
        from: Calendar = lastOccurrence,
        snoozeLength: Int = frequency
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
        return "DailyTodo(title='$title', " +
                "frequencyInDays=$frequency, " +
                "alarmTime=$alarmTime, " +
                "uniqueId=$uniqueId, " +
                "nextOccurrence=${nextOccurrence.timeInMillis}, " +
                "lastOccurrence=${lastOccurrence.timeInMillis}, " +
                "timesSnoozedSinceLastCompletion=$timesSnoozedSinceLastCompletion)"
    }

    override fun fromPropertiesMap(propertyMap: Map<String,String>): DailyTodo? {
        var parseIssue = false

        val title = (propertyMap["title"]?.removeSurrounding("'") ?: { parseIssue = true; "New Todo" }) as String
        val frequency = (propertyMap["frequencyInDays"]?.toInt() ?: { parseIssue = true; 1 }) as Int
        val alarmTime = (propertyMap["alarmTime"]?.let { LocalTime.parse(it) } ?: { parseIssue = true; LocalTime.of(9, 0) }) as LocalTime
        val uniqueId = (propertyMap["uniqueId"]?.toInt() ?: { parseIssue = true; ITodo.getNextUniqueId() }) as Int
        val timesSnoozed = (propertyMap["timesSnoozedSinceLastCompletion"]?.toInt() ?: { parseIssue = true; 0 }) as Int
        // Get the occurrence times and fix those in the newly made DailyTodo
        val lastOccurrence = {
            val time = (propertyMap["lastOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()
        val nextOccurrence = {
            val time = (propertyMap["nextOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()

        return when(parseIssue) {
            true -> null
            false -> {
                DailyTodo(
                    title = title,
                    frequency = frequency,
                    alarmTime = alarmTime,
                    uniqueId = uniqueId,
                    lastOccurrence = lastOccurrence,
                    nextOccurrence = nextOccurrence,
                    timesSnoozedSinceLastCompletion = timesSnoozed
                )
            }
        }
    }

    override fun clone(): Any {
        return DailyTodo(
            title = this.title,
            frequency = this.frequency,
            alarmTime = this.alarmTime,
            uniqueId = this.uniqueId,
            lastOccurrence = this.lastOccurrence,
            nextOccurrence = this.nextOccurrence,
            timesSnoozedSinceLastCompletion = this.timesSnoozedSinceLastCompletion
        )
    }
}