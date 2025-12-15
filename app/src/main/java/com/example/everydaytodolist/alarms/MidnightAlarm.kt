package com.example.everydaytodolist.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.everydaytodolist.preferencesDataStore
import com.example.everydaytodolist.receivers.AlarmReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class MidnightAlarm {

    companion object Factory {
        suspend fun createMidnightAlarms(context: Context) {
            if (isFirstRunAfterBootFlow(context).first()) {
                val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
                val nextMidnight = Calendar.getInstance()
                nextMidnight.apply {
                    isLenient = true
                    set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) + 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val midnightIntent = Intent(
                    "Midnight",
                    "".toUri(), // Doesn't need data
                    context,
                    AlarmReceiver::class.java
                )
                val midnightPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    midnightIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    (1000 * 60 * 60 * 24),
                    midnightPendingIntent
                )
            }
        }

        fun isFirstRunAfterBootFlow(context: Context): Flow<Boolean> {
            return context.preferencesDataStore.data.map { preferences ->
                preferences[booleanPreferencesKey("first_run_after_boot")] ?: true
            }
        }
    }
}