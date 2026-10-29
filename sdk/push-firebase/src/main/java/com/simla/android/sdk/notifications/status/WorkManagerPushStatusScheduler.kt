package com.simla.android.sdk.notifications.status

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal class WorkManagerPushStatusScheduler(
    private val endpoint: String,
    private val siteToken: String,
) : PushStatusScheduler {
    override fun enqueue(
        context: Context,
        pushId: String,
        status: PushStatus,
        changedAtSeconds: Long,
        buttonHash: String?,
    ) {
        val inputData = Data.Builder()
            .putString(PushStatusWorker.KEY_ENDPOINT, endpoint)
            .putString(PushStatusWorker.KEY_SITE_TOKEN, siteToken)
            .putString(PushStatusWorker.KEY_PUSH_ID, pushId)
            .putString(PushStatusWorker.KEY_STATUS, status.apiValue)
            .putLong(PushStatusWorker.KEY_CHANGED_AT, changedAtSeconds)
            .apply {
                buttonHash?.let { putString(PushStatusWorker.KEY_BUTTON_HASH, it) }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<PushStatusWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .build()

        val workName = uniqueWorkName(pushId, status, buttonHash)
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,
            request,
        )
        CrmPushLogger.log {
            "Push status schedule requested: pushId=$pushId, status=${status.apiValue}, " +
                    "changedAt=$changedAtSeconds, workId=${request.id}, workName=$workName, " +
                    "policy=KEEP"
        }
    }

    private fun uniqueWorkName(pushId: String, status: PushStatus, buttonHash: String?): String {
        val identity = buttonHash?.let { "$pushId\u0000$it" } ?: pushId
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "crm-push-status-$digest-${status.apiValue}"
    }

    private companion object {
        const val MIN_BACKOFF_SECONDS = 10L
    }
}
