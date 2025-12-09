package com.example.everydaytodolist.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.data.TodoListUtil
import com.example.everydaytodolist.getTodoFromListById
import com.example.everydaytodolist.notifications.NotificationFactory
import java.io.File
import java.util.Calendar

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, job: Intent) {
        println("Received alarm broadcast")
        when(job.action) {
            "Todo Notification" -> createTodoNotification(context, job)
            "Midnight" -> performMidnightTodoRollover(context, job)
            else -> println("Action \"${job.action}\" not covered")
        }
    }

    fun createTodoNotification(context: Context, job: Intent) {
        println("Received notification broadcast")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val todoId = job.data?.lastPathSegment?.toIntOrNull() ?: -1
            if (todoId >= 0) {
                // Safe way to have the todolist filename be global? I don't think making it an app string constant is safe (bla bla, not supposed to be obvious what a storage file would be called)
                val todoList =
                    TodoListUtil.readTodosFromFile(File(context.filesDir, "storedTodoList"))
                        ?: listOf()
                val todo = getTodoFromListById(todoList, todoId).second
                if (todo != null) {
                    val notificationManager = NotificationManagerCompat.from(context)
                    val notificationFac = NotificationFactory(context)
                    val notification = notificationFac.createTodoDue(todo)
                    notificationManager.notify(todoId, notification)
                }
            }
        }
    }

    fun performMidnightTodoRollover(context: Context, job: Intent) {
        println("Received midnight broadcast")
        val todoList =
            TodoListUtil.readTodosFromFile(File(context.filesDir, "storedTodoList"))
                ?: listOf()
        val midnightToday = Calendar.getInstance()
        midnightToday.apply {
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val movedTodos = mutableListOf<Todo>()
        for(todo in todoList) {
            val nextOccurrence = Calendar.getInstance()
            nextOccurrence.time = todo.getNextOccurrenceTime()
            if(nextOccurrence < midnightToday) {
                todo.snooze()
                movedTodos.add(todo)
            }
        }

        TodoListUtil.createNotificationAlarms(context, movedTodos)
    }
}