package com.example.everydaytodolist.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.everydaytodolist.R
import com.example.everydaytodolist.alarms.MidnightAlarm
import com.example.everydaytodolist.data.TodoListUtil
import com.example.everydaytodolist.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.io.File

class OnBootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, job: Intent) {
        println("Received broadcast")
        when(job.action) {
            Intent.ACTION_BOOT_COMPLETED -> runBlocking { doRebootActions(context, job) }
            else -> println("Action \"${job.action}\" not covered")
        }
    }

    fun CoroutineScope.doRebootActions(context: Context, job: Intent) {
        launch(Dispatchers.Default) {
            indicateFirstRunAfterBoot(context)
            MidnightAlarm.createMidnightAlarms(context)
        }

        var todoList =
            TodoListUtil.readTodosFromFile(File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))
                ?: listOf()

        // Snoozing any incomplete todos until today in case the device was off between days
        todoList = TodoListUtil.snoozeIncompleteTodosToToday(todoList)
        TodoListUtil.writeTodosToFile(todoList, File(context.filesDir, context.resources.getString(R.string.todo_storage_file)))

        // Reinitialize all the todo alarms
        TodoListUtil.createNotificationAlarms(context, todoList)
    }

    suspend fun indicateFirstRunAfterBoot(context: Context) {
        context.preferencesDataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[booleanPreferencesKey("first_run_after_boot")] = true
            }
        }
    }
}