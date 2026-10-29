package com.simla.android.sdk.notifications

import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.error.SdkHttpException
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkRawHttpRequest
import com.simla.android.sdk.notifications.registration.DefaultPushNotificationsClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DefaultPushNotificationsClientTest {
    @Test
    fun `registers SDK customer external ID with registered fid`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(network, fid = " installation-id ")

        client.registerDevice(
            categories = null,
        )

        val request = requireNotNull(network.lastRequest)
        val body = Json.parseToJsonElement(request.body).jsonObject
        assertEquals("https://example.test/api/client/v1/push-tokens", request.url)
        assertEquals("site-token", request.headers["X-Site-Token"])
        assertEquals("42", body.getValue("customerExternalId").jsonPrimitive.content)
        assertEquals("installation-id", body.getValue("fid").jsonPrimitive.content)
        assertFalse("token" in body)
        assertEquals("firebase-project", body.getValue("type").jsonPrimitive.content)
        assertEquals("android", body.getValue("deviceType").jsonPrimitive.content)
        assertEquals(
            emptyList<String>(),
            body.getValue("categories").jsonArray.map { it.jsonPrimitive.content },
        )
        assertFalse("customerId" in body)
    }

    @Test
    fun `uses registered fid with external customer`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(
            network,
            fid = "installation-id",
            customerIdentityProvider = { CrmCustomerIdentity(" external-42 ", " main ") })

        client.registerDevice()

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertEquals("external-42", body.getValue("customerExternalId").jsonPrimitive.content)
        assertEquals("main", body.getValue("crmSiteCode").jsonPrimitive.content)
        assertEquals("installation-id", body.getValue("fid").jsonPrimitive.content)
        assertFalse("token" in body)
        assertEquals(
            emptyList<String>(),
            body.getValue("categories").jsonArray.map { it.jsonPrimitive.content },
        )
        assertFalse("customerId" in body)
    }

    @Test
    fun `registers CRM customer ID without external identity`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(
            network,
            fid = "installation-id",
            customerIdentityProvider = { CrmCustomerIdentity(37) },
        )

        client.registerDevice()

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertEquals(37L, body.getValue("customerId").jsonPrimitive.content.toLong())
        assertFalse("customerExternalId" in body)
        assertFalse("crmSiteCode" in body)
    }

    @Test
    fun `registers both supported customer identities`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(
            network,
            fid = "installation-id",
            customerIdentityProvider = {
                CrmCustomerIdentity(37, " external-37 ", " main ")
            },
        )

        client.registerDevice()

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertEquals(37L, body.getValue("customerId").jsonPrimitive.content.toLong())
        assertEquals("external-37", body.getValue("customerExternalId").jsonPrimitive.content)
        assertEquals("main", body.getValue("crmSiteCode").jsonPrimitive.content)
    }

    @Test
    fun `registers requested push categories`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(network, fid = "installation-id")

        client.registerDevice(
            categories = setOf(" news ", "system", "news"),
        )

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertEquals(
            listOf("news", "system"),
            body.getValue("categories").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun `allows empty requested push categories`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(network, fid = "installation-id")

        client.registerDevice(
            categories = emptySet(),
        )

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertEquals(
            emptyList<String>(),
            body.getValue("categories").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun `clears previous subscriptions when categories are null or omitted`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(network, fid = "installation-id")

        for (omitCategories in listOf(false, true)) {
            client.registerDevice(categories = setOf("system", "news"))
            val subscribedBody = Json.parseToJsonElement(requireNotNull(network.lastRequest).body)
                .jsonObject
            assertEquals(
                listOf("news", "system"),
                subscribedBody.getValue("categories").jsonArray.map { it.jsonPrimitive.content },
            )

            if (omitCategories) client.registerDevice() else client.registerDevice(categories = null)

            val clearedBody = Json.parseToJsonElement(requireNotNull(network.lastRequest).body)
                .jsonObject
            assertEquals(emptyList<String>(), clearedBody.getValue("categories").jsonArray)
            assertEquals("installation-id", clearedBody.getValue("fid").jsonPrimitive.content)
        }
    }

    @Test
    fun `registers token when registered fid is unavailable`() = runBlocking {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(
            networkEngine = network,
            fid = null,
        )

        val registration = client.registerDevice(
            categories = null,
        )

        val body = Json.parseToJsonElement(requireNotNull(network.lastRequest).body).jsonObject
        assertFalse("fid" in body)
        assertEquals("fcm-registration-token", body.getValue("token").jsonPrimitive.content)
        assertEquals(emptyList<String>(), body.getValue("categories").jsonArray)
        assertEquals(null, registration.fid)
        assertEquals("fcm-registration-token", registration.token)
    }

    @Test
    fun `fails when neither registered fid nor token is available`() {
        val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
        val client = createClient(
            networkEngine = network,
            fid = null,
            registrationToken = null,
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                client.registerDevice(
                    categories = null,
                )
            }
        }

        assertEquals(
            "Either a registered Firebase installation ID or FCM registration token is required",
            exception.message,
        )
        assertEquals(null, network.lastRequest)
    }

    @Test
    fun `throws HTTP exception when backend does not return 204`() {
        val network = FakeNetworkEngine(
            SdkHttpResponse(code = 400, body = "{\"errors\":[\"invalid\"]}"),
        )
        val client = createClient(network, fid = "installation-id")

        val exception = assertThrows(SdkHttpException::class.java) {
            runBlocking {
                client.registerDevice(categories = null)
            }
        }

        assertEquals(400, exception.httpCode)
        assertEquals("{\"errors\":[\"invalid\"]}", exception.responseBody)
    }

    @Test
    fun `uses current SDK identity on each registration and rejects anonymous customer`() =
        runBlocking {
            val network = FakeNetworkEngine(SdkHttpResponse(code = 204))
            var siteCustomerId: String? = "customer-1"
            var siteCode = "main"
            val client = createClient(
                network,
                fid = "installation-id",
                customerIdentityProvider = {
                    siteCustomerId?.let {
                        CrmCustomerIdentity(
                            it,
                            siteCode
                        )
                    }
                })

            client.registerDevice()
            assertEquals(
                "customer-1", Json.parseToJsonElement(requireNotNull(network.lastRequest).body)
                    .jsonObject.getValue("customerExternalId").jsonPrimitive.content
            )

            siteCustomerId = "customer-2"
            siteCode = "other"
            client.registerDevice()
            assertEquals(
                "customer-2", Json.parseToJsonElement(requireNotNull(network.lastRequest).body)
                    .jsonObject.getValue("customerExternalId").jsonPrimitive.content
            )

            assertEquals(
                "other", Json.parseToJsonElement(requireNotNull(network.lastRequest).body)
                    .jsonObject.getValue("crmSiteCode").jsonPrimitive.content
            )

            val lastRequest = network.lastRequest
            siteCustomerId = null
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { client.registerDevice() }
            }
            assertEquals(lastRequest, network.lastRequest)
        }

    private fun createClient(
        networkEngine: SdkHttpNetworkEngine,
        fid: String?,
        registrationToken: String? = " fcm-registration-token ",
        customerIdentityProvider: () -> CrmCustomerIdentity? = {
            CrmCustomerIdentity(
                "42",
                "main"
            )
        },
    ): DefaultPushNotificationsClient = DefaultPushNotificationsClient(
        endpoint = "https://example.test/api/client/v1/push-tokens",
        siteToken = "site-token",
        networkEngine = networkEngine,
        installationIdProvider = { fid },
        registrationTokenProvider = { registrationToken },
        firebaseProjectNameProvider = { " firebase-project " },
        customerIdentityProvider = customerIdentityProvider,
    )

    private class FakeNetworkEngine(
        private val response: SdkHttpResponse,
    ) : SdkHttpNetworkEngine {
        var lastRequest: SdkHttpRequest? = null

        override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse {
            lastRequest = request
            return response
        }

        override suspend fun postRaw(request: SdkRawHttpRequest): SdkHttpResponse =
            error("Unexpected postRaw call")

        override suspend fun getJson(request: SdkHttpRequest): SdkHttpResponse =
            error("Unexpected getJson call")

        override fun close() = Unit
    }
}
