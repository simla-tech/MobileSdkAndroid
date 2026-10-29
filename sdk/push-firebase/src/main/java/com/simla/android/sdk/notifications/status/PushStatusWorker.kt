package com.simla.android.sdk.notifications.status

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL

internal class PushStatusWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val endpoint = inputData.getString(KEY_ENDPOINT)
            ?: return@withContext invalidInput(KEY_ENDPOINT)
        val siteToken = inputData.getString(KEY_SITE_TOKEN)
            ?: return@withContext invalidInput(KEY_SITE_TOKEN)
        val pushId = inputData.getString(KEY_PUSH_ID)
            ?: return@withContext invalidInput(KEY_PUSH_ID)
        val status = inputData.getString(KEY_STATUS)
            ?: return@withContext invalidInput(KEY_STATUS)
        val changedAt = inputData.getLong(KEY_CHANGED_AT, 0L)
        val buttonHash = inputData.getString(KEY_BUTTON_HASH)
        if (changedAt <= 0L) return@withContext invalidInput(KEY_CHANGED_AT)
        if (PushStatus.entries.none { it.apiValue == status }) {
            CrmPushLogger.log(
                "Push status sending failed: pushId=$pushId, invalid status=$status",
            )
            return@withContext Result.failure()
        }

        CrmPushLogger.log {
            "Push status sending: pushId=$pushId, status=$status, changedAt=$changedAt, " +
                    "attempt=${runAttemptCount + 1}, workId=$id"
        }

        val body = PushStatusJson.encodeToString(
            PushStatusRequest(
                pushId = pushId,
                status = status,
                changedAt = changedAt,
                buttonHash = buttonHash,
            ),
        )

        runCatching { send(endpoint, siteToken, body) }
            .fold(
                onSuccess = { responseCode ->
                    val decision = responseCode.toWorkDecision()
                    CrmPushLogger.log(
                        "Push status response received: pushId=$pushId, status=$status, " +
                                "httpCode=$responseCode, outcome=${decision.outcome}",
                    )
                    decision.result
                },
                onFailure = { error ->
                    val decision = retryOrFailureDecision()
                    CrmPushLogger.logError(
                        "Push status request failed: pushId=$pushId, status=$status, " +
                                "attempt=${runAttemptCount + 1}, outcome=${decision.outcome}",
                        error,
                    )
                    decision.result
                },
            )
    }

    private fun send(endpoint: String, siteToken: String, body: String): Int {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = NETWORK_TIMEOUT_MS
            connection.readTimeout = NETWORK_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Site-Token", siteToken)
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun Int.toWorkDecision(): WorkDecision = when {
        this == HttpURLConnection.HTTP_OK -> WorkDecision(Result.success(), "success")
        this == HttpURLConnection.HTTP_CLIENT_TIMEOUT -> retryOrFailureDecision()
        this == 425 || this == 429 -> retryOrFailureDecision()
        this >= HttpURLConnection.HTTP_INTERNAL_ERROR -> retryOrFailureDecision()
        else -> WorkDecision(Result.failure(), "failure")
    }

    private fun retryOrFailureDecision(): WorkDecision =
        if (runAttemptCount < MAX_ATTEMPTS) {
            WorkDecision(Result.retry(), "retry")
        } else {
            WorkDecision(Result.failure(), "failure (max attempts reached)")
        }

    private fun invalidInput(key: String): Result {
        CrmPushLogger.log(
            "Push status sending failed: invalid or missing WorkManager input key=$key",
        )
        return Result.failure()
    }

    private data class WorkDecision(
        val result: Result,
        val outcome: String,
    )

    internal companion object {
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_SITE_TOKEN = "site_token"
        const val KEY_PUSH_ID = "push_id"
        const val KEY_STATUS = "status"
        const val KEY_CHANGED_AT = "changed_at"
        const val KEY_BUTTON_HASH = "button_hash"
        private const val NETWORK_TIMEOUT_MS = 30_000
        private const val MAX_ATTEMPTS = 5
    }
}
