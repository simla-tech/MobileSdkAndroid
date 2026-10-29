package com.simla.android.sdk.runtime

import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.CrmSdkConfig
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.network.SdkRawHttpRequest
import com.simla.android.sdk.network.SdkWebSocketRequest
import com.simla.android.sdk.network.SdkWebSocketSession
import com.simla.android.sdk.network.SdkWebSocketSessionListener
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.storage.SdkSessionStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CrmRuntimeTest {
    @Test
    fun repeated_await_reuses_active_session() = runBlocking {
        val network = RecordingNetworkEngine()
        val runtime = createRuntime(network)

        runtime.initialize(SessionPageContext(url = "app://first"))
        runtime.awaitConnected()
        runtime.awaitConnected()

        assertEquals(1, network.postRequests.size)
        runtime.close()
    }

    @Test
    fun changing_customer_reconnects_started_runtime_with_new_identity() = runBlocking {
        val network = RecordingNetworkEngine()
        val storage = MemorySessionStorage()
        val runtime = createRuntime(network, storage)

        runtime.initialize()
        runtime.awaitConnected()
        runtime.setCustomerIdentity(CrmCustomerIdentity("customer-1", "main"))
        runtime.setCustomerIdentity(CrmCustomerIdentity("customer-2", "main"))

        assertEquals(3, network.postRequests.size)
        assertEquals(null, network.postRequests[0].headers["X-Site-Customer-Id"])
        assertEquals("customer-1", network.postRequests[1].headers["X-Site-Customer-Id"])
        assertEquals("customer-2", network.postRequests[2].headers["X-Site-Customer-Id"])
        assertEquals(listOf("customer-1", "customer-2"), storage.activatedScopes)
        runtime.close()
    }

    @Test
    fun listener_installed_after_start_receives_active_session() = runBlocking {
        val runtime = createRuntime(RecordingNetworkEngine())
        runtime.initialize(
            pageContext = SessionPageContext(url = "app://home"),
            user = SdkUserPayload(name = "Customer"),
        )
        runtime.awaitConnected()
        val connected = CompletableDeferred<Pair<String?, String?>>()

        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onConnected(
                response: CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) {
                connected.complete(pageContext.url to user.name)
            }
        })

        assertEquals("app://home" to "Customer", withTimeout(1_000) { connected.await() })
        runtime.close()
    }

    @Test
    fun failing_listener_does_not_prevent_other_features_from_connecting() = runBlocking {
        val runtime = createRuntime(RecordingNetworkEngine())
        val connected = CompletableDeferred<Unit>()
        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onConnected(
                response: CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) = error("Feature failed")
        })
        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onConnected(
                response: CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) {
                connected.complete(Unit)
            }
        })

        runtime.initialize()
        runtime.awaitConnected()

        withTimeout(1_000) { connected.await() }
        runtime.close()
    }

    @Test
    fun failing_listener_does_not_prevent_other_features_from_handling_foreground() = runBlocking {
        val runtime = createRuntime(RecordingNetworkEngine())
        val foregroundHandled = CompletableDeferred<Unit>()
        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onForeground() = error("Feature failed")
        })
        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onForeground() {
                foregroundHandled.complete(Unit)
            }
        })
        runtime.initialize()
        runtime.awaitConnected()

        runtime.setAppInForeground(true)

        withTimeout(1_000) { foregroundHandled.await() }
        runtime.close()
    }

    @Test
    fun listener_can_build_authorized_headers_during_customer_reconnect() = runBlocking {
        val runtime = createRuntime(RecordingNetworkEngine())
        runtime.addListener(object : CrmRuntimeListener {
            override suspend fun onConnected(
                response: CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) {
                runtime.buildAuthorizedHttpHeaders()
            }
        })
        runtime.initialize()
        runtime.awaitConnected()

        withTimeout(1_000) {
            runtime.setCustomerIdentity(CrmCustomerIdentity(37))
        }

        val headers = runtime.buildAuthorizedHttpHeaders()
        assertEquals("user-2", headers["X-User-Token"])
        assertEquals("2", headers["X-Session-Id"])
        runtime.close()
    }

    @Test
    fun crm_customer_identity_reconnects_without_site_customer_header() = runBlocking {
        val network = RecordingNetworkEngine()
        val storage = MemorySessionStorage()
        val runtime = createRuntime(network, storage)
        runtime.initialize()
        runtime.awaitConnected()

        runtime.setCustomerIdentity(CrmCustomerIdentity(37))

        assertEquals(null, network.postRequests.last().headers["X-Site-Customer-Id"])
        assertEquals("crm:37", storage.activatedScopes.last())
        assertEquals(37L, runtime.currentCustomerIdentity()?.crmCustomerId)
        runtime.close()
    }

    @Test
    fun customer_identity_validates_supported_identifier_combinations() = runBlocking {
        val runtime = createRuntime(RecordingNetworkEngine())
        for (site in listOf("", " ", "x".repeat(256))) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { runtime.setCustomerIdentity(CrmCustomerIdentity("customer-1", site)) }
            }
        }
        for (id in listOf("", " ", "x".repeat(256))) {
            assertThrows(IllegalArgumentException::class.java) {
                CrmCustomerIdentity(id, "main")
            }
        }
        assertThrows(IllegalArgumentException::class.java) { CrmCustomerIdentity(0) }
        assertThrows(IllegalArgumentException::class.java) {
            CrmCustomerIdentity(null, "customer-1", null)
        }
        val combined = CrmCustomerIdentity(
            crmCustomerId = 37,
            siteCustomerId = " customer-1 ",
            siteCode = " main ",
        )
        runtime.setCustomerIdentity(combined)
        assertEquals("main", runtime.currentCustomerIdentity()?.siteCode)
        assertEquals("customer-1", runtime.currentCustomerIdentity()?.siteCustomerId)
        assertEquals(37L, runtime.currentCustomerIdentity()?.crmCustomerId)
        runtime.setCustomerIdentity(CrmCustomerIdentity("customer-1", "other"))
        assertEquals("other", runtime.currentCustomerIdentity()?.siteCode)
        runtime.setCustomerIdentity(null)
        assertEquals(null, runtime.currentCustomerIdentity())
        assertEquals(null, runtime.currentSiteCustomerId())
        runtime.close()
    }

    private fun createRuntime(
        network: RecordingNetworkEngine,
        storage: MemorySessionStorage = MemorySessionStorage(),
    ) = CrmRuntime(
        config = CrmSdkConfig(siteToken = "site-token", baseUrl = "https://example.test"),
        networkEngine = network,
        logger = NoOpLogger,
        sessionStorage = storage,
    )
}

private class RecordingNetworkEngine : SdkNetworkEngine {
    val postRequests = mutableListOf<SdkHttpRequest>()

    override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse {
        postRequests += request
        val index = postRequests.size
        return SdkHttpResponse(
            code = 200,
            body = """{"user":{"token":"user-$index"},"session":{"id":$index},"behaviour":{"widget":true,"tracker":true}}""",
        )
    }

    override suspend fun postRaw(request: SdkRawHttpRequest) = error("Unexpected request")

    override suspend fun getJson(request: SdkHttpRequest) = error("Unexpected request")

    override fun openWebSocket(
        request: SdkWebSocketRequest,
        listener: SdkWebSocketSessionListener,
    ): SdkWebSocketSession = error("Unexpected websocket")

    override fun close() = Unit
}

private class MemorySessionStorage : SdkSessionStorage {
    val activatedScopes = mutableListOf<String?>()
    private var token: String? = null

    override fun activateScope(scope: String?) {
        activatedScopes += scope
        token = null
    }

    override fun getUserToken(): String? = token

    override fun setUserToken(value: String?) {
        token = value
    }

    override fun clear() {
        token = null
    }
}

private object NoOpLogger : SdkLogger {
    override fun d(message: String) = Unit

    override fun e(message: String, throwable: Throwable?) = Unit
}
