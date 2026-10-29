package com.simla.android.sdk.notifications.firebase

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.simla.android.sdk.notifications.interaction.PushInteractionPendingIntents
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PushNotificationImageWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pushId =
            inputData.getString(KEY_PUSH_ID) ?: return@withContext invalidInput(KEY_PUSH_ID)
        val imageUrl = inputData.getString(KEY_IMAGE_URL)
            ?: return@withContext invalidInput(KEY_IMAGE_URL)
        val notificationId = PushInteractionPendingIntents.notificationId(pushId)
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotification = notificationManager.activeNotifications
            .firstOrNull { it.id == notificationId }
            ?.notification
            ?: return@withContext Result.success()
        val image = PushNotificationImageLoader.load(imageUrl)
            ?: return@withContext Result.success()

        runCatching {
            Notification.Builder.recoverBuilder(applicationContext, activeNotification)
                .setLargeIcon(image)
                .setStyle(
                    Notification.BigPictureStyle()
                        .bigPicture(image)
                        .bigLargeIcon(null as Bitmap?),
                )
                .build()
        }.onSuccess { notification ->
            val isStillActive =
                notificationManager.activeNotifications.any { it.id == notificationId }
            if (isStillActive) {
                notificationManager.notify(notificationId, notification)
                CrmPushLogger.log("Push notification image applied: pushId=$pushId")
            }
        }.onFailure { error ->
            CrmPushLogger.logError("Push notification image update failed: pushId=$pushId", error)
        }

        Result.success()
    }

    private fun invalidInput(key: String): Result {
        CrmPushLogger.log("Push notification image update skipped: missing $key")
        return Result.failure()
    }

    companion object {
        internal const val KEY_PUSH_ID = "push_id"
        internal const val KEY_IMAGE_URL = "image_url"
        private const val WORK_NAME_PREFIX = "crm-push-image-"

        internal fun enqueue(context: Context, pushId: String, imageUrl: String) {
            val inputData = Data.Builder()
                .putString(KEY_PUSH_ID, pushId)
                .putString(KEY_IMAGE_URL, imageUrl)
                .build()
            val request = OneTimeWorkRequestBuilder<PushNotificationImageWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME_PREFIX + pushId,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
