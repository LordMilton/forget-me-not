package com.example.everydaytodolist.data

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class DailyTodoTest {

    @Test
    fun `constructor initializes with default values`() {
        // When a DailyTodo is created with no arguments
        val todo = DailyTodo()

        // Then it should have the default properties
        assertEquals("New Todo", todo.title)
        assertEquals(1, todo.frequency)
        assertEquals(LocalTime.of(9, 0), todo.alarmTime)
        assertEquals(0, todo.getTimesSnoozedSinceLastCompletion())
    }

    @Test
    fun `constructor initializes with provided values`() {
        // When a DailyTodo is created with specific arguments
        val alarm = LocalTime.of(15, 30)
        val todo = DailyTodo(title = "Test Task", frequency = 5, alarmTime = alarm)

        // Then it should have the provided properties
        assertEquals("Test Task", todo.title)
        assertEquals(5, todo.frequency)
        assertEquals(alarm, todo.alarmTime)
    }

    @Test
    fun `completedToday returns false for a new todo`() {
        // Given a newly created DailyTodo
        val todo = DailyTodo()

        // When we check if it was completed today
        // Then the result should be false, as its last occurrence is initialized to yesterday
        assertFalse("A new todo should not be marked as completed today.", todo.completedToday())
    }

    @Test
    fun `markCompleted sets lastOccurrence to today and resets snoozes`() {
        // Given a DailyTodo that has been snoozed
        val todo = DailyTodo()
        todo.snooze(2) // Snooze for 2 days
        assertEquals(1, todo.getTimesSnoozedSinceLastCompletion())

        // When markCompleted is called
        todo.markCompleted()

        // Then it should be marked as completed today
        assertTrue("Todo should be marked as completed today.", todo.completedToday())
        // And the snooze count should be reset
        assertEquals("Snooze count should be reset to 0.", 0, todo.getTimesSnoozedSinceLastCompletion())
    }

    @Test
    fun `markCompleted updates nextOccurrence correctly`() {
        // Given a DailyTodo with a frequency of 7 days and an alarm at 10:00
        val alarmTime = LocalTime.of(10, 0)
        val todo = DailyTodo(frequency = 7, alarmTime = alarmTime)

        // When it's marked as completed
        todo.markCompleted()
        val today = Calendar.getInstance()

        // Then the next occurrence should be 7 days from today at the specified alarm time
        val nextOccurrence = todo.getNextOccurrence()
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, alarmTime.hour)
            set(Calendar.MINUTE, alarmTime.minute)
            set(Calendar.SECOND, 0)
        }

        // Compare year, day of year, and time for accuracy
        assertEquals(expectedNextOccurrence.get(Calendar.YEAR), getCalendarField(nextOccurrence, Calendar.YEAR))
        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(nextOccurrence, Calendar.DAY_OF_YEAR))
        assertEquals(alarmTime.hour, getCalendarField(nextOccurrence, Calendar.HOUR_OF_DAY))
        assertEquals(alarmTime.minute, getCalendarField(nextOccurrence, Calendar.MINUTE))
    }

    @Test
    fun `changing alarmTime changes nextOccurrence`() {
        // Given a DailyTodo
        val todo = DailyTodo(frequency = 1, alarmTime = LocalTime.of(9, 0))
        val initialNextOccurrence = todo.getNextOccurrence()

        // When the DailyTodo alarmTime is changed
        val newAlarmTime = LocalTime.of(12, 30)
        val alteredTodo = DailyTodo(
            todo.title,
            todo.frequency,
            newAlarmTime,
            todo.uniqueId,
            todo.maxOccurrences,
            todo.endDate,
            Calendar.getInstance().apply { time = todo.getLastOccurrence() },
            Calendar.getInstance().apply { time = todo.getNextOccurrence() },
            todo.getTimesSnoozedSinceLastCompletion(),
            todo.getNumOccurrences()
        )

        // Then the alarmTime should be changed
        assertEquals(newAlarmTime, alteredTodo.alarmTime)

        // And the next occurrence should be the same day as before but with the new alarmTime
        val expectedNextOccurrence = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, getCalendarField(initialNextOccurrence, Calendar.DAY_OF_YEAR))
            set(Calendar.HOUR_OF_DAY, newAlarmTime.hour)
            set(Calendar.MINUTE, newAlarmTime.minute)
        }
        val newNextOccurrence = alteredTodo.getNextOccurrence()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
        assertEquals(expectedNextOccurrence.get(Calendar.HOUR_OF_DAY), getCalendarField(newNextOccurrence, Calendar.HOUR_OF_DAY))
        assertEquals(expectedNextOccurrence.get(Calendar.MINUTE), getCalendarField(newNextOccurrence, Calendar.MINUTE))
    }

    @Test
    fun `snooze increments snooze count and delays nextOccurrence`() {
        // Given a DailyTodo
        val todo = DailyTodo()
        val initialNextOccurrence = todo.getNextOccurrence()

        // When the DailyTodo is snoozed for 3 days
        val snoozeLength = 3
        todo.snooze(snoozeLength)

        // Then the snooze count should increase
        assertEquals(1, todo.getTimesSnoozedSinceLastCompletion())

        // And the next occurrence should be postponed by 3 days
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, snoozeLength)
        }
        val newNextOccurrence = todo.getNextOccurrence()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `snooze delays even further when used multiple times`() {
        // Given a DailyTodo
        var todo = DailyTodo()
        val initialNextOccurrence = todo.getNextOccurrence()

        // When the DailyTodo is snoozed for 1 day, twice
        val snoozeLength = 1
        val snoozeCount = 2
        repeat(snoozeCount) {
            todo.snooze(snoozeLength)
            // Copying has to happen to force recomposition in the real app, so make sure it doesn't cause weird things here too
            todo = todo.clone() as DailyTodo
        }

        // Then the snooze count should increase twice
        assertEquals(2, todo.getTimesSnoozedSinceLastCompletion())

        // And the next occurrence should be postponed by the snooze amount twice
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, snoozeLength * snoozeCount)
        }
        val newNextOccurrence = todo.getNextOccurrence()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `snooze with default length uses 1 day`() {
        // Given a DailyTodo
        val todo = DailyTodo()
        val initialNextOccurrence = todo.getNextOccurrence()

        // When the DailyTodo is snoozed with the default length
        todo.snooze() // Default is 1 day

        // Then the snooze count should increase
        assertEquals(1, todo.getTimesSnoozedSinceLastCompletion())

        // And the next occurrence should be postponed by 1 day
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val newNextOccurrence = todo.getNextOccurrence()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `Clone creates a deep enough copy`() {
        // Given an original DailyTodo
        val original = DailyTodo("Original Task", 10, LocalTime.of(12, 0))
        original.markCompleted()
        original.snooze(2)

        // When a copy is made
        val copy = original.clone() as DailyTodo

        // Then the copy should have the same properties as the original
        assertEquals(original.title, copy.title)
        assertEquals(original.frequency, copy.frequency)
        assertEquals(original.alarmTime, copy.alarmTime)
        assertEquals(original.uniqueId, copy.uniqueId)
        assertEquals(original.getTimesSnoozedSinceLastCompletion(), copy.getTimesSnoozedSinceLastCompletion())
        assertEquals(original.getLastOccurrence(), copy.getLastOccurrence())

        // And when the copy is modified, the original should not be affected
        copy.markCompleted()

        assertNotEquals("Marking copy completed should not reset original's snoozes.", original.getTimesSnoozedSinceLastCompletion(), copy.getTimesSnoozedSinceLastCompletion())
        assertNotEquals("Modifying copy's last occurrence should not change original's.", original.getLastOccurrence(), copy.getLastOccurrence())
    }

    // Helper function to convert Date to Calendar and get a field
    private fun getCalendarField(date: java.util.Date, field: Int): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(field)
    }
}
