package com.example.everydaytodolist.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.everydaytodolist.createNotificationChannel
import com.example.everydaytodolist.receivers.AlarmReceiver
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalTime
import java.util.Calendar

class TodoListUtil {
    companion object Factory {

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
                    val uniqueId = (propertyMap["uniqueId"]?.toInt() ?: { parseIssue = true; Todo.getNextUniqueId() }) as Int
                    val timesSnoozed = (propertyMap["timesSnoozedSinceLastCompletion"]?.toInt() ?: { parseIssue = true; 0 }) as Int
                    // Get the occurrence times and fix those in the newly made todo
                    val lastOccurrence = {
                        val time = (propertyMap["lastOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = time
                        calendar
                    }()
                    val nextOccurrence = {
                        val time = (propertyMap["nextOccurrence"]?.toLong() ?: { parseIssue = true; 1 }) as Long
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = time
                        calendar
                    }()

                    // Create the Todo object
                    val todo = Todo(title, frequencyInDays, alarmTime, uniqueId, lastOccurrence, nextOccurrence, timesSnoozed)

                    if(parseIssue) {
                        println("A stored todo was missing values, providing defaults")
                    }
                    todoList.add(todo)
                }
            } catch (e: Exception) {
                println("Could not read todos from internal storage: ${e.message}")
            }
            return todoList
        }

        fun createNotificationAlarms(context: Context, todoList: List<Todo>) {
            if(ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED)
            {
                createNotificationChannel(context)
                val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
                val baseTodoIdUri = "content://todos".toUri()
                for (todo in todoList) {
                    val todoIdUri =
                        Uri.withAppendedPath(baseTodoIdUri, todo.getUniqueId().toString())
                    val notificationIntent = Intent(
                        "Todo Notification",
                        todoIdUri,
                        context,
                        AlarmReceiver::class.java
                    )
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        1,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        todo.getNextOccurrenceTime().time,
                        pendingIntent
                    )
                }
            }
        }

        fun snoozeIncompleteTodosToToday(todoList: List<Todo>): List<Todo> {
            val today = Calendar.getInstance()
            for(todo in todoList) {
                if(todo.dueBeforeToday()) {
                    todo.snoozeUntil(today)
                }
            }

            return todoList
        }
    }
}