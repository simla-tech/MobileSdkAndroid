package com.simla.android.sdk.network

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class SdkOkHttpNetworkEngine private constructor(
    internal val httpClient: OkHttpClient,
    private val ownsClientResources: Boolean
) : SdkNetworkEngine {

    /** Timeout settings for an engine-owned OkHttp client. Zero disables the corresponding timeout. */
    data class Options(
        val connectTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        val readTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        val writeTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ) {
        init {
            require(connectTimeoutSeconds >= 0) { "connectTimeoutSeconds must not be negative" }
            require(readTimeoutSeconds >= 0) { "readTimeoutSeconds must not be negative" }
            require(writeTimeoutSeconds >= 0) { "writeTimeoutSeconds must not be negative" }
        }
    }

    override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse =
        withContext(Dispatchers.IO) {
            val httpRequest = Request.Builder()
                .url(request.url)
                .post(request.body.toRequestBody(JSON_MEDIA_TYPE))
                .apply {
                    request.headers.forEach { (key, value) -> addHeader(key, value) }
                }
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                response.toSdkHttpResponse()
            }
        }

    override suspend fun postRaw(request: SdkRawHttpRequest): SdkHttpResponse =
        withContext(Dispatchers.IO) {
            val httpRequest = Request.Builder()
                .url(request.url)
                .post(request.body.toRequestBody(request.contentType.toMediaType()))
                .apply {
                    request.headers.forEach { (key, value) -> addHeader(key, value) }
                }
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                response.toSdkHttpResponse()
            }
        }

    override suspend fun getJson(request: SdkHttpRequest): SdkHttpResponse =
        withContext(Dispatchers.IO) {
            val httpRequest = Request.Builder()
                .url(request.url)
                .get()
                .apply {
                    request.headers.forEach { (key, value) -> addHeader(key, value) }
                }
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                response.toSdkHttpResponse()
            }
        }

    override fun openWebSocket(
        request: SdkWebSocketRequest,
        listener: SdkWebSocketSessionListener,
    ): SdkWebSocketSession {
        val adapter = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onOpen(response.toSdkHttpResponse(body = ""))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener.onBinaryMessage(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosing(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onFailure(
                    throwable = t,
                    response = response?.toSdkHttpResponse(),
                )
            }
        }

        val webSocket = httpClient.newWebSocket(
            Request.Builder()
                .url(request.url)
                .apply {
                    request.headers.forEach { (key, value) -> addHeader(key, value) }
                }
                .build(),
            adapter
        )

        return object : SdkWebSocketSession {
            override fun close(code: Int, reason: String): Boolean = webSocket.close(code, reason)
        }
    }

    override fun close() {
        if (!ownsClientResources) {
            return
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Thread(::closeOwnedClientResources, "sdk-okhttp-close").start()
            return
        }

        closeOwnedClientResources()
    }

    private fun closeOwnedClientResources() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val DEFAULT_TIMEOUT_SECONDS = 30L

        /** Creates an engine with its own OkHttp client and resources. */
        fun create(options: Options = Options()): SdkOkHttpNetworkEngine {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(options.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(options.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(options.writeTimeoutSeconds, TimeUnit.SECONDS)
                .pingInterval(0, TimeUnit.SECONDS)
                .build()

            return SdkOkHttpNetworkEngine(
                httpClient = httpClient,
                ownsClientResources = true,
            )
        }

        /** Uses the supplied client without changing its settings or taking ownership of its resources. */
        fun from(okHttpClient: OkHttpClient): SdkOkHttpNetworkEngine =
            SdkOkHttpNetworkEngine(
                httpClient = okHttpClient,
                ownsClientResources = false,
            )
    }
}
