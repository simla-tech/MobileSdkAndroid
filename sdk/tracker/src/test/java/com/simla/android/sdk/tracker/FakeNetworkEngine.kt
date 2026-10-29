package com.simla.android.sdk.tracker

import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkRawHttpRequest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal class FakeNetworkEngine(
    private val trackerEnabled: Boolean,
    private val eventResponseCode: Int = 200,
    private val eventResponseBody: String = "{}",
    private val eventResponseCodes: List<Int>? = null,
    private val consentForm: Boolean = false,
    private val gpc: Boolean = false,
    var userToken: String = "user-token",
) : SdkHttpNetworkEngine {
    private val eventResponseIndex = AtomicInteger()
    val requestPaths = CopyOnWriteArrayList<String>()
    val requestUrls = CopyOnWriteArrayList<String>()
    val requestBodies = CopyOnWriteArrayList<String>()
    val requestHeaders = CopyOnWriteArrayList<Map<String, String>>()
    val getRequestUrls = CopyOnWriteArrayList<String>()

    override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse {
        val path = request.url.substringBefore('?').substringAfterLast('/')
        requestPaths += path
        requestUrls += request.url
        requestBodies += request.body
        requestHeaders += request.headers
        return if (path == "connect") {
            SdkHttpResponse(
                code = 200,
                body = """{"ts":"2026-07-27T00:00:00Z","user":{"token":"$userToken"},"session":{"id":1},"behaviour":{"widget":false,"tracker":$trackerEnabled,"gpc":$gpc,"consent_form":$consentForm}}""",
            )
        } else {
            val responseIndex = eventResponseIndex.getAndIncrement()
            val responseCode = eventResponseCodes
                ?.getOrElse(responseIndex) { eventResponseCodes.last() }
                ?: eventResponseCode
            SdkHttpResponse(code = responseCode, body = eventResponseBody)
        }
    }

    override suspend fun postRaw(request: SdkRawHttpRequest) = SdkHttpResponse(200)

    override suspend fun getJson(request: SdkHttpRequest): SdkHttpResponse {
        getRequestUrls += request.url
        return SdkHttpResponse(
            code = 200,
            body = """{"ts":"2026-07-27T00:00:00Z"}""",
        )
    }

    override fun close() = Unit
}
