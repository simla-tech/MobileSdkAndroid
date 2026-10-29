package com.simla.android.sdk.tracker

import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkRawHttpRequest
import kotlinx.coroutines.CompletableDeferred
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

internal class BlockingEventNetworkEngine(
    private val trackerEnabled: Boolean,
) : SdkHttpNetworkEngine {
    val firstEventRequestStarted = CompletableDeferred<Unit>()
    val releaseFirstEventRequest = CompletableDeferred<Unit>()
    val maxActiveEventRequests = AtomicInteger()
    val eventRequestBodies = Collections.synchronizedList(mutableListOf<String>())

    private val eventRequestCount = AtomicInteger()
    private val activeEventRequests = AtomicInteger()

    override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse {
        val path = request.url.substringBefore('?').substringAfterLast('/')
        if (path == "connect") {
            return SdkHttpResponse(
                code = 200,
                body = """{"ts":"2026-07-27T00:00:00Z","user":{"token":"user-token"},"session":{"id":1},"behaviour":{"widget":false,"tracker":$trackerEnabled}}""",
            )
        }

        val requestNumber = eventRequestCount.incrementAndGet()
        val activeRequests = activeEventRequests.incrementAndGet()
        maxActiveEventRequests.updateAndGet { current -> maxOf(current, activeRequests) }
        eventRequestBodies += request.body
        return try {
            if (requestNumber == 1) {
                firstEventRequestStarted.complete(Unit)
                releaseFirstEventRequest.await()
            }
            SdkHttpResponse(code = 200, body = "{}")
        } finally {
            activeEventRequests.decrementAndGet()
        }
    }

    override suspend fun postRaw(request: SdkRawHttpRequest) = SdkHttpResponse(200)

    override suspend fun getJson(request: SdkHttpRequest) = SdkHttpResponse(
        code = 200,
        body = """{"ts":"2026-07-27T00:00:00Z"}""",
    )

    override fun close() = Unit
}
