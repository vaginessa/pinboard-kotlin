package com.fibelatti.pinboard.features

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.fibelatti.core.extension.getSystemService
import com.fibelatti.pinboard.R

object PushNotifications {

    private fun Context.getNotificationManager(): NotificationManager? = getSystemService()

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        channelImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    ) {
        val notificationChannel = NotificationChannel(channelId, channelName, channelImportance).apply {
            description = channelDescription
        }
        context.getNotificationManager()?.createNotificationChannel(notificationChannel)
    }

    fun createNotification(
        context: Context,
        channelId: String,
        contentText: String,
        onGoing: Boolean = false,
        contentIntent: PendingIntent? = null,
        action: NotificationCompat.Action? = null,
    ): Notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_pin)
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .apply {
            if (onGoing) {
                setOngoing(onGoing)
                setProgress(0, 0, true)
            } else {
                setAutoCancel(true)
            }
            contentIntent?.let(::setContentIntent)
            action?.let(::addAction)
        }
        .build()

    fun notify(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ) {
        context.getNotificationManager()?.notify(notificationId, notification)
    }

    fun cancelNotification(
        context: Context,
        notificationId: Int,
    ) {
        context.getNotificationManager()?.cancel(notificationId)
    }
}
