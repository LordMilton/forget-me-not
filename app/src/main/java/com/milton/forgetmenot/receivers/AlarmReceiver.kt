package com.milton.forgetmenot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.milton.forgetmenot.R
import com.milton.forgetmenot.data.TodoListUtil
import com.milton.forgetmenot.data.todos.ITodo
import com.milton.forgetmenot.getTodoFromListById
import com.milton.forgetmenot.notifications.NotificationFactory
import java.io.File

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
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val todoId = job.data?.lastPathSegment?.toIntOrNull() ?: -1
            if (todoId >= 0) {
                // Safe way to have the todolist filename be global? I don't think making it an app string constant is safe (bla bla, not supposed to be obvious what a storage file would be called)
                val todoList =
                    TodoListUtil.readTodosFromFile(File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))
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
        var todoList =
            TodoListUtil.readTodosFromFile(File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))
                ?: listOf()
        todoList = TodoListUtil.snoozeIncompleteTodosToToday(todoList)

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            var snoozedToTodayList = mutableListOf<ITodo>()
            todoList.forEach {
                if (it.dueToday() && it.getTimesSnoozedSinceLastCompletion() > 0)
                    snoozedToTodayList.add(it)
            }
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationFac = NotificationFactory(context)
            val notification = notificationFac.createSnoozedReminder(snoozedToTodayList)
            if(notification != null) {
                notificationManager.notify(-50, notification)
            }
        }

        TodoListUtil.createNotificationAlarms(context, todoList)

        TodoListUtil.writeTodosToFile(todoList, File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))
    }
}