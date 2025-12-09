package com.example.everydaytodolist.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.everydaytodolist.data.TodoListUtil
import com.example.everydaytodolist.R
import java.io.File

class OnBootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, job: Intent) {
        println("Received broadcast")
        when(job.action) {
            Intent.ACTION_REBOOT -> doRebootActions(context, job)
            else -> println("Action \"${job.action}\" not covered")
        }
    }

    fun doRebootActions(context: Context, job: Intent) {
        var todoList =
            TodoListUtil.readTodosFromFile(File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))
                ?: listOf()

        // Snoozing any incomplete todos until today in case the device was off between days
        todoList = TodoListUtil.snoozeIncompleteTodosToToday(todoList)
        TodoListUtil.writeTodosToFile(todoList, File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))

        // Reinitialize all the todo alarms
        TodoListUtil.createNotificationAlarms(context, todoList)
    }
}