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

        fun writeTodosToFile(todoList: List<ITodo>, file: File): Boolean {
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

        fun readTodosFromFile(file: File): List<ITodo>? {
            val todoList = mutableListOf<ITodo>()
            if (!file.exists()) {
                println("Todo list storage file did not exist")
                return null
            }

            try {
                file.forEachLine { line ->
                    println(line)
                    // Line format: DailyTodo(title='...', frequencyInDays=..., alarmTime=..., uniqueId=..., nextOccurrence=..., lastOccurrence=..., timesSnoozedSinceLastCompletion=...)
                    val todoTypeString = line.substringBefore("Todo")
                    val properties = line.substringAfter("Todo(").substringBeforeLast(")")
                    val propertyMap = properties.split(", ").associate {
                        val (key, value) = it.split("=", limit = 2)
                        key.trim() to value.trim()
                    }

                    // Create the TodoObject
                    val todo = when(todoTypeString) {
                        "Daily","" -> DailyTodo()
                        else -> null
                    }?.fromPropertiesMap(propertyMap)

                    when(todo) {
                        null -> println("A stored todo was missing values, providing defaults")
                        else -> todoList.add(todo)
                    }
                }
            } catch (e: Exception) {
                println("Could not read todos from internal storage: ${e.message}")
            }
            return todoList
        }

        fun createNotificationAlarms(context: Context, todoList: List<ITodo>) {
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
                        Uri.withAppendedPath(baseTodoIdUri, todo.uniqueId.toString())
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
                        todo.getNextOccurrence().time,
                        pendingIntent
                    )
                }
            }
        }

        fun snoozeIncompleteTodosToToday(todoList: List<ITodo>): List<ITodo> {
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