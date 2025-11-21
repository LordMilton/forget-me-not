package com.example.everydaytodolist.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.everydaytodolist.R
import com.example.everydaytodolist.data.Todo

class NotificationFactory(
    private val context: Context
) {
    fun createTodoDue(data: Todo): Notification {
        val builder = NotificationCompat.Builder(context, "Todo")
            .setSmallIcon(R.drawable.alarm_notification)
            .setContentTitle(data.title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setWhen(data.getNextOccurrenceTime().time)
        val notification = builder.build()
        return notification
    }
}