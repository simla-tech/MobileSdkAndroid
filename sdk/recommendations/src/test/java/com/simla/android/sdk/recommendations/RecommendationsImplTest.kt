package com.simla.android.sdk.recommendations

import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.network.SdkHttpResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsImplTest {
    @Test
    fun logs_resolved_request_and_full_raw_error_response() = runBlocking {
        val messages = mutableListOf<String>()
        val logger = object : com.simla.android.sdk.logging.SdkLogger {
            override fun d(message: String) {
                messages += message
            }

            override fun e(message: String, throwable: Throwable?) {
                messages += message
            }
        }
        val rawBody = """{"errors":["${"Ошибка".repeat(400)}"]}"""
        val network =
            RecordingRecommendationNetworkEngine(SdkHttpResponse(code = 400, body = rawBody))
        val client = createClient(network, logger = logger)
        val failure = runCatching {
            client.getProducts(
                CrmRecommendationRequest(mechanicCode = "mobile-home", blockCode = " home "),
            )
        }.exceptionOrNull()
        assertTrue(failure is CrmRecommendationsHttpException)
        val requestParts =
            messages.filter { it.startsWith("Recommendations request") && it.contains("]: ") }
        assertEquals(
            network.request.body,
            requestParts.joinToString("") { it.substringAfter("]: ") })
        val responseParts = messages.filter { it.startsWith("Recommendations response HTTP 400 [") }
        assertTrue(responseParts.size > 1)
        assertEquals(rawBody, responseParts.joinToString("") { it.substringAfter("]: ") })
        assertFalse(messages.any { it.contains("user-token") || it.contains("session-id") })
    }

    @Test
    fun get_products_uses_backend_contract_and_tracker_customer_identity() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(network)

        val result = client.getProducts(
            CrmRecommendationRequest(
                mechanicCode = "mobile-home",
                blockCode = "home",
                productExternalId = "product-101",
            ),
        )

        assertEquals(
            "https://consultant.example.com/api/client/v1/recommendations/products",
            network.request.url,
        )
        assertEquals("user-token", network.request.headers["X-User-Token"])
        assertEquals("session-id", network.request.headers["X-Session-Id"])
        val body = SdkJson.parseToJsonElement(network.request.body).jsonObject
        assertEquals("mobile-home", body.getValue("mechanic_code").jsonPrimitive.content)
        assertFalse(body.containsKey("mechanic_id"))
        assertEquals("home", body.getValue("block_code").jsonPrimitive.content)
        assertEquals(
            "customer-42",
            body.getValue("system_customer_external_id").jsonPrimitive.content,
        )
        assertEquals("product-101", body.getValue("product_external_id").jsonPrimitive.content)
        assertFalse(body.containsKey("system_customer_id"))
        assertFalse(body.containsKey("strategy"))
        assertFalse(body.containsKey("limit"))
        assertEquals(701L, result.executionId)
        assertEquals("similar", result.context.mechanicType)
        assertEquals(101L, result.productIds.single())
        assertEquals(1001L, result.products.single().id)
    }

    @Test
    fun mechanic_code_is_sent_without_normalization() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(network)

        client.getProducts(
            CrmRecommendationRequest(mechanicCode = " Mobile ", blockCode = "home"),
        )

        val body = SdkJson.parseToJsonElement(network.request.body).jsonObject
        assertEquals(" Mobile ", body.getValue("mechanic_code").jsonPrimitive.content)
    }

    @Test
    fun customer_fields_are_omitted_when_sdk_identity_is_absent() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(network, siteCustomerId = null)

        client.getProducts(
            CrmRecommendationRequest(
                mechanicCode = "mobile-home",
                blockCode = "home",
            ),
        )

        val body = SdkJson.parseToJsonElement(network.request.body).jsonObject
        assertFalse(body.containsKey("system_customer_external_id"))
        assertFalse(body.containsKey("system_customer_id"))
    }

    @Test
    fun site_customer_identity_is_used_when_crm_id_is_absent() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(
            network = network,
            siteCustomerId = "customer-42",
        )

        client.getProducts(
            CrmRecommendationRequest(mechanicCode = "mobile-home", blockCode = "home"),
        )

        val body = SdkJson.parseToJsonElement(network.request.body).jsonObject
        assertEquals(
            "customer-42",
            body.getValue("system_customer_external_id").jsonPrimitive.content,
        )
        assertFalse(body.containsKey("system_customer_id"))
    }

    @Test
    fun crm_customer_identity_is_used_before_external_identity() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = RecommendationsImpl(
            networkEngine = network,
            authorizedHeadersProvider = { emptyMap() },
            customerIdentityProvider = {
                CrmCustomerIdentity(37, "external-37", "main")
            },
            endpointResolver = { path -> "https://consultant.example.com/api/client/v1/$path" },
            pageViewTracker = { _, _, _ -> },
            eventTracker = { _, _ -> },
        )

        client.getProducts(
            CrmRecommendationRequest(mechanicCode = "mobile-home", blockCode = "home"),
        )

        val body = SdkJson.parseToJsonElement(network.request.body).jsonObject
        assertEquals(37L, body.getValue("system_customer_id").jsonPrimitive.content.toLong())
        assertFalse(body.containsKey("system_customer_external_id"))
    }

    @Test
    fun backend_errors_are_exposed_as_typed_exception() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(
            SdkHttpResponse(
                code = 400,
                body = """{"errors":["Invalid product recommendation mechanic code."]}""",
            ),
        )
        val client = createClient(network)

        val error = runCatching {
            client.getProducts(
                CrmRecommendationRequest(mechanicCode = "Mobile", blockCode = "home"),
            )
        }.exceptionOrNull() as CrmRecommendationsHttpException

        assertEquals(400, error.httpCode)
        assertEquals(listOf("Invalid product recommendation mechanic code."), error.errors)
    }

    @Test
    fun invalid_request_is_rejected_before_network_call() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(network)

        val error = runCatching {
            client.getProducts(CrmRecommendationRequest(mechanicCode = "", blockCode = "home"))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Recommendation mechanic code must not be empty", error?.message)
        assertFalse(runCatching { network.request }.isSuccess)
    }

    @Test
    fun product_and_offer_context_are_mutually_exclusive() = runBlocking {
        val network = RecordingRecommendationNetworkEngine(successResponse())
        val client = createClient(network)

        val error = runCatching {
            client.getProducts(
                CrmRecommendationRequest(
                    mechanicCode = "mobile-home",
                    blockCode = "home",
                    productId = 101,
                    offerId = 1001,
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(
            "Recommendation requires either product context or offer context",
            error?.message,
        )
        assertFalse(runCatching { network.request }.isSuccess)
    }

    @Test
    fun impression_and_click_use_recommendation_execution_id() = runBlocking {
        val pageViews = mutableListOf<TrackedPageView>()
        val events = mutableListOf<Pair<String, kotlinx.serialization.json.JsonObject>>()
        val client = createClient(
            network = RecordingRecommendationNetworkEngine(successResponse()),
            pageViews = pageViews,
            events = events,
        )
        val recommendation = recommendation()

        client.trackImpression(recommendation, screenUri = "app://home", title = "Home")
        client.trackClick(
            recommendation = recommendation,
            productId = 102,
            position = 2,
            element = "recommendations#home",
            leaving = true,
        )

        val impression = pageViews.single()
        assertEquals("app://home", impression.screenUri)
        assertEquals("Home", impression.title)
        assertEquals(
            701L,
            impression.data.getValue("recommendation_execution_id").jsonPrimitive.content.toLong(),
        )
        assertEquals(setOf("recommendation_execution_id"), impression.data.keys)

        val click = events.single()
        assertEquals("click", click.first)
        val clickData = click.second
        assertEquals(
            701L,
            clickData.getValue("recommendation_execution_id").jsonPrimitive.content.toLong(),
        )
        assertEquals(102L, clickData.getValue("product_id").jsonPrimitive.content.toLong())
        assertEquals(2, clickData.getValue("recommendation_position").jsonPrimitive.content.toInt())
        assertTrue(clickData.getValue("leaving").jsonPrimitive.content.toBoolean())
        assertFalse(clickData.containsKey("recommendation_uuid"))
    }

    private fun createClient(
        network: RecordingRecommendationNetworkEngine,
        siteCustomerId: String? = "customer-42",
        pageViews: MutableList<TrackedPageView> = mutableListOf(),
        events: MutableList<Pair<String, kotlinx.serialization.json.JsonObject>> = mutableListOf(),
        logger: com.simla.android.sdk.logging.SdkLogger? = null,
    ) = RecommendationsImpl(
        networkEngine = network,
        logger = logger,
        authorizedHeadersProvider = {
            mapOf(
                "X-User-Token" to "user-token",
                "X-Session-Id" to "session-id",
            )
        },
        customerIdentityProvider = {
            siteCustomerId?.let { CrmCustomerIdentity(it, "main") }
        },
        endpointResolver = { path -> "https://consultant.example.com/api/client/v1/$path" },
        pageViewTracker = { screenUri, title, data ->
            pageViews += TrackedPageView(screenUri, title, data)
        },
        eventTracker = { type, data -> events += type to data },
    )

    private fun recommendation() = CrmRecommendationResult(
        executionId = 701,
        personalized = false,
        context = CrmRecommendationContext(
            mechanicType = "similar",
            required = listOf("product"),
        ),
        productIds = listOf(101, 102),
    )

    private fun successResponse() = SdkHttpResponse(
        code = 200,
        body = """{
            "executionId":701,
            "personalized":false,
            "context":{"mechanicType":"similar","required":["product"]},
            "productIds":[101],
            "products":[{
                "id":1001,
                "productId":101,
                "name":"Offer",
                "price":1500,
                "oldPrice":0,
                "currency":"RUB",
                "imageUrl":"https://example.com/image.jpg",
                "url":"https://example.com/product/101",
                "available":true
            }]
        }""".trimIndent(),
    )
}
