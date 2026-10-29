package com.simla.android.sdk.consultant

import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkOkHttpNetworkEngine
import com.simla.android.sdk.network.SdkWebSocketSession
import com.simla.android.sdk.network.SdkWebSocketSessionListener
import com.simla.android.sdk.session.SdkSession
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConsultantWebSocketAuthenticationTest {
    @Test
    fun `websocket authentication is sent in headers instead of query parameters`() {
        val server = MockWebServer()
        val client = OkHttpClient()
        val engine = SdkOkHttpNetworkEngine.from(client)
        val opened = CountDownLatch(1)
        val serverClosed = CountDownLatch(1)
        var webSocket: SdkWebSocketSession? = null

        try {
            server.enqueue(
                MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) = Unit

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        serverClosed.countDown()
                    }
                }),
            )
            val request = buildSdkWebSocketRequest(
                url = server.url("/api/client/v1/ws").toString(),
                session = SdkSession(
                    userToken = "user-token",
                    sessionId = "42",
                    siteToken = "site-token",
                    platformName = "android",
                ),
                updatedAt = "2026-09-07T10:15:30Z",
            )

            webSocket = engine.openWebSocket(request, listener(opened))

            assertTrue("WebSocket handshake timed out", opened.await(5, TimeUnit.SECONDS))
            val recordedRequest = server.takeRequest(5, TimeUnit.SECONDS)
                ?: error("WebSocket handshake request was not recorded")
            val requestUrl = recordedRequest.requestUrl
                ?: error("WebSocket handshake URL is missing")

            assertEquals("user-token", recordedRequest.getHeader("X-User-Token"))
            assertEquals("42", recordedRequest.getHeader("X-Session-Id"))
            assertNull(requestUrl.queryParameter("token"))
            assertNull(requestUrl.queryParameter("session"))
            assertEquals("2026-09-07T10:15:30Z", requestUrl.queryParameter("updated_at"))
        } finally {
            webSocket?.close(1000, "test complete")
            serverClosed.await(5, TimeUnit.SECONDS)
            server.shutdown()
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    @Test
    fun `websocket URL contains no query when updated marker is absent`() {
        val request = buildSdkWebSocketRequest(
            url = "https://consultant.example.com/api/client/v1/ws",
            session = SdkSession(
                userToken = "user-token",
                sessionId = "42",
                siteToken = "site-token",
                platformName = "android",
            ),
            updatedAt = null,
        )

        assertNull(request.url.toHttpUrl().query)
    }

    private fun listener(opened: CountDownLatch) = object : SdkWebSocketSessionListener {
        override fun onOpen(response: SdkHttpResponse) {
            opened.countDown()
        }

        override fun onTextMessage(text: String) = Unit

        override fun onBinaryMessage(payload: ByteArray) = Unit

        override fun onClosing(code: Int, reason: String) = Unit

        override fun onClosed(code: Int, reason: String) = Unit

        override fun onFailure(throwable: Throwable, response: SdkHttpResponse?) = Unit
    }
}
