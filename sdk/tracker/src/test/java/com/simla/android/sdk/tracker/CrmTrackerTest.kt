package com.simla.android.sdk.tracker

import com.simla.android.sdk.CrmUtmAttribution
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.tracker.model.TrackerConnectionResult
import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.serialization.SdkJson
import com.simla.android.sdk.tracker.storage.TrackerStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrmTrackerTest {
    @Test
    fun track_order_automatically_captures_current_utm_attribution() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val storage = RecordingIncrementalTrackerStorage()
        val tracker = CrmTracker.createManaged(
            config = object : TrackerRuntimeConfig {
                override val trackerBatchSize = 50
                override val trackerFlushIntervalMs = 15_000L
                override val trackerTimeSyncIntervalMs = 60 * 60 * 1_000L
                override val trackerMaxQueueSize = 1_000
            },
            logger = NoOpLogger,
            networkEngine = network,
            trackerStorage = storage,
            scope = scope,
            sessionProvider = {
                SdkSession("user", "session", "site-token", "android")
            },
            authorizedHeadersProvider = { emptyMap() },
            endpointResolver = { path -> "https://consultant.example.com/api/client/v1/$path" },
            utmAttributionProvider = {
                CrmUtmAttribution(
                    source = "push",
                    medium = "mobile",
                    campaign = "october",
                    term = "jackets",
                    content = "banner",
                )
            },
        )

        try {
            tracker.onHostConnected(serverTs = null, trackerEnabled = true)
            tracker.trackOrder(TrackerOrderIdentifier.ExternalId("order-42"))

            assertEquals(null, storage.upsertedEvents.single().single().utm)

            tracker.flush()

            val event = SdkJson.parseToJsonElement(network.requestBodies.single())
                .jsonArray.single().jsonObject
            val utm = event.getValue("utm").jsonObject
            assertEquals("push", utm.getValue("utm_source").jsonPrimitive.content)
            assertEquals("mobile", utm.getValue("utm_medium").jsonPrimitive.content)
            assertEquals("october", utm.getValue("utm_campaign").jsonPrimitive.content)
            assertEquals("jackets", utm.getValue("utm_keyword").jsonPrimitive.content)
            assertEquals("banner", utm.getValue("utm_content").jsonPrimitive.content)
            assertTrue("utm_term" !in utm)
        } finally {
            tracker.close()
            scope.cancel()
        }
    }

    @Test
    fun typed_api_builds_every_supported_event_type() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)
        val offer = TrackerOfferIdentifier.ExternalId("offer-42")
        val item = TrackerCartItem(offer, price = 100.0, quantity = 2)
        val subscription = TrackerSubscription(
            TrackerSubscriptionChannel.EMAIL,
            "customer@example.com",
            active = true,
        )

        tracker.connect(SessionPageContext(url = "app://home"))
        tracker.trackPageView(
            screenUri = "app://product/42",
            product = TrackerViewedProduct(offer = offer, productId = 42, brand = "Simla"),
        )
        tracker.trackClick("product_card", leaving = true)
        tracker.trackRegister(TrackerCustomer(email = "customer@example.com"))
        tracker.trackLogin("customer@example.com", listOf(subscription))
        tracker.trackConsult("customer@example.com", listOf(subscription))
        tracker.trackOpenCart("customer@example.com")
        tracker.trackContact(TrackerContactChannel.WHATSAPP)
        tracker.trackCart("cart-42", listOf(item))
        tracker.trackOrder(
            identifier = TrackerOrderIdentifier.SystemId(42),
            cartId = "cart-42",
            items = listOf(item),
            deliveryPrice = 10.0,
            total = 210.0,
        )
        tracker.trackSubscription(subscription)
        tracker.trackFormStart("checkout")
        tracker.trackFormSubmit("checkout")
        tracker.trackFileDownload("invoice.pdf", "invoice_link")
        tracker.trackFavoriteProduct(offer, removed = true)
        tracker.flush()

        val events = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        )
        assertEquals(
            listOf(
                "page_view",
                "click",
                "register",
                "login",
                "consult",
                "open_cart",
                "contact",
                "cart",
                "order",
                "subscription",
                "form_start",
                "form_submit",
                "file_download",
                "favorite_product",
            ),
            events.map { it.type },
        )
        assertEquals("whatsapp", events[6].data?.get("contact_channel")?.jsonPrimitive?.content)
        assertEquals("cart-42", events[7].data?.get("cart_id")?.jsonPrimitive?.content)
        assertEquals("42", events[8].data?.get("order_id")?.jsonPrimitive?.content)
        assertEquals("email", events[9].data?.get("channel")?.jsonPrimitive?.content)
        assertEquals(true, events[13].data?.get("removed")?.jsonPrimitive?.content?.toBoolean())
        tracker.close()
    }

    @Test
    fun order_system_id_accepts_only_positive_signed_32_bit_values() {
        assertEquals(1L, TrackerOrderIdentifier.SystemId(1).value)
        assertEquals(
            Int.MAX_VALUE.toLong(),
            TrackerOrderIdentifier.SystemId(Int.MAX_VALUE.toLong()).value,
        )
        assertEquals(
            "Order system ID must fit a positive signed 32-bit integer",
            runCatching { TrackerOrderIdentifier.SystemId(0) }.exceptionOrNull()?.message,
        )
        assertEquals(
            "Order system ID must fit a positive signed 32-bit integer",
            runCatching { TrackerOrderIdentifier.SystemId(Int.MAX_VALUE.toLong() + 1) }
                .exceptionOrNull()?.message,
        )
    }

    @Test
    fun outgoing_event_adapter_adds_android_platform_to_every_event() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.connect()
        listOf(
            TrackerEvent(
                type = "page_view",
                code = "catalog_opened",
                pageUrl = "app://catalog",
            ),
            TrackerEvent(type = "click"),
        ).forEach { event ->
            tracker.enqueueEvent(event)
        }
        tracker.flush()

        val sentEvents = SdkJson.parseToJsonElement(network.requestBodies.last()).jsonArray
        assertEquals(
            listOf("android", "android"),
            sentEvents.map { event ->
                event.jsonObject.getValue("platform").jsonPrimitive.content
            },
        )
        assertEquals(
            "catalog_opened",
            sentEvents.first().jsonObject.getValue("code").jsonPrimitive.content,
        )
        assertTrue("code" !in sentEvents.last().jsonObject)
        assertTrue(sentEvents.none { "_id" in it.jsonObject })
        tracker.close()
    }

    @Test
    fun screen_uri_is_applied_to_page_views_and_following_events() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network)

        tracker.connect(SessionPageContext(title = "Home", url = "app://home"))
        tracker.trackPageView(
            screenUri = "app://catalog",
            title = "Catalog",
            code = "catalog_opened",
        )
        tracker.enqueueEvent(TrackerEvent(type = "click"))
        tracker.trackPageView(screenUri = "app://product/42", title = "Product 42")

        assertTrue(
            network.requestBodies[0].contains(
                "\"url\":\"app://home\"",
            ),
        )
        assertTrue(
            !network.requestBodies[0].contains("\"refer_url\""),
        )
        assertTrue(
            network.requestBodies[1].contains(
                "\"page_url\":\"app://catalog\"",
            ),
        )
        assertTrue(
            network.requestBodies[1].contains(
                "\"referrer_url\":\"app://home\"",
            ),
        )
        assertTrue(network.requestBodies[1].contains("\"page_title\":\"Catalog\""))
        assertTrue(network.requestBodies[1].contains("\"code\":\"catalog_opened\""))
        assertTrue(
            network.requestBodies[2].contains(
                "\"page_url\":\"app://catalog\"",
            ),
        )
        assertTrue(
            network.requestBodies[2].contains(
                "\"referrer_url\":\"app://home\"",
            ),
        )
        assertTrue(network.requestBodies[2].contains("\"page_title\":\"Catalog\""))
        assertTrue(
            network.requestBodies[3].contains(
                "\"page_url\":\"app://product/42\"",
            ),
        )
        assertTrue(
            network.requestBodies[3].contains(
                "\"referrer_url\":\"app://catalog\"",
            ),
        )
        tracker.close()
    }

    @Test
    fun page_view_requires_absolute_screen_uri() = runBlocking {
        val tracker = createTracker(FakeNetworkEngine(trackerEnabled = true))
        tracker.connect()

        val error = runCatching {
            tracker.trackPageView(screenUri = "/catalog")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("absolute URI"))
        tracker.close()
    }

    @Test
    fun tracker_disabled_by_server_does_not_send_events() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = false)
        val tracker = createTracker(network)

        val result = tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))
        tracker.flush()

        assertTrue(result is TrackerConnectionResult.DisabledByServer)
        assertEquals(listOf("connect"), network.requestPaths)
        tracker.close()
    }

    @Test
    fun tracker_enabled_by_server_sends_events_without_realtime_transport() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerTimeSyncIntervalMs = 0)

        val result = tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertTrue(result is TrackerConnectionResult.Available)
        assertEquals(listOf("connect", "event"), network.requestPaths)
        assertTrue(network.requestUrls.last().contains("user=user-token"))
        assertTrue(network.requestUrls.last().contains("session=1"))
        assertTrue(network.getRequestUrls.single().contains("user=user-token"))
        assertTrue(network.getRequestUrls.single().contains("session=1"))
        tracker.close()
    }

    @Test
    fun events_tracked_before_connect_are_sent_after_tracker_becomes_available() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.trackPageView(screenUri = "app://startup")

        assertTrue(network.requestPaths.isEmpty())
        assertEquals(1, tracker.state.value.pendingEventCount)

        tracker.connect()
        tracker.flush()

        assertEquals(listOf("connect", "event"), network.requestPaths)
        val event = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        ).single()
        assertEquals("page_view", event.type)
        assertEquals("app://startup", event.pageUrl)
        tracker.close()
    }

    @Test
    fun consent_form_buffers_events_until_explicit_grant() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val session = SdkSession("user-token", "1", "site-token", "android")
        val tracker = CrmTracker.createManaged(
            config = CrmTrackerConfig(
                siteToken = "site-token",
                baseUrl = "https://consultant.example.com",
                trackerBatchSize = 50,
            ),
            logger = NoOpLogger,
            networkEngine = network,
            trackerStorage = ScopedMemoryTrackerStorage(),
            scope = scope,
            sessionProvider = { session },
            authorizedHeadersProvider = { emptyMap() },
            endpointResolver = { "https://consultant.example.com/api/client/v1/$it" },
        )
        try {
            tracker.onHostConnected(
                serverTs = "2026-07-27T00:00:00Z",
                trackerEnabled = true,
                consentRequired = true,
            )
            tracker.enqueueEvent(TrackerEvent(type = "page_view"))
            tracker.flush()

            assertTrue(tracker.state.value.consentRequired)
            assertEquals(TrackerConsentState.UNKNOWN, tracker.state.value.consentState)
            assertEquals(1, tracker.state.value.pendingEventCount)
            assertTrue(network.requestPaths.isEmpty())

            tracker.setTrackingConsent(TrackerConsentState.GRANTED)
            tracker.flush()

            assertEquals(listOf("event"), network.requestPaths)
        } finally {
            tracker.close()
            scope.cancel()
        }
    }

    @Test
    fun consent_handler_result_enables_tracking() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, consentForm = true)
        val tracker = createTracker(network)
        val handler = RecordingConsentHandler(TrackerConsentState.GRANTED)
        tracker.setConsentHandler(handler)

        tracker.connect()
        withTimeout(1_000) {
            while (tracker.state.value.consentState != TrackerConsentState.GRANTED) delay(10)
        }
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(1, handler.getCalls)
        assertEquals(listOf("connect", "event"), network.requestPaths)
        tracker.close()
    }

    @Test
    fun consent_handler_uses_saved_decision_without_requesting_consent() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, consentForm = true)
        val tracker = createTracker(network)
        val handler = RecordingConsentHandler(TrackerConsentState.DENIED)
        tracker.setConsentHandler(handler)

        tracker.connect()
        withTimeout(1_000) {
            while (tracker.state.value.consentState != TrackerConsentState.DENIED) delay(10)
        }
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))
        tracker.flush()

        assertEquals(1, handler.getCalls)
        assertEquals(listOf("connect"), network.requestPaths)
        tracker.close()
    }

    @Test
    fun consent_handler_is_not_called_when_backend_does_not_require_consent() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, consentForm = false)
        val tracker = createTracker(network)
        val handler = RecordingConsentHandler(TrackerConsentState.DENIED)
        tracker.setConsentHandler(handler)

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(0, handler.getCalls)
        assertEquals(listOf("connect", "event"), network.requestPaths)
        tracker.close()
    }

    @Test
    fun revoked_consent_clears_queue_and_regrant_sends_only_new_events() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, consentForm = true)
        val tracker = createTracker(network, trackerBatchSize = 50)
        val queuedBeforeRevoke = "before-revoke"
        val attemptedWhileDenied = "while-denied"
        val queuedAfterRegrant = "after-regrant"

        tracker.setConsentHandler(RecordingConsentHandler(TrackerConsentState.GRANTED))
        tracker.connect()
        withTimeout(1_000) {
            while (tracker.state.value.consentState != TrackerConsentState.GRANTED) delay(10)
        }
        tracker.enqueueEvent(TrackerEvent(id = queuedBeforeRevoke, type = "click"))
        assertEquals(1, tracker.state.value.pendingEventCount)

        tracker.setTrackingConsent(TrackerConsentState.DENIED)
        assertEquals(0, tracker.state.value.pendingEventCount)
        tracker.enqueueEvent(TrackerEvent(id = attemptedWhileDenied, type = "click"))
        assertEquals(0, tracker.state.value.pendingEventCount)

        tracker.setTrackingConsent(TrackerConsentState.GRANTED)
        tracker.enqueueEvent(TrackerEvent(id = queuedAfterRegrant, type = "click"))
        tracker.flush()

        val flushedEvents = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        )
        assertEquals(listOf(queuedAfterRegrant), flushedEvents.map { it.id })
        tracker.close()
    }

    @Test
    fun gpc_setting_does_not_disable_native_tracker() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, gpc = true)
        val tracker = createTracker(network)

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(listOf("connect", "event"), network.requestPaths)
        tracker.close()
    }

    @Test
    fun managed_tracker_uses_host_session_without_second_connect() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val session = SdkSession("user-token", "1", "site-token", "android")
        val tracker = CrmTracker.createManaged(
            config = CrmTrackerConfig(
                siteToken = "site-token",
                baseUrl = "https://consultant.example.com",
                trackerBatchSize = 1,
            ),
            logger = NoOpLogger,
            networkEngine = network,
            trackerStorage = ScopedMemoryTrackerStorage(),
            scope = scope,
            sessionProvider = { session },
            authorizedHeadersProvider = {
                mapOf(
                    "X-Site-Token" to session.siteToken,
                    "X-User-Token" to session.userToken,
                    "X-Session-Id" to session.sessionId,
                )
            },
            endpointResolver = { "https://consultant.example.com/api/client/v1/$it" },
        )

        tracker.onHostConnected("2026-07-27T00:00:00Z", trackerEnabled = true)
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(listOf("event"), network.requestPaths)
        tracker.close()
        scope.cancel()
    }

    @Test
    fun managed_tracker_reads_customer_identity_from_core_provider() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val session = SdkSession("user-token", "1", "site-token", "android")
        var customerSystemId: Long? = 37
        var siteCustomerId: String? = "customer-1"
        val tracker = CrmTracker.createManaged(
            config = CrmTrackerConfig(
                siteToken = "site-token",
                baseUrl = "https://consultant.example.com",
                trackerBatchSize = 50,
            ),
            logger = NoOpLogger,
            networkEngine = network,
            trackerStorage = ScopedMemoryTrackerStorage(),
            scope = scope,
            sessionProvider = { session },
            authorizedHeadersProvider = {
                mapOf(
                    "X-Site-Token" to session.siteToken,
                    "X-User-Token" to session.userToken,
                    "X-Session-Id" to session.sessionId,
                )
            },
            endpointResolver = { "https://consultant.example.com/api/client/v1/$it" },
            customerSystemIdProvider = { customerSystemId },
            siteCustomerIdProvider = { siteCustomerId },
        )

        tracker.onHostConnected("2026-07-27T00:00:00Z", trackerEnabled = true)
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))
        tracker.flush()
        customerSystemId = 38
        siteCustomerId = "customer-2"
        tracker.enqueueEvent(TrackerEvent(type = "click"))
        tracker.flush()

        val events = network.requestBodies.map { body ->
            SdkJson.decodeFromString<List<TrackerEvent>>(body).single()
        }
        assertEquals(listOf(37L, 38L), events.map { it.systemCustomerId })
        assertEquals(listOf("customer-1", "customer-2"), events.map { it.systemCustomerExternalId })
        tracker.close()
        scope.cancel()
    }

    @Test
    fun tracker_exposes_queue_and_delivery_updates() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(TrackerConnectionStatus.CONNECTED, tracker.state.value.connectionStatus)
        assertTrue(tracker.state.value.serverAllowsTracking)
        assertEquals(0, tracker.state.value.pendingEventCount)
        assertTrue(tracker.state.value.lastSuccessfulFlushAtEpochMs != null)
        assertEquals(
            listOf(
                TrackerEventUpdate.Queued::class,
                TrackerEventUpdate.Sending::class,
                TrackerEventUpdate.Delivered::class,
            ),
            updates.map { it::class },
        )
        tracker.close()
    }

    @Test
    fun tracker_reports_backend_rejection_and_removes_event_from_queue() = runBlocking {
        val eventId = "4dd2d510-7506-40bf-959c-61d6af5b05d7"
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseBody = """{"$eventId":{"data.cart_id":"required"}}""",
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(id = eventId, type = "cart"))

        val rejection = updates.filterIsInstance<TrackerEventUpdate.Rejected>().single()
        assertEquals(eventId, rejection.event.id)
        assertTrue(rejection.reason.contains("cart_id"))
        assertEquals(0, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun http_error_keeps_event_for_periodic_retry() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCode = 400,
            eventResponseBody = """{"error":"invalid request"}""",
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        val retry = updates.filterIsInstance<TrackerEventUpdate.RetryScheduled>().single()
        assertTrue(retry.reason.contains("HTTP 400"))
        assertTrue(retry.reason.contains("invalid request"))
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun middleware_error_array_is_visible_and_event_is_kept() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCode = 403,
            eventResponseBody = """{"errors":["user is wrong"]}""",
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        val retry = updates.filterIsInstance<TrackerEventUpdate.RetryScheduled>().single()
        assertTrue(retry.reason.contains("HTTP 403"))
        assertTrue(retry.reason.contains("user is wrong"))
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun server_error_keeps_event_for_retry() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCode = 500,
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(1, updates.filterIsInstance<TrackerEventUpdate.RetryScheduled>().size)
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun oversized_event_is_rejected_without_blocking_the_next_event() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 2)
        val updates = mutableListOf<TrackerEventUpdate>()
        tracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(
            TrackerEvent(
                id = "oversized-event",
                type = "click",
                data = buildJsonObject { put("payload", "x".repeat(1024 * 1024)) },
            ),
        )
        tracker.enqueueEvent(TrackerEvent(id = "next-event", type = "click"))

        withTimeout(5_000) {
            while (tracker.state.value.pendingEventCount != 0) {
                delay(10)
            }
        }

        val rejection = updates.filterIsInstance<TrackerEventUpdate.Rejected>().single()
        assertEquals("oversized-event", rejection.event.id)
        assertTrue(rejection.reason.contains("maximum request body size"))
        val delivered = updates.filterIsInstance<TrackerEventUpdate.Delivered>().single()
        assertEquals(listOf("next-event"), delivered.events.map { it.id })
        val sentEvents = SdkJson.parseToJsonElement(network.requestBodies.last()).jsonArray
        assertEquals(1, sentEvents.size)
        assertEquals(
            "next-event",
            sentEvents.single().jsonObject.getValue("id").jsonPrimitive.content
        )
        tracker.close()
    }

    @Test
    fun payload_too_large_response_splits_the_batch() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCodes = listOf(413, 200, 200),
        )
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.connect()
        repeat(4) { index ->
            tracker.enqueueEvent(TrackerEvent(id = "event-$index", type = "click"))
        }

        tracker.flush()
        tracker.flush()

        val eventRequests = network.requestBodies.drop(1).map { body ->
            SdkJson.parseToJsonElement(body).jsonArray.size
        }
        assertEquals(listOf(4, 2, 2), eventRequests)
        assertEquals(0, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun payload_too_large_response_rejects_a_single_event_and_continues() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCodes = listOf(413, 413, 200),
        )
        val tracker = createTracker(network, trackerBatchSize = 50)
        val updates = mutableListOf<TrackerEventUpdate>()
        tracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(id = "rejected-event", type = "click"))
        tracker.enqueueEvent(TrackerEvent(id = "delivered-event", type = "click"))

        tracker.flush()

        val rejection = updates.filterIsInstance<TrackerEventUpdate.Rejected>().single()
        assertEquals("rejected-event", rejection.event.id)
        assertTrue(rejection.reason.contains("HTTP 413"))
        val delivered = updates.filterIsInstance<TrackerEventUpdate.Delivered>().single()
        assertEquals(listOf("delivered-event"), delivered.events.map { it.id })
        assertEquals(0, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun backend_try_again_later_keeps_event_for_retry() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCode = 422,
            eventResponseBody = """{"error":"try again later"}""",
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(1, updates.filterIsInstance<TrackerEventUpdate.RetryScheduled>().size)
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun rate_limit_without_retry_after_keeps_event_for_retry() = runBlocking {
        val network = FakeNetworkEngine(
            trackerEnabled = true,
            eventResponseCode = 429,
        )
        val tracker = createTracker(network)
        val updates = mutableListOf<TrackerEventUpdate>()
        val publicTracker: Tracker = tracker
        publicTracker.setEventUpdateListener { update -> updates += update }

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "page_view"))

        assertEquals(1, updates.filterIsInstance<TrackerEventUpdate.RetryScheduled>().size)
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun manual_timer_and_batch_triggered_flushes_are_single_flight() = runBlocking {
        val firstEventId = "first-event"
        val secondEventId = "second-event"
        val network = BlockingEventNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(
            network = network,
            trackerBatchSize = 2,
            trackerFlushIntervalMs = 100,
        )

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(id = firstEventId, type = "page_view"))
        val manualFlush = launch { tracker.flush() }
        withTimeout(1_000) { network.firstEventRequestStarted.await() }

        withTimeout(500) {
            tracker.enqueueEvent(TrackerEvent(id = secondEventId, type = "click"))
        }
        delay(150)
        network.releaseFirstEventRequest.complete(Unit)

        withTimeout(2_000) {
            manualFlush.join()
            while (network.eventRequestBodies.size < 2 || tracker.state.value.pendingEventCount > 0) {
                delay(10)
            }
        }

        val sentEventIds = network.eventRequestBodies.flatMap { body ->
            SdkJson.decodeFromString<List<TrackerEvent>>(body).mapNotNull { it.id }
        }
        assertEquals(1, network.maxActiveEventRequests.get())
        assertEquals(listOf(firstEventId, secondEventId), sentEventIds)
        tracker.close()
    }

    @Test
    fun pending_events_are_isolated_by_connected_user() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val storage = ScopedMemoryTrackerStorage()
        val tracker = createTracker(
            network = network,
            trackerBatchSize = 50,
            trackerStorage = storage,
        )
        val firstUserEventId = "8f0ce8e5-8279-46ed-902f-1d5c6415e40a"
        val secondUserEventId = "f286a383-7edf-47cd-af82-4256136c76dd"

        network.userToken = "first-user-token"
        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(id = firstUserEventId, type = "click"))
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.disconnect()

        network.userToken = "second-user-token"
        tracker.connect()
        assertEquals(0, tracker.state.value.pendingEventCount)
        tracker.enqueueEvent(TrackerEvent(id = secondUserEventId, type = "click"))
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.disconnect()

        network.userToken = "first-user-token"
        tracker.connect()
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.flush()

        val flushedEvents = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        )
        assertEquals(listOf(firstUserEventId), flushedEvents.map { it.id })
        assertEquals(0, tracker.state.value.pendingEventCount)

        tracker.disconnect()
        network.userToken = "second-user-token"
        tracker.connect()
        assertEquals(1, tracker.state.value.pendingEventCount)
        tracker.close()
    }

    @Test
    fun site_customer_id_set_before_flush_is_applied_to_event_and_headers() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "click"))
        tracker.setSiteCustomerId(" shop-customer-42 ")
        tracker.flush()

        val event = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        ).single()
        assertEquals(null, event.systemCustomerId)
        assertEquals("shop-customer-42", event.systemCustomerExternalId)
        assertEquals("shop-customer-42", network.requestHeaders.last()["X-Site-Customer-Id"])
        tracker.close()
    }

    @Test
    fun site_customer_id_setter_uses_latest_value_for_each_flush() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.setSiteCustomerId("customer-1")
        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(type = "click"))
        tracker.flush()
        tracker.setSiteCustomerId("customer-2")
        tracker.enqueueEvent(TrackerEvent(type = "login"))
        tracker.flush()

        val sentIds = network.requestBodies.drop(1).map { body ->
            SdkJson.decodeFromString<List<TrackerEvent>>(body)
                .single()
                .systemCustomerExternalId
        }
        assertEquals(listOf("customer-1", "customer-2"), sentIds)
        tracker.close()
    }

    @Test
    fun event_customer_identity_overrides_tracker_identity() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.setSiteCustomerId("global-external-id")
        tracker.connect()
        tracker.enqueueEvent(
            TrackerEvent(
                type = "click",
                systemCustomerId = 84L,
                systemCustomerExternalId = "event-site-id",
            ),
        )
        tracker.flush()

        val event = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        ).single()
        assertEquals(84L, event.systemCustomerId)
        assertEquals("event-site-id", event.systemCustomerExternalId)
        assertEquals("global-external-id", network.requestHeaders.last()["X-Site-Customer-Id"])
        assertEquals("global-external-id", network.requestHeaders.first()["X-Site-Customer-Id"])
        tracker.close()
    }

    @Test
    fun blank_site_customer_id_is_rejected() {
        val tracker = createTracker(FakeNetworkEngine(trackerEnabled = true))

        val error = runCatching { tracker.setSiteCustomerId("  ") }.exceptionOrNull()

        assertEquals("Site customer ID must not be blank", error?.message)
        tracker.close()
    }

    @Test
    fun null_site_customer_id_clears_identity() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)

        tracker.setSiteCustomerId("customer-42")
        tracker.connect()
        tracker.setSiteCustomerId(null)
        tracker.enqueueEvent(TrackerEvent(type = "click"))
        tracker.flush()

        val event = SdkJson.decodeFromString<List<TrackerEvent>>(
            network.requestBodies.last(),
        ).single()
        assertEquals(null, event.systemCustomerExternalId)
        assertEquals(null, network.requestHeaders.last()["X-Site-Customer-Id"])
        tracker.close()
    }

    @Test
    fun incremental_storage_only_upserts_new_events() = runBlocking {
        val storage = RecordingIncrementalTrackerStorage()
        val tracker = createTracker(
            network = FakeNetworkEngine(trackerEnabled = true),
            trackerBatchSize = 50,
            trackerStorage = storage,
        )
        val firstEventId = "496aaefd-250f-47ec-b5f8-79fa30cdee85"
        val secondEventId = "3537d0fa-5d99-4e72-bc6e-9bcb8559a797"

        tracker.connect()
        tracker.enqueueEvent(TrackerEvent(id = firstEventId, type = "click"))
        tracker.enqueueEvent(TrackerEvent(id = secondEventId, type = "login"))

        assertEquals(listOf(listOf(firstEventId), listOf(secondEventId)), storage.upsertedEventIds)
        assertEquals(0, storage.fullSnapshotWrites)
        tracker.close()
    }

    @Test
    fun public_listener_can_be_replaced_and_removed_without_replaying_updates() = runBlocking {
        val tracker = createTracker(FakeNetworkEngine(trackerEnabled = true), trackerBatchSize = 50)
        val publicTracker: Tracker = tracker
        val firstUpdates = mutableListOf<TrackerEventUpdate>()
        val secondUpdates = mutableListOf<TrackerEventUpdate>()
        try {
            tracker.connect()
            publicTracker.trackClick("before_listener")
            publicTracker.setEventUpdateListener { firstUpdates += it }
            assertTrue(firstUpdates.isEmpty())
            publicTracker.trackClick("first_listener")
            publicTracker.setEventUpdateListener { secondUpdates += it }
            publicTracker.trackClick("second_listener")
            publicTracker.setEventUpdateListener(null)
            publicTracker.trackClick("no_listener")
            publicTracker.flush()

            assertEquals(1, firstUpdates.size)
            assertEquals(1, secondUpdates.size)
            assertTrue(firstUpdates.single() is TrackerEventUpdate.Queued)
            assertTrue(secondUpdates.single() is TrackerEventUpdate.Queued)
            assertTrue(
                firstUpdates.single().events.single().id != secondUpdates.single().events.single().id,
            )
        } finally {
            tracker.close()
        }
    }

    @Test
    fun listener_exception_does_not_interrupt_queue_or_delivery() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true)
        val tracker = createTracker(network, trackerBatchSize = 50)
        val publicTracker: Tracker = tracker
        val updates = mutableListOf<TrackerEventUpdate>()
        publicTracker.setEventUpdateListener {
            updates += it
            throw IllegalStateException("Application listener failed")
        }
        try {
            tracker.connect()
            publicTracker.trackClick("product")
            publicTracker.flush()

            assertEquals(
                listOf(
                    TrackerEventUpdate.Queued::class,
                    TrackerEventUpdate.Sending::class,
                    TrackerEventUpdate.Delivered::class,
                ),
                updates.map { it::class },
            )
            assertEquals(TrackerEventKind.CLICK, updates.first().events.single().kind)
            assertEquals(updates.first().events, updates.last().events)
            assertEquals(0, tracker.state.value.pendingEventCount)
        } finally {
            tracker.close()
        }
    }

    @Test
    fun public_listener_reports_queue_overflow() = runBlocking {
        val tracker = createTracker(
            network = FakeNetworkEngine(trackerEnabled = true),
            trackerBatchSize = 50,
            trackerMaxQueueSize = 1,
        )
        val publicTracker: Tracker = tracker
        val updates = mutableListOf<TrackerEventUpdate>()
        publicTracker.setEventUpdateListener { updates += it }
        try {
            tracker.connect()
            publicTracker.trackClick("first")
            publicTracker.trackClick("second")

            val dropped = updates.filterIsInstance<TrackerEventUpdate.Dropped>().single()
            assertEquals(updates.first().events, dropped.events)
            assertTrue(dropped.reason.isNotBlank())
            assertEquals(1, tracker.state.value.pendingEventCount)
        } finally {
            tracker.close()
        }
    }

    @Test
    fun sending_update_contains_exact_outgoing_body_before_each_attempt() = runBlocking {
        val network = FakeNetworkEngine(trackerEnabled = true, eventResponseCode = 400)
        val tracker = createTracker(network, trackerBatchSize = 50)
        val publicTracker: Tracker = tracker
        val attempts = mutableListOf<TrackerEventUpdate.Sending>()
        val requestCounts = mutableListOf<Int>()
        publicTracker.setEventUpdateListener { update ->
            if (update is TrackerEventUpdate.Sending) {
                attempts += update
                requestCounts += network.requestBodies.size
            }
        }
        try {
            tracker.setSiteCustomerId("customer-42")
            tracker.connect(SessionPageContext(url = "app://catalog", title = "Каталог"))
            publicTracker.trackRegister(
                TrackerCustomer(email = "customer@example.com"),
                code = "signup"
            )
            publicTracker.flush()
            tracker.setSiteCustomerId("customer-84")
            publicTracker.flush()

            assertEquals(network.requestBodies.drop(1), attempts.map { it.payload })
            assertEquals(listOf(1, 2), requestCounts)
            assertEquals(2, attempts.size)
            val events = attempts.map {
                SdkJson.parseToJsonElement(it.payload).jsonArray.single().jsonObject
            }
            assertEquals(
                listOf("customer-42", "customer-84"),
                events.map { it.getValue("system_customer_external_id").jsonPrimitive.content },
            )
            events.forEachIndexed { index, event ->
                assertEquals("android", event.getValue("platform").jsonPrimitive.content)
                assertEquals("signup", event.getValue("code").jsonPrimitive.content)
                assertEquals("app://catalog", event.getValue("page_url").jsonPrimitive.content)
                assertEquals(
                    "customer@example.com",
                    event.getValue("data").jsonObject.getValue("customer_email").jsonPrimitive.content,
                )
                assertEquals(
                    event.getValue("id").jsonPrimitive.content,
                    attempts[index].events.single().id
                )
            }
        } finally {
            tracker.close()
        }
    }

    private fun createTracker(
        network: SdkHttpNetworkEngine,
        trackerBatchSize: Int = 1,
        trackerFlushIntervalMs: Long = 15_000,
        trackerTimeSyncIntervalMs: Long = 60 * 60 * 1000,
        trackerStorage: TrackerStorage = ScopedMemoryTrackerStorage(),
        trackerMaxQueueSize: Int = 1_000,
    ): CrmTracker = CrmTracker.create(
        config = CrmTrackerConfig(
            siteToken = "site-token",
            baseUrl = "https://consultant.example.com",
            trackerBatchSize = trackerBatchSize,
            trackerFlushIntervalMs = trackerFlushIntervalMs,
            trackerTimeSyncIntervalMs = trackerTimeSyncIntervalMs,
            trackerMaxQueueSize = trackerMaxQueueSize,
        ),
        networkEngine = network,
        sessionStorage = MemorySessionStorage(),
        trackerStorage = trackerStorage,
        logger = NoOpLogger,
    )

    private class RecordingConsentHandler(
        private val result: TrackerConsentState,
    ) : TrackerConsentHandler {
        @Volatile
        var getCalls = 0

        override suspend fun getConsent(): TrackerConsentState {
            getCalls += 1
            return result
        }
    }
}
