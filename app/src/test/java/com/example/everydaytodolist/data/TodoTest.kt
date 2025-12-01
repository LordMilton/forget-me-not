package com.example.everydaytodolist.data

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class TodoTest {

    @Test
    fun `constructor initializes with default values`() {
        // When a Todo is created with no arguments
        val todo = Todo()

        // Then it should have the default properties
        assertEquals("New Todo", todo.title)
        assertEquals(1, todo.frequencyInDays)
        assertEquals(LocalTime.of(9, 0), todo.alarmTime)
        assertEquals(0, todo.timesSnoozedSinceLastCompletion)
    }

    @Test
    fun `constructor initializes with provided values`() {
        // When a Todo is created with specific arguments
        val alarm = LocalTime.of(15, 30)
        val todo = Todo(title = "Test Task", frequencyInDays = 5, alarmTime = alarm)

        // Then it should have the provided properties
        assertEquals("Test Task", todo.title)
        assertEquals(5, todo.frequencyInDays)
        assertEquals(alarm, todo.alarmTime)
    }

    @Test
    fun `completedToday returns false for a new todo`() {
        // Given a newly created Todo
        val todo = Todo()

        // When we check if it was completed today
        // Then the result should be false, as its last occurrence is initialized to yesterday
        assertFalse("A new todo should not be marked as completed today.", todo.completedToday())
    }

    @Test
    fun `markCompleted sets lastOccurrence to today and resets snoozes`() {
        // Given a todo that has been snoozed
        val todo = Todo()
        todo.snooze(2) // Snooze for 2 days
        assertEquals(1, todo.timesSnoozedSinceLastCompletion)

        // When markCompleted is called
        todo.markCompleted()

        // Then it should be marked as completed today
        assertTrue("Todo should be marked as completed today.", todo.completedToday())
        // And the snooze count should be reset
        assertEquals("Snooze count should be reset to 0.", 0, todo.timesSnoozedSinceLastCompletion)
    }

    @Test
    fun `markCompleted updates nextOccurrence correctly`() {
        // Given a todo with a frequency of 7 days and an alarm at 10:00
        val alarmTime = LocalTime.of(10, 0)
        val todo = Todo(frequencyInDays = 7, alarmTime = alarmTime)

        // When it's marked as completed
        todo.markCompleted()
        val today = Calendar.getInstance()

        // Then the next occurrence should be 7 days from today at the specified alarm time
        val nextOccurrence = todo.getNextOccurrenceTime()
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
        // Given a todo
        val todo = Todo(frequencyInDays = 1, alarmTime = LocalTime.of(9, 0))
        val initialNextOccurrence = todo.getNextOccurrenceTime()

        // When the todo alarmTime is changed
        val newAlarmTime = LocalTime.of(12, 30)
        todo.alarmTime = newAlarmTime

        // Then the alarmTime should be changed
        assertEquals(newAlarmTime, todo.alarmTime)

        // And the next occurrence should be the same day as before but with the new alarmTime
        val expectedNextOccurrence = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, getCalendarField(initialNextOccurrence, Calendar.DAY_OF_YEAR))
            set(Calendar.HOUR, newAlarmTime.hour)
            set(Calendar.MINUTE, newAlarmTime.minute)
        }
        val newNextOccurrence = todo.getNextOccurrenceTime()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
        assertEquals(expectedNextOccurrence.get(Calendar.HOUR), getCalendarField(newNextOccurrence, Calendar.HOUR))
        assertEquals(expectedNextOccurrence.get(Calendar.MINUTE), getCalendarField(newNextOccurrence, Calendar.MINUTE))
    }

    @Test
    fun `snooze increments snooze count and delays nextOccurrence`() {
        // Given a todo
        val todo = Todo()
        val initialNextOccurrence = todo.getNextOccurrenceTime()

        // When the todo is snoozed for 3 days
        val snoozeLength = 3
        todo.snooze(snoozeLength)

        // Then the snooze count should increase
        assertEquals(1, todo.timesSnoozedSinceLastCompletion)

        // And the next occurrence should be postponed by 3 days
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, snoozeLength)
        }
        val newNextOccurrence = todo.getNextOccurrenceTime()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `snooze delays even further when used multiple times`() {
        // Given a todo
        var todo = Todo()
        val initialNextOccurrence = todo.getNextOccurrenceTime()

        // When the todo is snoozed for 1 day, twice
        val snoozeLength = 1
        val snoozeCount = 2
        repeat(snoozeCount) {
            todo.snooze(snoozeLength)
            // Copying has to happen to force recomposition in the real app, so make sure it doesn't cause weird things here too
            todo = Todo.copy(todo)
        }

        // Then the snooze count should increase twice
        assertEquals(2, todo.timesSnoozedSinceLastCompletion)

        // And the next occurrence should be postponed by the snooze amount twice
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, snoozeLength * snoozeCount)
        }
        val newNextOccurrence = todo.getNextOccurrenceTime()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `snooze with default length uses 1 day`() {
        // Given a todo
        val todo = Todo()
        val initialNextOccurrence = todo.getNextOccurrenceTime()

        // When the todo is snoozed with the default length
        todo.snooze() // Default is 1 day

        // Then the snooze count should increase
        assertEquals(1, todo.timesSnoozedSinceLastCompletion)

        // And the next occurrence should be postponed by 1 day
        val expectedNextOccurrence = Calendar.getInstance().apply {
            time = initialNextOccurrence
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val newNextOccurrence = todo.getNextOccurrenceTime()

        assertEquals(expectedNextOccurrence.get(Calendar.DAY_OF_YEAR), getCalendarField(newNextOccurrence, Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `Factory copy creates a deep enough copy`() {
        // Given an original todo
        val original = Todo("Original Task", 10, LocalTime.of(12, 0))
        original.markCompleted()
        original.snooze(2)

        // When a copy is made
        val copy = Todo.Factory.copy(original)

        // Then the copy should have the same properties as the original
        assertEquals(original.title, copy.title)
        assertEquals(original.frequencyInDays, copy.frequencyInDays)
        assertEquals(original.alarmTime, copy.alarmTime)
        assertEquals(original.getUniqueId(), copy.getUniqueId())
        assertEquals(original.timesSnoozedSinceLastCompletion, copy.timesSnoozedSinceLastCompletion)
        assertEquals(original.getLastOccurrenceTime(), copy.getLastOccurrenceTime())

        // And when the copy is modified, the original should not be affected
        copy.title = "Modified Task"
        copy.markCompleted()

        assertNotEquals("Modifying copy's title should not change original's title.", original.title, copy.title)
        assertNotEquals("Modifying copy's last occurrence should not change original's.", original.getLastOccurrenceTime(), copy.getLastOccurrenceTime())
    }

    // Helper function to convert Date to Calendar and get a field
    private fun getCalendarField(date: java.util.Date, field: Int): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(field)
    }
}
