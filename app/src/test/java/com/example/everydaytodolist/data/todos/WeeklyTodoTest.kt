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
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayDayOfWeek = calendarToTimeDayOfWeek(yesterday.get(Calendar.DAY_OF_WEEK))
        println(yesterdayDayOfWeek)
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowDayOfWeek = calendarToTimeDayOfWeek(tomorrow.get(Calendar.DAY_OF_WEEK))
        println(tomorrowDayOfWeek)

        // A task for Wednesday and Friday
        val todo = WeeklyTodo(
            title = "Mid-week task",
            frequency = 1,
            daysOfWeek = listOf(yesterdayDayOfWeek, tomorrowDayOfWeek),
            alarmTime = LocalTime.of(9, 0)
        )

        // When we mark todoObject as completed
        todo.markCompleted()

        // Then the next occurrence should be tomorrow
        val next = Calendar.getInstance()
        next.time = todo.getNextOccurrence()
        assertEquals(tomorrowDayOfWeek, calendarToTimeDayOfWeek(next.get(Calendar.DAY_OF_WEEK)))
    }

    @Test
    fun `test completion triggers frequency gap`() {
        // Given a task every 2 weeks on Mondays
        val todo = WeeklyTodo(
            title = "Bi-weekly task",
            frequency = 2,
            daysOfWeek = listOf(DayOfWeek.MONDAY)
        )

        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

        // When completed
        todo.markCompleted()

        // Then next occurrence should be 2 weeks later
        val next = Calendar.getInstance()
        next.time = todo.getNextOccurrence()
        assertEquals(currentWeek + 2, next.get(Calendar.WEEK_OF_YEAR))
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