package com.example.everydaytodolist.data

import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import kotlin.text.removeSurrounding
import kotlin.text.toInt
import kotlin.text.toLong

class DailyTodo(
    override val title: String = ITodo.defaultName,
    override val frequency: Int = ITodo.defaultFrequency,
    override val alarmTime: LocalTime = ITodo.defaultAlarmTime,
    override val uniqueId: Int = ITodo.getNextUniqueId(),
    override val maxOccurrences: Int? = null,
    override val endDate: Calendar? = null
): ITodo {

    constructor(
        title: String,
        frequency: Int,
        alarmTime: LocalTime,
        maxOccurrences: Int?,
        endDate: Calendar?,
        nextOccurrence: Calendar
    ) : this(title = title, frequency = frequency, alarmTime = alarmTime, maxOccurrences = maxOccurrences, endDate = endDate) {
        this.nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        ITodo.bumpLastUniqueId(uniqueId)
    }

    // copy constructor: helps mimic underlying data from another TodoObject
    constructor(
        title: String,
        frequency: Int,
        alarmTime: LocalTime,
        uniqueId: Int,
        maxOccurrences: Int?,
        endDate: Calendar?,
        lastOccurrence: Calendar,
        nextOccurrence: Calendar,
        timesSnoozedSinceLastCompletion: Int,
        numOccurrences: Int
    ) : this(title, frequency, alarmTime, uniqueId, maxOccurrences, endDate) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletion
        this.numOccurrences = numOccurrences
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
        maxOccurrences: Int?,
        endDate: Calendar?,
        lastOccurrence: Calendar,
        timesSnoozedSinceLastCompletions: Int,
        numOccurrences: Int
    ) : this(title, frequency, alarmTime, uniqueId, maxOccurrences, endDate) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = calculateNextOccurrence()
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletions
        this.numOccurrences = numOccurrences
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

    private var numOccurrences: Int = 0
    override fun getNumOccurrences(): Int {
        return numOccurrences
    }

    private var timesSnoozedSinceLastCompletion: Int = 0
    override fun getTimesSnoozedSinceLastCompletion(): Int {
        return timesSnoozedSinceLastCompletion
    }

    override fun markCompleted(): Boolean {
        timesSnoozedSinceLastCompletion = 0
        lastOccurrence = Calendar.getInstance()
        lastOccurrence.isLenient = true
        nextOccurrence = calculateNextOccurrence(from = lastOccurrence)
        numOccurrences++
        if((maxOccurrences != null && numOccurrences >= maxOccurrences) ||
            (endDate != null && nextOccurrence > endDate)) {
            return false
        }
        return true
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
                "maxOccurrences=$maxOccurrences, " +
                "endDate=${endDate?.timeInMillis}, " +
                "nextOccurrence=${nextOccurrence.timeInMillis}, " +
                "lastOccurrence=${lastOccurrence.timeInMillis}, " +
                "timesSnoozedSinceLastCompletion=$timesSnoozedSinceLastCompletion), " +
                "numOccurrences=$numOccurrences" +
                ")"
    }

    override fun fromPropertiesMap(propertyMap: Map<String,String>): DailyTodo? {
        var parseIssue = false

        val title = (propertyMap["title"]?.removeSurrounding("'") ?: { parseIssue = true; "New Todo" }) as String
        val frequency = (propertyMap["frequencyInDays"]?.toIntOrNull() ?: { parseIssue = true; 1 }) as Int
        val alarmTime = (propertyMap["alarmTime"]?.let { LocalTime.parse(it) } ?: { parseIssue = true; LocalTime.of(9, 0) }) as LocalTime
        val uniqueId = (propertyMap["uniqueId"]?.toIntOrNull() ?: { parseIssue = true; ITodo.getNextUniqueId() }) as Int
        val maxOccurrences = (propertyMap["maxOccurrences"]?.toIntOrNull() ?: {/*parseIssue = true*/ null }) as Int?
        val endDate = {
            val time = (propertyMap["endDate"]?.toLongOrNull() ?: { /*parseIssue = true*/; null }) as Long?
            val calendar = Calendar.getInstance()
            when(time) {
                null -> null
                else -> calendar.apply { timeInMillis = time }
            }
        }()
        val timesSnoozed = (propertyMap["timesSnoozedSinceLastCompletion"]?.toIntOrNull() ?: { parseIssue = true; 0 }) as Int
        val lastOccurrence = {
            val time = (propertyMap["lastOccurrence"]?.toLongOrNull() ?: { parseIssue = true; 1 }) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()
        val nextOccurrence = {
            val time = (propertyMap["nextOccurrence"]?.toLongOrNull() ?: { parseIssue = true; 1 }) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()
        val numOccurrences = (propertyMap["numOccurrences"]?.toIntOrNull() ?: {/*parseIssue = true*/ 0 }) as Int

        return when(parseIssue) {
            true -> null
            false -> {
                DailyTodo(
                    title = title,
                    frequency = frequency,
                    alarmTime = alarmTime,
                    uniqueId = uniqueId,
                    maxOccurrences = maxOccurrences,
                    endDate = endDate,
                    lastOccurrence = lastOccurrence,
                    nextOccurrence = nextOccurrence,
                    timesSnoozedSinceLastCompletion = timesSnoozed,
                    numOccurrences = numOccurrences
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
            maxOccurrences = this.maxOccurrences,
            endDate = this.endDate,
            lastOccurrence = this.lastOccurrence,
            nextOccurrence = this.nextOccurrence,
            timesSnoozedSinceLastCompletion = this.timesSnoozedSinceLastCompletion,
            numOccurrences = this.numOccurrences
        )
    }
}