package com.fibelatti.pinboard.features.share

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fibelatti.core.extension.toast
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.features.MainActivity
import com.fibelatti.pinboard.features.PushNotifications
import java.util.Objects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareReceiverWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val shareReceiver: ShareReceiver,
) : CoroutineWorker(context, workerParams) {

    companion object {

        private const val DATA_KEY_BOOKMARK = "KEY_BOOKMARK"
        private const val NOTIFICATION_CHANNEL_ID_PENDING = "NOTIFICATION_CHANNEL_BOOKMARK_SHARING_PENDING"
        private const val NOTIFICATION_CHANNEL_ID_RESULT = "NOTIFICATION_CHANNEL_BOOKMARK_SHARING_RESULT"
        private const val NOTIFICATION_ID_PENDING = 1001
        private const val NOTIFICATION_URL_MAX_LENGTH = 25

        fun createData(url: String): Data = workDataOf(DATA_KEY_BOOKMARK to url)
    }

    override suspend fun doWork(): Result {
        val bookmarkUrl = inputData.getString(DATA_KEY_BOOKMARK) ?: return Result.failure()

        setupNotificationChannels()
        setupImmediateExecution(applicationContext.getString(R.string.push_notifications_bookmark_pending, bookmarkUrl))

        val notificationId = Objects.hash(bookmarkUrl)

        when (val result = shareReceiver(bookmarkUrl)) {
            is ShareReceiver.Result.NotSaved -> {
                val notificationFailure = PushNotifications.createNotification(
                    context = applicationContext,
                    channelId = NOTIFICATION_CHANNEL_ID_RESULT,
                    contentText = applicationContext.getString(
                        R.string.push_notifications_bookmark_failed,
                        bookmarkUrl.truncate(NOTIFICATION_URL_MAX_LENGTH)
                    ),
                )
                PushNotifications.notify(applicationContext, notificationId, notificationFailure)
            }
            is ShareReceiver.Result.Parsed -> {
                val intent = MainActivity.Builder(applicationContext).newTask().setPost(result.post).build()
                applicationContext.startActivity(intent)
            }
            is ShareReceiver.Result.Saved -> {
                withContext(Dispatchers.Main) {
                    applicationContext.toast(applicationContext.getString(R.string.push_notifications_bookmark_saved))
                }
            }
            is ShareReceiver.Result.Notify -> {
                val notificationSaved = PushNotifications.createNotification(
                    context = applicationContext,
                    channelId = NOTIFICATION_CHANNEL_ID_RESULT,
                    contentText = applicationContext.getString(
                        R.string.push_notifications_bookmark_notify,
                        bookmarkUrl.truncate(NOTIFICATION_URL_MAX_LENGTH)
                    ),
                    contentIntent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        MainActivity.Builder(applicationContext).setPost(result.post).build(),
                        0
                    )
                )
                PushNotifications.notify(applicationContext, notificationId, notificationSaved)
            }
        }

        return Result.success()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        PushNotifications.createChannel(
            context = applicationContext,
            channelId = NOTIFICATION_CHANNEL_ID_PENDING,
            channelName = applicationContext.getString(R.string.push_notifications_channel_name_pending),
            channelDescription = applicationContext.getString(R.string.push_notifications_channel_description_pending),
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        PushNotifications.createChannel(
            context = applicationContext,
            channelId = NOTIFICATION_CHANNEL_ID_RESULT,
            channelName = applicationContext.getString(R.string.push_notifications_channel_name_result),
            channelDescription = applicationContext.getString(R.string.push_notifications_channel_description_result),
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
        )
    }

    private suspend fun setupImmediateExecution(pushNotificationContentText: String) {
        val notificationPending = PushNotifications.createNotification(
            context = applicationContext,
            channelId = NOTIFICATION_CHANNEL_ID_PENDING,
            contentText = pushNotificationContentText,
            onGoing = true,
        )

        val foregroundInfo = ForegroundInfo(NOTIFICATION_ID_PENDING, notificationPending)

        setForeground(foregroundInfo)
    }

    private fun String.truncate(length: Int): String = if (this.length > length) {
        this.take(length) + "..."
    } else {
        this
    }
}
