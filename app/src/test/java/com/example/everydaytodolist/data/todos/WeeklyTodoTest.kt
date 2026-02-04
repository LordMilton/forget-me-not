package com.example.everydaytodolist.data.todos

import com.example.everydaytodolist.data.DayOfWeekUtil.Factory.calendarToTimeDayOfWeek
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Calendar

class WeeklyTodoTest {

    @Test
    fun `test serialization and deserialization`() {
        // Given a WeeklyTodo with multiple days
        val original = WeeklyTodo(
            title = "Gym",
            frequency = 1,
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            alarmTime = LocalTime.of(18, 30)
        )

        // When converted to string and back through a properties map
        val serializedString = original.toString()

        // Mocking the behavior of your property map parser 
        // (Assuming you have a helper to turn the toString back into a Map)
        val propertyMap = parseToStringToMap(serializedString)
        val deserialized = WeeklyTodo().fromPropertiesMap(propertyMap)

        // Then values should match
        assertNotNull(deserialized)
        assertEquals(original.title, deserialized?.title)
        assertEquals(original.daysOfWeek, deserialized?.daysOfWeek)
        assertEquals(original.alarmTime, deserialized?.alarmTime)
    }

    @Test
    fun `test next occurrence calculation for same week`() {
        // Given today is Monday
        val weekOfYear = 3
        val monday = Calendar.getInstance().apply {
            set(Calendar.WEEK_OF_YEAR, weekOfYear)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        // A task for Wednesday and Friday
        val todo = WeeklyTodo(
            title = "Mid-week task",
            frequency = 1,
            daysOfWeek = listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            alarmTime = LocalTime.of(9, 0),
            now = monday
        )

        // The first occurrence should be Wednesday
        val next = Calendar.getInstance().apply {
            time = todo.getNextOccurrence()
        }
        assertEquals(weekOfYear, next.get(Calendar.WEEK_OF_YEAR))
        assertEquals(DayOfWeek.WEDNESDAY, calendarToTimeDayOfWeek(next.get(Calendar.DAY_OF_WEEK)))

        // and when marked complete
        todo.markCompleted()
        // The next occurrence should be Friday
        val next2 = Calendar.getInstance().apply {
            time = todo.getNextOccurrence()
        }
        assertEquals(weekOfYear, next.get(Calendar.WEEK_OF_YEAR))
        assertEquals(DayOfWeek.FRIDAY, calendarToTimeDayOfWeek(next2.get(Calendar.DAY_OF_WEEK)))
    }

    @Test
    fun `test completion day of triggers frequency gap`() {

        // Given today is Wednesday on the 3rd week of the year
        val currentWeek = 3
        val wednesday = Calendar.getInstance().apply {
            set(Calendar.WEEK_OF_YEAR, currentWeek)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        }

        // Given a task every 2 weeks on Wednesdays
        val todo = WeeklyTodo(
            title = "Bi-weekly task",
            frequency = 2,
            daysOfWeek = listOf(DayOfWeek.WEDNESDAY),
            now = wednesday
        )

        // When completed
        todo.markCompleted()

        // Then next occurrence should be 2 weeks later
        val next = Calendar.getInstance()
        next.time = todo.getNextOccurrence()
        assertEquals(currentWeek + 2, next.get(Calendar.WEEK_OF_YEAR))
    }

    @Test
    fun `test completion day before and one week after triggers frequency gap`() {

        // Given today is Tuesday on the 3rd week of the year
        val currentWeek = 3
        val tuesday = Calendar.getInstance().apply {
            set(Calendar.WEEK_OF_YEAR, currentWeek)
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        }

        // Given a task every 2 weeks on Wednesdays that was due last Wednesday
        val lastWednesday = Calendar.getInstance().apply {
            set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        }
        val lastTuesday = Calendar.getInstance().apply {
            set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        }
        val todo = WeeklyTodo(
            title = "Bi-weekly task",
            frequency = 2,
            daysOfWeek = listOf(DayOfWeek.WEDNESDAY),
            lastOccurrence = lastTuesday,
            nextOccurrence = lastWednesday,
            alarmTime = LocalTime.of(9,0),
            uniqueId = 1,
            maxOccurrences = null,
            endDate = null,
            timesSnoozedSinceLastCompletion = 1,
            numOccurrences = 0
        )
        todo.setNow(tuesday)

        // When completed
        todo.markCompleted()

        // Then next occurrence should be 2 weeks later
        val next = Calendar.getInstance()
        next.time = todo.getNextOccurrence()
        assertEquals(currentWeek + 1, next.get(Calendar.WEEK_OF_YEAR))
        assertEquals(DayOfWeek.WEDNESDAY, calendarToTimeDayOfWeek(next.get(Calendar.DAY_OF_WEEK)))
    }

    /**
     * Helper to simulate your parsing logic for the test
     */
    private fun parseToStringToMap(toStringOutput: String): Map<String, String> {
        val contents = toStringOutput.substringAfter("(").substringBeforeLast(")")
        return contents.split(", ")
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }
    }
}