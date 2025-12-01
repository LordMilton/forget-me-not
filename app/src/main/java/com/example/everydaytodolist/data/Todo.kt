package com.example.everydaytodolist.data

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Calendar //DO NOT USE THE ANDROID IMPORT, it breaks the unit tests and I am _not_ mocking out Calendar, that's dumb
import java.time.LocalTime
import java.util.Date

//TODO Interface for different types of Todos
class Todo(
    var title: String = "New Todo", //TODO Get this into a string resource
    frequencyInDays: Int = 1,
    alarmTime: LocalTime = LocalTime.of(9,0),
    private val uniqueId: Int = getNextUniqueId()
) {
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
                fixNextOccurrenceTime()
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
        println("Created new Todo: $this")
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

    private fun calculateNextOccurrence(
        from: Calendar = lastOccurrence,
        snoozeLength: Int = frequencyInDays
    ): Calendar {
        val calendar = from.clone() as Calendar
        calendar.isLenient = true
        calendar.add(Calendar.DAY_OF_YEAR, snoozeLength)
        // If the nextOccurrence ends up being in the past, bump it forward to today
        val today = Calendar.getInstance().apply{
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if(today.after(calendar)) {
            calendar.apply{
                set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
                set(Calendar.YEAR, today.get(Calendar.YEAR))
            }
        }
        calendar.set(Calendar.HOUR_OF_DAY, alarmTime.hour)
        calendar.set(Calendar.MINUTE, alarmTime.minute)
        calendar.set(Calendar.SECOND, 0)
        return calendar
    }

    private fun fixNextOccurrenceTime() {
        val calendar = nextOccurrence.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, alarmTime.hour)
        calendar.set(Calendar.MINUTE, alarmTime.minute)
        calendar.set(Calendar.SECOND, 0)

        nextOccurrence = calendar
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

        fun getNextUniqueId(): Int{
            return ++lastUniqueId
        }

        fun copy(other: Todo): Todo { //TODO Should probably change this by making Todo Cloneable and implementing this as clone()
            var todoCopy = Todo(other.title, other.frequencyInDays, other.alarmTime, other.getUniqueId())
            todoCopy.lastOccurrence = other.lastOccurrence
            todoCopy.nextOccurrence = other.nextOccurrence
            todoCopy.timesSnoozedSinceLastCompletion = other.timesSnoozedSinceLastCompletion
            return todoCopy
        }

        fun writeTodosToFile(todoList: List<Todo>, file: File): Boolean {
            var success = true
            val writer = BufferedWriter(FileWriter(file))
            try {
                for (todo in todoList) {
                    writer.write(todo.toString() + "\n")
                }
            } catch(e: IOException) {
                println("Could not write todos to internal storage: $e")
                success = false
            } finally {
                writer.close()
            }
            return success
        }

        fun readTodosFromFile(file: File): List<Todo>? {
            val todoList = mutableListOf<Todo>()
            if (!file.exists()) {
                println("Todo list storage file did not exist")
                return null
            }

            try {
                file.forEachLine { line ->
                    println(line)
                    // Line format: Todo(title='...', frequencyInDays=..., alarmTime=..., uniqueId=..., nextOccurrence=..., lastOccurrence=..., timesSnoozedSinceLastCompletion=...)
                    val properties = line.substringAfter("Todo(").substringBeforeLast(")")
                    val propertyMap = properties.split(", ").associate {
                        val (key, value) = it.split("=", limit = 2)
                        key.trim() to value.trim()
                    }
                    
                    var parseIssue = false

                    val title = (propertyMap["title"]?.removeSurrounding("'") ?: { parseIssue = true; "New Todo" }) as String
                    val frequencyInDays = (propertyMap["frequencyInDays"]?.toInt() ?: { parseIssue = true; 1 }) as Int
                    val alarmTime = (propertyMap["alarmTime"]?.let { LocalTime.parse(it) } ?: { parseIssue = true; LocalTime.of(9, 0) }) as LocalTime
                    val uniqueId = (propertyMap["uniqueId"]?.toInt() ?: { parseIssue = true; getNextUniqueId() }) as Int
                    val timesSnoozed = (propertyMap["timesSnoozedSinceLastCompletion"]?.toInt() ?: { parseIssue = true; 0 }) as Int

                    // Create the Todo object
                    val todo = Todo(title, frequencyInDays, alarmTime, uniqueId)
                    todo.timesSnoozedSinceLastCompletion = timesSnoozed

                    // Get the occurrence times and fix those in the newly made todo
                    val lastOccurrence = {
                        val time = (propertyMap["lastOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
                        val calendar = todo.lastOccurrence
                        calendar.timeInMillis = time
                        calendar
                    }()
                    val nextOccurrence = {
                        val time = (propertyMap["nextOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
                        val calendar = todo.nextOccurrence
                        calendar.timeInMillis = time
                        calendar
                    }()
                    todo.lastOccurrence = lastOccurrence
                    todo.nextOccurrence = nextOccurrence

                    if(parseIssue) {
                        println("A stored todo was missing values, providing defaults")
                    }
                    todoList.add(todo)
                }
            } catch (e: Exception) {
                println("Could not read todos from internal storage: ${e.message}")
                // Depending on desired behavior, you might want to return an empty list or re-throw the exception
            }
            return todoList
        }

    }
}