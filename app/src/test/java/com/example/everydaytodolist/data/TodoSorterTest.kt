package com.example.everydaytodolist.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.util.Calendar
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class TodoSorterTest {

    // Test data declarations
    private lateinit var todoA: DailyTodo // Due earliest, snoozed most, created earliest, title "A"
    private lateinit var todoZ: DailyTodo // Due middle, snoozed least, created middle, title "Z"
    private lateinit var todoM: DailyTodo // Due latest, snoozed middle, created latest, title "M"
    private lateinit var todoK: DailyTodo // Same snooze count as todoM, but due earlier, title "K"

    private lateinit var toBeSortedList: MutableList<ITodo>

    /* Helper function to modify private properties for testing */
    private fun <T: Any> T.setPrivateProperty(name: String, value: Any?) {
        val property = this::class.memberProperties
            .find { it.name == name }
            ?: throw NoSuchFieldException("Property '$name' not found in class ${this::class.simpleName}")

        property.isAccessible = true
        (property as? kotlin.reflect.KMutableProperty<*>)?.setter?.call(this, value)
            ?: throw ClassCastException("Property '$name' is not mutable")
    }

    @Before
    fun setUp() {
        // --- Create Base Todos using the correct constructor ---
        val time1 = 1000000000000L // Earliest
        val time2 = 2000000000000L
        val time3 = 3000000000000L
        val time4 = 4000000000000L // Latest

        // todoA: Due middle, Snoozed Most
        todoA = DailyTodo(
            title = "A Task",
            frequency = 1,
            alarmTime = LocalTime.NOON,
            uniqueId = 1,
            maxOccurrences = null,
            endDate = null,
            lastOccurrence = Calendar.getInstance(),
            nextOccurrence = Calendar.getInstance().apply { timeInMillis = time2 },
            timesSnoozedSinceLastCompletion = 5,
            numOccurrences = 0
        )
        // todoZ: Due second-latest, Snoozed Least
        todoZ = DailyTodo(
            title = "Z Task",
            frequency = 1,
            alarmTime = LocalTime.NOON,
            uniqueId = 2,
            maxOccurrences = null,
            endDate = null,
            lastOccurrence = Calendar.getInstance(),
            nextOccurrence = Calendar.getInstance().apply { timeInMillis = time3 },
            timesSnoozedSinceLastCompletion = 0,
            numOccurrences = 0
        )
        // todoK: Due earliest, Snoozed Middle
        todoK = DailyTodo(
            title = "K Task",
            frequency = 1,
            alarmTime = LocalTime.NOON,
            uniqueId = 3,
            maxOccurrences = null,
            endDate = null,
            lastOccurrence = Calendar.getInstance(),
            nextOccurrence = Calendar.getInstance().apply { timeInMillis = time1 },
            timesSnoozedSinceLastCompletion = 2,
            numOccurrences = 0
        )
        // todoM: Due latest, Snoozed Middle (same as K)
        todoM = DailyTodo(
            title = "M Task",
            frequency = 1,
            alarmTime = LocalTime.NOON,
            uniqueId = 4,
            maxOccurrences = null,
            endDate = null,
            lastOccurrence = Calendar.getInstance(),
            nextOccurrence = Calendar.getInstance().apply { timeInMillis = time4 },
            timesSnoozedSinceLastCompletion = 2,
            numOccurrences = 0
        )

        toBeSortedList = mutableListOf(todoA, todoZ, todoK, todoM)
    }

    @Test
    fun `sort by DUE_DATE sorts todos by next occurrence time correctly`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.DUE_DATE)
        // Expected order: todoK (earliest), todoA, todoZ, todoM
        assertEquals(4, toBeSortedList.size)
        assertEquals(listOf(todoK, todoA, todoZ, todoM), toBeSortedList)
    }

    @Test
    fun `sort by DUE_DATE reversed sorts todos in descending order of due date`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.DUE_DATE, reversed = true)
        // Expected order: todoZ/todoM, todoA, todoK
        assertEquals(4, toBeSortedList.size)
        assertEquals(listOf(todoM, todoZ, todoA, todoK), toBeSortedList)
    }

    @Test
    fun `sort by SNOOZE_COUNT sorts by snooze count, then by due date`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.SNOOZE_COUNT)
        // Expected order:
        // 1. todoZ (0 snoozes)
        // 2. todoK (2 snoozes, but due earlier than todoM)
        // 3. todoM (2 snoozes, but due later than todoK)
        // 4. todoA (5 snoozes)
        assertEquals(listOf(todoZ, todoK, todoM, todoA), toBeSortedList)
    }

    @Test
    fun `sort by SNOOZE_COUNT reversed sorts by descending snooze count, then by due date`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.SNOOZE_COUNT, reversed = true)
        // Expected order (reversed):
        // 1. todoA (5 snoozes)
        // 2. todoM (2 snoozes, due later)
        // 3. todoK (2 snoozes, due earlier)
        // 4. todoZ (0 snoozes)
        assertEquals(listOf(todoA, todoM, todoK, todoZ), toBeSortedList)
    }

    @Test
    fun `sort by CREATED_DATE sorts by unique ID from creation`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.CREATED_DATE)
        // Expected order by creation ID: todoA, todoZ, todoK, todoM
        assertEquals(listOf(todoA, todoZ, todoK, todoM), toBeSortedList)

    }

    @Test
    fun `sort by CREATED_DATE reversed sorts by descending unique ID`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.CREATED_DATE, reversed = true)
        // Expected order (reversed): todoM, todoK, todoZ, todoA
        assertEquals(listOf(todoM, todoK, todoZ, todoA), toBeSortedList)

    }

    @Test
    fun `sort by TITLE_ALPHANUM sorts alphabetically by title`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.TITLE_ALPHANUM)
        // Expected order: "A Task", "K Task", "M Task", "Z Task"
        assertEquals(listOf(todoA, todoK, todoM, todoZ), toBeSortedList)

    }

    @Test
    fun `sort by TITLE_ALPHANUM reversed sorts in reverse alphabetical order`() {
        TodoSorter.sort(toBeSortedList, TodoSorter.SortMethod.TITLE_ALPHANUM, reversed = true)
        // Expected order: "Z Task", "M Task", "K Task", "A Task"
        assertEquals(listOf(todoZ, todoM, todoK, todoA), toBeSortedList)

    }
}
