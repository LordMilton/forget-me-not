package com.milton.forgetmenot.data.todos

import androidx.compose.ui.util.fastJoinToString
import com.milton.forgetmenot.data.DayOfWeekUtil.Factory.calendarToTimeDayOfWeek
import com.milton.forgetmenot.data.DayOfWeekUtil.Factory.timeToCalendarDayOfWeek
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeeklyTodo(
    override val title: String = ITodo.defaultName,
    override val frequency: Int = ITodo.defaultFrequency,
    val daysOfWeek: List<DayOfWeek> = listOf(DayOfWeek.MONDAY),
    override val alarmTime: LocalTime = ITodo.defaultAlarmTime,
    override val uniqueId: Int = ITodo.getNextUniqueId(),
    override val maxOccurrences: Int? = null,
    override val endDate: Calendar? = null,
    private var now: Calendar = Calendar.getInstance()
): ITodo {

    constructor(
        title: String,
        frequency: Int,
        daysOfWeek: List<DayOfWeek>,
        alarmTime: LocalTime,
        maxOccurrences: Int?,
        endDate: Calendar?,
        nextOccurrence: Calendar
    ) : this(title = title, frequency = frequency, daysOfWeek = daysOfWeek, alarmTime = alarmTime, maxOccurrences = maxOccurrences, endDate = endDate) {
        this.nextOccurrence = setCalendarToAlarmTime(nextOccurrence)
        ITodo.bumpLastUniqueId(uniqueId)
    }

    // copy constructor: helps mimic underlying data from another TodoObject
    constructor(
        title: String,
        frequency: Int,
        daysOfWeek: List<DayOfWeek>,
        alarmTime: LocalTime,
        uniqueId: Int,
        maxOccurrences: Int?,
        endDate: Calendar?,
        lastOccurrence: Calendar,
        nextOccurrence: Calendar,
        timesSnoozedSinceLastCompletion: Int,
        numOccurrences: Int
    ) : this(title, frequency, daysOfWeek, alarmTime, uniqueId, maxOccurrences, endDate) {
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
        daysOfWeek: List<DayOfWeek>,
        alarmTime: LocalTime,
        uniqueId: Int,
        maxOccurrences: Int?,
        endDate: Calendar?,
        lastOccurrence: Calendar,
        timesSnoozedSinceLastCompletions: Int,
        numOccurrences: Int
    ) : this(title, frequency, daysOfWeek, alarmTime, uniqueId, maxOccurrences, endDate) {
        this.lastOccurrence = lastOccurrence
        this.nextOccurrence = calculateNextOccurrence()
        this.timesSnoozedSinceLastCompletion = timesSnoozedSinceLastCompletions
        this.numOccurrences = numOccurrences
        ITodo.bumpLastUniqueId(uniqueId)
    }

    private var lastOccurrence: Calendar = {
        val calendar = getNow()
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

    /**
     * Manipulable "today" that lets us make testing consistent
     * Outside of tests, this should not be manipulated
     */
    fun setNow(today: Calendar) {
        this.now = today.clone() as Calendar
        now.isLenient = true
    }
    fun getNow(): Calendar {
        return now.clone() as Calendar
    }

    override fun markCompleted(): Boolean {
        timesSnoozedSinceLastCompletion = 0
        val lastLastOccurrence = getNow().apply {
            timeInMillis = lastOccurrence.timeInMillis
        }
        lastOccurrence = getNow()
        nextOccurrence = calculateNextOccurrence(
            from = lastOccurrence,
            latestOccurrenceWasEarlierWeek = lastLastOccurrence.get(Calendar.WEEK_OF_YEAR) < lastOccurrence.get(Calendar.WEEK_OF_YEAR),
            completedEarly = compareDay(getNow(), nextOccurrence) < 0
        )
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
        val today = getNow()
        val dueToday = nextOccurrence.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                nextOccurrence.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        return dueToday
    }

    override fun dueBeforeToday(): Boolean {
        val midnightToday = getNow()
        midnightToday.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextOccurrence < midnightToday
    }

    override fun completedToday(): Boolean {
        val today = getNow()
        return (today.get(Calendar.DAY_OF_YEAR) == lastOccurrence.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == lastOccurrence.get(Calendar.YEAR))
    }

    private fun calculateNextOccurrence(
        from: Calendar = lastOccurrence,
        snoozeLength: Int = -1,
        latestOccurrenceWasEarlierWeek: Boolean = false,
        completedEarly: Boolean = false
    ): Calendar {
        var calendar = from.clone() as Calendar
        calendar.isLenient = true
        val today = getNow().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        //Handle snoozing
        if(snoozeLength > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, snoozeLength)
            // If the nextOccurrence ends up being in the past, bump it forward to today
            if (today.after(calendar)) {
                calendar.apply {
                    set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                }
            }
            calendar = setCalendarToAlarmTime(calendar)
        }
        else { // Handle non-snoozing (probably completion)
            val lastDayOfWeekAsDate = getNow().apply { set(Calendar.DAY_OF_WEEK, timeToCalendarDayOfWeek(daysOfWeek.last())) }

            var weekCompleted = false
            // If we've run through the week, and frequency needs to come into play
            if(latestOccurrenceWasEarlierWeek && !completedEarly) { //If we ran through last week, but marked completed late
                weekCompleted = true
                calendar.set(Calendar.WEEK_OF_YEAR, from.get(Calendar.WEEK_OF_YEAR) + frequency-1)
            }
            else if(compareDay(from, lastDayOfWeekAsDate) >= 0) { //If we are marking the last occurrence of the week completed
                weekCompleted = true
                calendar.set(Calendar.WEEK_OF_YEAR, from.get(Calendar.WEEK_OF_YEAR) + frequency)
            }
            if(weekCompleted) {
                calendar.set(Calendar.DAY_OF_WEEK, timeToCalendarDayOfWeek(daysOfWeek[0]))
                // If the calculatedNextOccurrence ends up being before the date from which we're calculating (this can happen with a frequency of one week that's being completed late),
                // then bump it forward to the from date but stick with the next reasonable daysOfWeek
                if(compareDay(calendar, from) <= 0) {
                    calendar.apply {
                        set(
                            Calendar.DAY_OF_WEEK,
                            timeToCalendarDayOfWeek(getNextOccurringDayOfWeek(from.get(Calendar.DAY_OF_WEEK)))
                        )
                        set(Calendar.YEAR, from.get(Calendar.YEAR))
                        set(Calendar.WEEK_OF_YEAR, from.get(Calendar.WEEK_OF_YEAR))
                    }
                    if (from.after(calendar)) {
                        calendar.add(Calendar.WEEK_OF_YEAR, frequency)
                    }
                }
                calendar = setCalendarToAlarmTime(calendar)
            }
            // If we're still going through the week
            else {
                calendar.set(Calendar.DAY_OF_WEEK, timeToCalendarDayOfWeek(getNextOccurringDayOfWeek(from.get(Calendar.DAY_OF_WEEK))))
            }
        }
        // Special case, if I complete a task early, I want the next occurrence to actually bump instead of the task
        // just showing back up on the same day in spite of me completing it
        @Suppress("SENSELESS_COMPARISON") // Compiler confused about nextOccurrence != null, but it definitively prevents null pointer errors
        if(nextOccurrence != null && compareDay(calendar, nextOccurrence) <= 0) {
            calendar = calculateNextOccurrence(from = nextOccurrence)
        }
        return calendar
    }

    private fun getNextOccurringDayOfWeek(calendarDayOfWeek: Int): DayOfWeek {
        val curDayOfWeek = calendarToTimeDayOfWeek(calendarDayOfWeek)
        var toReturn = daysOfWeek[daysOfWeek.size-1]
        if(curDayOfWeek > daysOfWeek[daysOfWeek.size-1]) {
            toReturn = daysOfWeek[0]
        }
        else {
            for (day in daysOfWeek) {
                if (day > curDayOfWeek) {
                    toReturn = day
                    break
                }
            }
        }

        return toReturn
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

    private fun compareDay(calendar1: Calendar, calendar2: Calendar): Int {
        val calendar1Clone = (calendar1.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calendar2Clone = (calendar2.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar1Clone.compareTo(calendar2Clone)
    }

    fun daysOfWeekToStringShort(): String {
        return daysOfWeek.sortedBy { timeToCalendarDayOfWeek(it) }.fastJoinToString() {
            it.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        }
    }

    override fun toString(): String {
        return "WeeklyTodo(title='$title', " +
                "frequency=$frequency, " +
                "daysOfWeek=${daysOfWeek.joinToString(separator = ",") { it.name }}, " +
                "alarmTime=$alarmTime, " +
                "uniqueId=$uniqueId, " +
                "maxOccurrences=$maxOccurrences, " +
                "endDate=${endDate?.timeInMillis}, " +
                "nextOccurrence=${nextOccurrence.timeInMillis}, " +
                "lastOccurrence=${lastOccurrence.timeInMillis}, " +
                "timesSnoozedSinceLastCompletion=$timesSnoozedSinceLastCompletion, " +
                "numOccurrences=$numOccurrences" +
                ")"
    }

    @Suppress("USELESS_CAST")
    override fun fromPropertiesMap(propertyMap: Map<String,String>): WeeklyTodo? {
        var parseIssue = false

        val title = (propertyMap["title"]?.removeSurrounding("'") ?: { println("Couldn't parse Todo Title"); parseIssue = true; "New Todo" }()) as String
        val frequency = (propertyMap["frequency"]?.toIntOrNull() ?: { println("Couldn't parse Todo Frequency"); parseIssue = true; 1 }()) as Int
        val daysOfWeek = (propertyMap["daysOfWeek"]?.split(",")?.mapNotNull {
            try { DayOfWeek.valueOf(it) } catch (e: Exception) { println("Couldn't parse a value from daysOfWeek"); null }
        } ?: { println("Couldn't parse Todo Days Of Week"); parseIssue = true; listOf(DayOfWeek.MONDAY) }()) as List<DayOfWeek>
        val alarmTime = (propertyMap["alarmTime"]?.let { LocalTime.parse(it) } ?: { println("Couldn't parse Todo Alarm Time"); parseIssue = true; LocalTime.of(9, 0) }()) as LocalTime
        val uniqueId = (propertyMap["uniqueId"]?.toIntOrNull() ?: { println("Couldn't parse Todo Unique Id"); parseIssue = true; ITodo.getNextUniqueId() }()) as Int
        val maxOccurrences = (propertyMap["maxOccurrences"]?.toIntOrNull() ?: { println("Couldn't parse Todo Max Occurrences (or it was indicated as null)"); /*parseIssue = true;*/ null }()) as Int?
        val endDate = {
            val time = (propertyMap["endDate"]?.toLongOrNull() ?: { println("Couldn't parse Todo End Date (or it was indicated as null)"); /*parseIssue = true;*/ null }()) as Long?
            val calendar = Calendar.getInstance()
            when(time) {
                null -> null
                else -> calendar.apply { timeInMillis = time }
            }
        }()
        val timesSnoozed = (propertyMap["timesSnoozedSinceLastCompletion"]?.toIntOrNull() ?: { println("Couldn't parse Todo Times Snoozed"); parseIssue = true; 0 }()) as Int
        val lastOccurrence = {
            val time = (propertyMap["lastOccurrence"]?.toLongOrNull() ?: { println("Couldn't parse Todo Last Occurrence"); parseIssue = true; 1 }()) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()
        val nextOccurrence = {
            val time = (propertyMap["nextOccurrence"]?.toLongOrNull() ?: { println("Couldn't parse Todo Next Occurrence"); parseIssue = true; 1 }()) as Long
            Calendar.getInstance().apply { timeInMillis = time }
        }()
        val numOccurrences = (propertyMap["numOccurrences"]?.toIntOrNull() ?: { println("Couldn't parse Todo Num Occurrences"); /*parseIssue = true*/ 0 }()) as Int

        return when(parseIssue) {
            true -> null
            false -> {
                WeeklyTodo(
                    title = title,
                    frequency = frequency,
                    daysOfWeek = daysOfWeek,
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
        return WeeklyTodo(
            title = this.title,
            frequency = this.frequency,
            daysOfWeek = this.daysOfWeek,
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