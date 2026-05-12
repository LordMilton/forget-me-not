package com.milton.forgetmenot.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.milton.forgetmenot.MainActivity
import com.milton.forgetmenot.R
import com.milton.forgetmenot.data.todos.ITodo

class NotificationFactory(
    private val context: Context
) {
    fun createTodoDue(data: ITodo): Notification {
        val todoIdUri = Uri.withAppendedPath("content://todos".toUri(), data.uniqueId.toString())

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        intent.data = todoIdUri
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, "Todo")
            .setSmallIcon(R.drawable.alarm_notification)
            .setContentTitle(data.title)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setWhen(data.getNextOccurrence().time)
        val notification = builder.build()
        return notification
    }
}