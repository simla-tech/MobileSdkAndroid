package com.simla.android.sdk.tracker

import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.storage.SdkSessionStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StandaloneTrackerConnection(
    val config: CrmTrackerConfig,
    val sessionStorage: SdkSessionStorage,
) {
    val stateMutex = Mutex()
    var activeSession: SdkSession? = null

    fun endpoint(path: String): String = "${config.clientApiBaseUrl}/${path.trimStart('/')}"

    fun baseHeaders(): Map<String, String> = mapOf(
        "X-Site-Token" to config.siteToken,
        "Accept" to "application/json",
    )

    suspend fun authorizedHeaders(): Map<String, String> {
        val session = stateMutex.withLock { activeSession }
            ?: throw IllegalStateException("connect() must be called before tracker requests")
        return baseHeaders() + mapOf(
            "X-User-Token" to session.userToken,
            "X-Session-Id" to session.sessionId,
        )
    }
}
