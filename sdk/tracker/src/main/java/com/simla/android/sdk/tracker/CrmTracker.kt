package com.simla.android.sdk.tracker

import android.content.Context
import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmUtmAttribution
import com.simla.android.sdk.logging.DefaultSdkLogger
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.session.SdkConnectRequest
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.storage.KeystoreSdkSessionStorage
import com.simla.android.sdk.storage.SdkSessionStorage
import com.simla.android.sdk.tracker.model.TrackerConnectResponseDto
import com.simla.android.sdk.tracker.model.TrackerConnectionResult
import com.simla.android.sdk.tracker.model.TrackerCustomerIdentity
import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.model.TrackerUtm
import com.simla.android.sdk.tracker.model.toDomain
import com.simla.android.sdk.tracker.model.toDto
import com.simla.android.sdk.tracker.serialization.SdkJson
import com.simla.android.sdk.tracker.storage.SQLiteTrackerStorage
import com.simla.android.sdk.tracker.storage.TrackerStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

internal class CrmTracker private constructor(
    private val config: TrackerRuntimeConfig,
    private val logger: SdkLogger,
    private val networkEngine: SdkHttpNetworkEngine,
    trackerStorage: TrackerStorage,
    private val scope: CoroutineScope,
    private val ownsScope: Boolean,
    private val ownsNetworkEngine: Boolean,
    private val standaloneConnection: StandaloneTrackerConnection?,
    private val managedCustomerSystemIdProvider: (() -> Long?)?,
    private val managedSiteCustomerIdProvider: (() -> String?)?,
    private val utmAttributionProvider: () -> CrmUtmAttribution?,
    sessionProvider: suspend () -> SdkSession?,
    authorizedHeadersProvider: suspend () -> Map<String, String>,
    endpointResolver: (String) -> String,
) : Tracker {
    private val stateMutex = Mutex()
    private val consentResolutionMutex = Mutex()
    private val mutableState = MutableStateFlow(
        CrmTrackerState(),
    )
    internal val state: StateFlow<CrmTrackerState> = mutableState.asStateFlow()
    private val trackerObserver = object : ConsultantTrackerObserver {
        override fun onQueueSizeChanged(size: Int) {
            updateState { copy(pendingEventCount = size) }
        }

        override fun onEventUpdate(update: TrackerEventUpdate) {
            val listener = eventUpdateListener ?: return
            try {
                listener.onEventUpdate(update)
            } catch (error: Exception) {
                logger.e("tracker event update listener failed", error)
            }
        }

        override fun onFlushSucceeded(atEpochMs: Long) {
            updateState {
                copy(
                    lastSuccessfulFlushAtEpochMs = atEpochMs,
                    lastError = null,
                    rateLimitedUntilEpochMs = null,
                )
            }
        }

        override fun onError(message: String?) {
            updateState { copy(lastError = message) }
        }

        override fun onRateLimited(untilEpochMs: Long?) {
            updateState { copy(rateLimitedUntilEpochMs = untilEpochMs) }
        }
    }
    private var serverAllowsTracking = false
    private var consentRequired = false
    private var consentState = TrackerConsentState.UNKNOWN

    @Volatile
    private var consentHandler: TrackerConsentHandler? = null

    @Volatile
    private var eventUpdateListener: TrackerEventUpdateListener? = null

    @Volatile
    private var siteCustomerId: String? = null
    private val pageContextMutex = Mutex()
    private var currentPageContext: SessionPageContext? = null
    private val dispatcher = ConsultantTrackerDispatcher(
        config = config,
        logger = logger,
        networkEngine = networkEngine,
        trackerStorage = trackerStorage,
        sdkScope = scope,
        sessionProvider = sessionProvider,
        authorizedHeadersProvider = authorizedHeadersProvider,
        customerIdentityProvider = ::resolveCustomerIdentity,
        endpointResolver = endpointResolver,
        currentPageContextProvider = {
            pageContextMutex.withLock { currentPageContext }
        },
        observer = trackerObserver,
    )

    internal fun setSiteCustomerId(id: String?) {
        siteCustomerId = id?.trim()?.also {
            require(it.isNotEmpty()) { "Site customer ID must not be blank" }
        }
    }

    suspend fun connect(
        pageContext: SessionPageContext = SessionPageContext(),
        user: SdkUserPayload = SdkUserPayload(),
    ): TrackerConnectionResult {
        val connection = standaloneConnection
            ?: error("Tracker connection is managed by CrmSdk")
        updateState {
            copy(
                connectionStatus = TrackerConnectionStatus.CONNECTING,
                lastError = null
            )
        }
        try {
            val request = SdkConnectRequest(
                user = user.copy(token = user.token ?: connection.sessionStorage.getUserToken()),
                pageTitle = pageContext.title,
                url = pageContext.url,
                referUrl = pageContext.referUrl,
            )
            val response = networkEngine.postJson(
                SdkHttpRequest(
                    url = connection.endpoint("connect"),
                    headers = connection.baseHeaders() + siteCustomerIdHeaders(),
                    body = SdkJson.encodeToString(request.toDto()),
                ),
            )
            if (!response.isSuccessful) {
                throw IllegalStateException("Tracker connect failed: HTTP ${response.code}")
            }
            val parsed = SdkJson.decodeFromString<TrackerConnectResponseDto>(response.body)
                .toDomain()
            val userToken = parsed.user.token ?: request.user.token
            ?: throw IllegalStateException("Connect response does not contain user token")
            val sessionId = parsed.session.idValue
                ?: throw IllegalStateException("Connect response does not contain session id")
            connection.stateMutex.withLock {
                connection.activeSession = SdkSession(
                    userToken = userToken,
                    sessionId = sessionId,
                    siteToken = connection.config.siteToken,
                    platformName = "android",
                )
            }
            connection.sessionStorage.setUserToken(userToken)
            onHostConnected(
                serverTs = parsed.ts,
                trackerEnabled = parsed.behaviour.tracker == true,
                consentRequired = parsed.behaviour.consentForm == true,
                pageContext = pageContext,
            )
            return if (parsed.behaviour.tracker == true) {
                TrackerConnectionResult.Available(parsed)
            } else {
                TrackerConnectionResult.DisabledByServer(parsed)
            }
        } catch (throwable: Throwable) {
            updateState {
                copy(
                    connectionStatus = TrackerConnectionStatus.FAILED,
                    lastError = throwable.message ?: throwable::class.java.simpleName,
                )
            }
            throw throwable
        }
    }

    override fun setEventUpdateListener(listener: TrackerEventUpdateListener?) {
        eventUpdateListener = listener
    }

    override fun setConsentHandler(handler: TrackerConsentHandler?) {
        consentHandler = handler
        if (state.value.connectionStatus == TrackerConnectionStatus.CONNECTED) {
            scheduleConsentResolution()
        }
    }

    internal suspend fun setTrackingConsent(consent: TrackerConsentState) {
        val (runtimeEnabled, awaitEnablement) = stateMutex.withLock {
            consentState = consent
            isRuntimeTrackingEnabledLocked() to shouldAwaitEnablementLocked()
        }
        updateState { copy(consentState = consent) }
        dispatcher.onConnect(
            serverTs = null,
            trackerEnabled = runtimeEnabled,
            awaitEnablement = awaitEnablement,
        )
        if (!runtimeEnabled && consent != TrackerConsentState.GRANTED) {
            if (!awaitEnablement) {
                dispatcher.clear()
            }
        }
    }

    override suspend fun trackPageView(
        screenUri: String,
        title: String?,
        product: TrackerViewedProduct?,
        code: String?,
    ) = trackPageViewEvent(
        screenUri = screenUri,
        title = title,
        data = TrackerEventDataFactory.pageView(product),
        code = code,
    )

    internal suspend fun trackPageViewEvent(
        screenUri: String,
        title: String?,
        data: JsonObject?,
        code: String? = null,
    ) {
        val pageView = pageContextMutex.withLock {
            val normalizedScreenUri = normalizeScreenUri(screenUri)
            val referrerUrl = currentPageContext?.url
            currentPageContext = SessionPageContext(
                title = title,
                url = normalizedScreenUri,
                referUrl = referrerUrl,
            )
            TrackerEvent(
                type = "page_view",
                code = normalizeEventCode(code),
                data = data,
                pageUrl = normalizedScreenUri,
                pageTitle = title,
                referrerUrl = referrerUrl,
            )
        }
        dispatcher.track(listOf(pageView))
    }

    override suspend fun trackClick(element: String, leaving: Boolean, code: String?) =
        trackTypedEvent("click", TrackerEventDataFactory.click(element, leaving), code)

    override suspend fun trackRegister(customer: TrackerCustomer, code: String?) =
        trackTypedEvent("register", TrackerEventDataFactory.register(customer), code)

    override suspend fun trackLogin(
        customerEmail: String,
        subscriptions: List<TrackerSubscription>,
        code: String?,
    ) = trackTypedEvent(
        "login",
        TrackerEventDataFactory.customerEmail(customerEmail, subscriptions),
        code,
    )

    override suspend fun trackConsult(
        customerEmail: String,
        subscriptions: List<TrackerSubscription>,
        code: String?,
    ) = trackTypedEvent(
        "consult",
        TrackerEventDataFactory.customerEmail(customerEmail, subscriptions),
        code,
    )

    override suspend fun trackOpenCart(customerEmail: String?, code: String?) =
        trackTypedEvent("open_cart", TrackerEventDataFactory.openCart(customerEmail), code)

    override suspend fun trackContact(channel: TrackerContactChannel, code: String?) =
        trackTypedEvent("contact", TrackerEventDataFactory.contact(channel), code)

    override suspend fun trackCart(
        cartId: String,
        items: List<TrackerCartItem>,
        code: String?,
    ) = trackTypedEvent("cart", TrackerEventDataFactory.cart(cartId, items), code)

    override suspend fun trackOrder(
        identifier: TrackerOrderIdentifier,
        cartId: String?,
        items: List<TrackerCartItem>,
        deliveryPrice: Double?,
        total: Double?,
        code: String?,
    ) = trackTypedEvent(
        "order",
        TrackerEventDataFactory.order(identifier, cartId, items, deliveryPrice, total),
        code,
        utmAttributionProvider()?.toTrackerUtm(),
    )

    override suspend fun trackSubscription(
        subscription: TrackerSubscription,
        code: String?,
    ) = trackTypedEvent(
        "subscription",
        TrackerEventDataFactory.subscription(subscription),
        code,
    )

    override suspend fun trackFormStart(formSelector: String, code: String?) =
        trackTypedEvent("form_start", TrackerEventDataFactory.form(formSelector), code)

    override suspend fun trackFormSubmit(formSelector: String, code: String?) =
        trackTypedEvent("form_submit", TrackerEventDataFactory.form(formSelector), code)

    override suspend fun trackFileDownload(
        fileName: String,
        element: String?,
        code: String?,
    ) = trackTypedEvent(
        "file_download",
        TrackerEventDataFactory.fileDownload(fileName, element),
        code,
    )

    override suspend fun trackFavoriteProduct(
        offer: TrackerOfferIdentifier,
        removed: Boolean,
        code: String?,
    ) = trackTypedEvent(
        "favorite_product",
        TrackerEventDataFactory.favoriteProduct(offer, removed),
        code,
    )

    private suspend fun trackTypedEvent(
        type: String,
        data: JsonObject?,
        code: String?,
        utm: TrackerUtm? = null,
    ) {
        enqueueEvent(
            TrackerEvent(
                type = type,
                data = data,
                code = normalizeEventCode(code),
                utm = utm,
            ),
        )
    }

    private fun normalizeEventCode(code: String?): String? = code?.trim()?.also {
        require(it.isNotEmpty()) { "Event code must not be blank" }
    }

    private fun CrmUtmAttribution.toTrackerUtm(): TrackerUtm = TrackerUtm(
        source = source,
        medium = medium,
        campaign = campaign,
        keyword = term,
        content = content,
    )

    internal suspend fun enqueueEvent(event: TrackerEvent) {
        dispatcher.track(listOf(event))
    }

    override suspend fun flush() {
        dispatcher.flush()
    }

    suspend fun disconnect() {
        val connection = standaloneConnection
            ?: error("Tracker connection is managed by CrmSdk")
        onHostDisconnected()
        connection.stateMutex.withLock { connection.activeSession = null }
    }

    suspend fun forgetUser() {
        val connection = standaloneConnection
            ?: error("Tracker user lifecycle is managed by CrmSdk")
        disconnect()
        setSiteCustomerId(null)
        connection.sessionStorage.clear()
        dispatcher.clear()
    }

    fun close() {
        eventUpdateListener = null
        dispatcher.close()
        updateState {
            copy(
                connectionStatus = TrackerConnectionStatus.DISCONNECTED,
                serverAllowsTracking = false,
                consentRequired = false,
            )
        }
        if (ownsScope) {
            scope.cancel()
        }
        if (ownsNetworkEngine) {
            networkEngine.close()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun onHostConnected(
        serverTs: String?,
        trackerEnabled: Boolean,
        consentRequired: Boolean = false,
        pageContext: SessionPageContext = SessionPageContext(),
    ) {
        pageContextMutex.withLock {
            currentPageContext = pageContext
        }
        val (runtimeEnabled, awaitEnablement) = stateMutex.withLock {
            serverAllowsTracking = trackerEnabled
            this.consentRequired = consentRequired
            isRuntimeTrackingEnabledLocked() to shouldAwaitEnablementLocked()
        }
        updateState {
            copy(
                connectionStatus = TrackerConnectionStatus.CONNECTED,
                serverAllowsTracking = trackerEnabled,
                consentRequired = consentRequired,
                lastError = null,
            )
        }
        dispatcher.onConnect(
            serverTs = serverTs,
            trackerEnabled = runtimeEnabled,
            awaitEnablement = awaitEnablement,
        )
        if (!runtimeEnabled && consentState == TrackerConsentState.DENIED) {
            dispatcher.clear()
        }
        scheduleConsentResolution()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun onHostDisconnected() {
        stateMutex.withLock {
            serverAllowsTracking = false
            consentRequired = false
        }
        pageContextMutex.withLock { currentPageContext = null }
        updateState {
            copy(
                connectionStatus = TrackerConnectionStatus.DISCONNECTED,
                serverAllowsTracking = false,
                consentRequired = false,
            )
        }
        dispatcher.onConnect(
            serverTs = null,
            trackerEnabled = false,
            awaitEnablement = true,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun clearPendingData() {
        dispatcher.clear()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun currentSiteCustomerId(): String? =
        managedSiteCustomerIdProvider?.invoke() ?: siteCustomerId

    internal fun resolveCustomerIdentity(): TrackerCustomerIdentity =
        TrackerCustomerIdentity(
            systemId = managedCustomerSystemIdProvider?.invoke(),
            siteCustomerId = managedSiteCustomerIdProvider?.invoke() ?: siteCustomerId,
        )

    private fun siteCustomerIdHeaders(): Map<String, String> =
        currentSiteCustomerId()?.let { mapOf("X-Site-Customer-Id" to it) }.orEmpty()

    private fun updateState(transform: CrmTrackerState.() -> CrmTrackerState) {
        mutableState.update { current -> current.transform() }
    }

    private fun scheduleConsentResolution() {
        scope.launch {
            consentResolutionMutex.withLock {
                val shouldResolve = stateMutex.withLock {
                    serverAllowsTracking && consentRequired
                }
                if (!shouldResolve) {
                    return@withLock
                }
                val handler = consentHandler
                if (handler == null) {
                    setTrackingConsent(TrackerConsentState.UNKNOWN)
                    return@withLock
                }

                val consent = runCatching { handler.getConsent() }
                    .onFailure { throwable ->
                        logger.e("unable to get tracker consent", throwable)
                    }
                    .getOrNull()
                    ?: return@withLock
                setTrackingConsent(consent)
            }
        }
    }

    private fun isRuntimeTrackingEnabledLocked(): Boolean =
        serverAllowsTracking &&
                consentState != TrackerConsentState.DENIED &&
                (!consentRequired || consentState == TrackerConsentState.GRANTED)

    private fun shouldAwaitEnablementLocked(): Boolean =
        serverAllowsTracking &&
                consentRequired &&
                consentState == TrackerConsentState.UNKNOWN

    companion object {
        fun create(
            config: CrmTrackerConfig,
            networkEngine: SdkHttpNetworkEngine,
            sessionStorage: SdkSessionStorage,
            trackerStorage: TrackerStorage,
            logger: SdkLogger = DefaultSdkLogger(config.debug),
        ): CrmTracker {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return createStandalone(
                config = config,
                logger = logger,
                networkEngine = networkEngine,
                sessionStorage = sessionStorage,
                trackerStorage = trackerStorage,
                scope = scope,
                ownsScope = true,
                ownsNetworkEngine = false,
            )
        }

        fun create(
            context: Context,
            config: CrmTrackerConfig,
            networkEngine: SdkHttpNetworkEngine,
            logger: SdkLogger = DefaultSdkLogger(config.debug),
            sessionStorage: SdkSessionStorage? = null,
            trackerStorage: TrackerStorage? = null,
        ): CrmTracker {
            val appContext = context.applicationContext
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return createStandalone(
                config = config,
                logger = logger,
                networkEngine = networkEngine,
                sessionStorage = sessionStorage ?: KeystoreSdkSessionStorage(
                    appContext
                ),
                trackerStorage = trackerStorage ?: SQLiteTrackerStorage(
                    appContext
                ),
                scope = scope,
                ownsScope = true,
                ownsNetworkEngine = false,
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createManaged(
            config: TrackerRuntimeConfig,
            logger: SdkLogger,
            networkEngine: SdkHttpNetworkEngine,
            trackerStorage: TrackerStorage,
            scope: CoroutineScope,
            sessionProvider: suspend () -> SdkSession?,
            authorizedHeadersProvider: suspend () -> Map<String, String>,
            endpointResolver: (String) -> String,
            customerSystemIdProvider: () -> Long? = { null },
            siteCustomerIdProvider: () -> String? = { null },
            utmAttributionProvider: () -> CrmUtmAttribution? = { null },
        ): CrmTracker = CrmTracker(
            config = config,
            logger = logger,
            networkEngine = networkEngine,
            trackerStorage = trackerStorage,
            scope = scope,
            ownsScope = false,
            ownsNetworkEngine = false,
            standaloneConnection = null,
            managedCustomerSystemIdProvider = customerSystemIdProvider,
            managedSiteCustomerIdProvider = siteCustomerIdProvider,
            utmAttributionProvider = utmAttributionProvider,
            sessionProvider = sessionProvider,
            authorizedHeadersProvider = authorizedHeadersProvider,
            endpointResolver = endpointResolver,
        )

        private fun createStandalone(
            config: CrmTrackerConfig,
            logger: SdkLogger,
            networkEngine: SdkHttpNetworkEngine,
            sessionStorage: SdkSessionStorage,
            trackerStorage: TrackerStorage,
            scope: CoroutineScope,
            ownsScope: Boolean,
            ownsNetworkEngine: Boolean,
        ): CrmTracker {
            val connection = StandaloneTrackerConnection(config, sessionStorage)
            return CrmTracker(
                config = config,
                logger = logger,
                networkEngine = networkEngine,
                trackerStorage = trackerStorage,
                scope = scope,
                ownsScope = ownsScope,
                ownsNetworkEngine = ownsNetworkEngine,
                standaloneConnection = connection,
                managedCustomerSystemIdProvider = null,
                managedSiteCustomerIdProvider = null,
                utmAttributionProvider = { null },
                sessionProvider = { connection.stateMutex.withLock { connection.activeSession } },
                authorizedHeadersProvider = { connection.authorizedHeaders() },
                endpointResolver = connection::endpoint,
            )
        }
    }
}
