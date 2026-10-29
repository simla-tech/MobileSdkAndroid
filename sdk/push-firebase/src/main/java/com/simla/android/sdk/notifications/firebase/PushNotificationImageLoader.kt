package com.simla.android.sdk.notifications.firebase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import java.net.HttpURLConnection
import java.net.URL

internal object PushNotificationImageLoader {
    private const val NETWORK_TIMEOUT_MS = 10_000
    private const val MAX_CONTENT_LENGTH_BYTES = 5L * 1024 * 1024

    fun load(imageUrl: String): Bitmap? {
        val url = runCatching { URL(imageUrl) }.getOrNull() ?: return null
        if (url.protocol != "http" && url.protocol != "https") return null

        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = NETWORK_TIMEOUT_MS
            connection.readTimeout = NETWORK_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode !in 200..299) {
                CrmPushLogger.log(
                    "Push image loading failed: url=$imageUrl, " +
                            "httpCode=${connection.responseCode}",
                )
                return null
            }
            if (connection.contentLengthLong > MAX_CONTENT_LENGTH_BYTES) {
                CrmPushLogger.log(
                    "Push image loading skipped: url=$imageUrl, " +
                            "contentLength=${connection.contentLengthLong}",
                )
                return null
            }

            connection.inputStream.buffered().use(BitmapFactory::decodeStream)
                ?.also { bitmap ->
                    CrmPushLogger.log(
                        "Push image loaded: url=$imageUrl, " +
                                "size=${bitmap.width}x${bitmap.height}",
                    )
                }
        } catch (error: Exception) {
            CrmPushLogger.logError("Push image loading failed: url=$imageUrl", error)
            null
        } finally {
            connection.disconnect()
        }
    }
}
