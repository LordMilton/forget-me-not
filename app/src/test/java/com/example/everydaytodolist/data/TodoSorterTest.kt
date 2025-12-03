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
    private lateinit var todoA: Todo // Due earliest, snoozed most, created earliest, title "A"
    private lateinit var todoZ: Todo // Due middle, snoozed least, created middle, title "Z"
    private lateinit var todoM: Todo // Due latest, snoozed middle, created latest, title "M"
    private lateinit var todoK: Todo // Same snooze count as todoM, but due earlier, title "K"

    private lateinit var toBeSortedList: MutableList<Todo>

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
        // uniqueId is managed by the Todo class constructor, so we don't need to specify it
        // to get sequential IDs.
        todoA = Todo(title = "A Task", frequencyInDays = 1, alarmTime = LocalTime.NOON)
        todoZ = Todo(title = "Z Task", frequencyInDays = 1, alarmTime = LocalTime.NOON)
        todoK = Todo(title = "K Task", frequencyInDays = 1, alarmTime = LocalTime.NOON)
        todoM = Todo(title = "M Task", frequencyInDays = 1, alarmTime = LocalTime.NOON)

        // --- Set up test conditions using reflection ---
        val time1 = 100000000L // Earliest
        val time2 = 200000000L
        val time3 = 300000000L
        val time4 = 400000000L // Latest

        // todoA: Due middle, Snoozed Most
        todoA.setPrivateProperty("nextOccurrence", Calendar.getInstance().apply { timeInMillis = time2 })
        todoA.setPrivateProperty("timesSnoozedSinceLastCompletion", 5)

        // todoZ: Due second-latest, Snoozed Least
        todoZ.setPrivateProperty("nextOccurrence", Calendar.getInstance().apply { timeInMillis = time3 })
        todoZ.setPrivateProperty("timesSnoozedSinceLastCompletion", 0)

        // todoK: Due earliest, Snoozed Middle
        todoK.setPrivateProperty("nextOccurrence", Calendar.getInstance().apply { timeInMillis = time1 })
        todoK.setPrivateProperty("timesSnoozedSinceLastCompletion", 2)

        // todoM: Due latest, Snoozed Middle (same as K)
        todoM.setPrivateProperty("nextOccurrence", Calendar.getInstance().apply { timeInMillis = time4 })
        todoM.setPrivateProperty("timesSnoozedSinceLastCompletion", 2)

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
