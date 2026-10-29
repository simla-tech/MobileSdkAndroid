package com.simla.android.sdk.runtime

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.CrmSdkConfig
import com.simla.android.sdk.CrmSdkInternalApi
import com.simla.android.sdk.error.CrmSdkException
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.serialization.SdkBehaviourDto
import com.simla.android.sdk.serialization.SdkConnectedSessionDto
import com.simla.android.sdk.serialization.SdkConnectedUserDto
import com.simla.android.sdk.serialization.SdkJson
import com.simla.android.sdk.serialization.asFlexibleString
import com.simla.android.sdk.serialization.toDomain
import com.simla.android.sdk.serialization.toDto
import com.simla.android.sdk.session.SdkBehaviour
import com.simla.android.sdk.session.SdkConnectRequest
import com.simla.android.sdk.session.SdkConnectedSession
import com.simla.android.sdk.session.SdkConnectedUser
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.storage.SdkSessionStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

/** Shared SDK lifecycle used by all installed features. */
@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CrmRuntime(
    val config: CrmSdkConfig,
    val networkEngine: SdkNetworkEngine,
    private val logger: SdkLogger,
    private val sessionStorage: SdkSessionStorage,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val stateMutex = Mutex()
    private val connectionMutex = Mutex()
    private val customerIdentityMutex = Mutex()
    private val lifecycleLock = Any()
    private val listeners = mutableListOf<CrmRuntimeListener>()
    private var activeSession: SdkSession? = null
    private var lastConnectResponse: CrmConnectResponse? = null
    private val initializationLock = Any()
    private var initialized = false
    private var initialPageContext = SessionPageContext()
    private var initialUser = SdkUserPayload()
    private var connectedSnapshot: ConnectedSnapshot? = null

    @Volatile
    private var appInForeground = false

    @Volatile
    private var customerIdentity: CrmCustomerIdentity? = null

    fun addListener(listener: CrmRuntimeListener) {
        val snapshot = synchronized(lifecycleLock) {
            check(listener !in listeners) { "Runtime listener is already registered" }
            listeners += listener
            connectedSnapshot
        }
        if (snapshot != null) {
            scope.launch {
                notifyListener("active session initialization") {
                    listener.onConnected(snapshot.response, snapshot.pageContext, snapshot.user)
                }
            }
        }
    }

    fun initialize(
        pageContext: SessionPageContext = SessionPageContext(),
        user: SdkUserPayload = SdkUserPayload(),
    ) {
        synchronized(initializationLock) {
            check(!initialized) { "SDK runtime is already initialized" }
            initialized = true
            initialPageContext = pageContext
            initialUser = user
        }
        scope.launch {
            runCatching { awaitConnected() }
                .onFailure { logger.e("unable to initialize SDK session", it) }
        }
    }

    suspend fun awaitConnected(): CrmConnectResponse = customerIdentityMutex.withLock {
        withContext(NonCancellable) {
            val initialArguments = synchronized(initializationLock) {
                check(initialized) { "CrmSdk.init must be called first" }
                initialPageContext to initialUser
            }
            ensureConnected(
                pageContext = initialArguments.first,
                user = initialArguments.second,
            )
        }
    }

    private suspend fun ensureConnected(
        pageContext: SessionPageContext,
        user: SdkUserPayload,
    ): CrmConnectResponse = connectionMutex.withLock {
        stateMutex.withLock { lastConnectResponse }?.let { return@withLock it }
        val request = SdkConnectRequest(
            user = user.copy(token = user.token ?: sessionStorage.getUserToken()),
            pageTitle = pageContext.title,
            url = pageContext.url,
            referUrl = pageContext.referUrl,
        )
        logger.d("connect requested for ${SdkPlatform.name}")
        val response = networkEngine.postJson(
            SdkHttpRequest(
                url = resolveClientApiUrl("connect"),
                headers = buildHttpHeaders(),
                body = SdkJson.encodeToString(request.toDto()),
            ),
        )
        if (!response.isSuccessful) {
            throw CrmSdkException("Connect failed: HTTP ${response.code}")
        }

        val parsed = try {
            SdkJson.decodeFromString<CrmConnectPayload>(response.body)
        } catch (throwable: Throwable) {
            throw CrmSdkException("Unable to parse connect response", throwable)
        }
        val connectedUser = parsed.user.toDomain()
        val connectedSession = parsed.session.toDomain()
        val behaviour = parsed.behaviour?.toDomain()
        val resolvedUserToken = connectedUser.token ?: request.user.token
        val resolvedSessionId = connectedSession.id
        if (resolvedUserToken.isNullOrBlank() || resolvedSessionId.isNullOrBlank()) {
            throw CrmSdkException("Connect response does not contain session credentials")
        }

        val connectResponse = CrmConnectResponse(
            rawBody = response.body,
            ts = parsed.ts.asFlexibleString(),
            user = connectedUser,
            session = connectedSession,
            behaviour = behaviour,
        )
        stateMutex.withLock {
            activeSession = SdkSession(
                userToken = resolvedUserToken,
                sessionId = resolvedSessionId,
                siteToken = config.siteToken,
                platformName = SdkPlatform.name,
            )
            lastConnectResponse = connectResponse
        }
        sessionStorage.setUserToken(resolvedUserToken)
        val listenersToNotify = synchronized(lifecycleLock) {
            connectedSnapshot = ConnectedSnapshot(connectResponse, pageContext, user)
            listeners.toList()
        }
        listenersToNotify.forEach { listener ->
            notifyListener("connected session initialization") {
                listener.onConnected(connectResponse, pageContext, user)
            }
        }
        connectResponse
    }

    suspend fun disconnect() = connectionMutex.withLock {
        stateMutex.withLock {
            activeSession = null
            lastConnectResponse = null
        }
        val listenersToNotify = synchronized(lifecycleLock) {
            connectedSnapshot = null
            listeners.toList()
        }
        listenersToNotify.forEach { listener ->
            notifyListener("session disconnect") { listener.onDisconnected() }
        }
    }

    suspend fun setCustomerIdentity(identity: CrmCustomerIdentity?) {
        val normalizedIdentity = identity?.copy(
            siteCustomerId = identity.siteCustomerId?.trim(),
            siteCode = identity.siteCode?.trim(),
        )
        customerIdentityMutex.withLock {
            withContext(NonCancellable) {
                if (currentCustomerIdentity() != normalizedIdentity) {
                    disconnect()
                    sessionStorage.activateScope(normalizedIdentity?.sessionScope())
                    customerIdentity = normalizedIdentity
                }
                val initializationArguments = synchronized(initializationLock) {
                    if (initialized) initialPageContext to initialUser else null
                }
                initializationArguments?.let { (pageContext, user) ->
                    ensureConnected(pageContext, user)
                }
            }
        }
    }

    suspend fun currentSession(): SdkSession? = stateMutex.withLock { activeSession }

    suspend fun currentConnectResponse(): CrmConnectResponse? =
        stateMutex.withLock { lastConnectResponse }

    fun currentSiteCustomerId(): String? = customerIdentity?.siteCustomerId

    fun currentCustomerIdentity(): CrmCustomerIdentity? = customerIdentity

    private fun CrmCustomerIdentity.sessionScope(): String =
        crmCustomerId?.let { "crm:$it" } ?: requireNotNull(siteCustomerId)

    fun resolveClientApiUrl(path: String): String = "${config.clientApiBaseUrl}/$path"

    suspend fun buildAuthorizedHttpHeaders(
        includeSiteCustomerId: Boolean = true,
    ): Map<String, String> {
        val session = currentSession() ?: run {
            awaitConnected()
            checkNotNull(currentSession())
        }
        return buildMap {
            putAll(buildHttpHeaders(includeSiteCustomerId))
            put("X-User-Token", session.userToken)
            put("X-Session-Id", session.sessionId)
        }
    }

    suspend fun buildHttpHeaders(
        includeSiteCustomerId: Boolean = true,
    ): Map<String, String> = buildMap {
        put("X-Site-Token", config.siteToken)
        put("Accept", "application/json")
        if (includeSiteCustomerId) {
            currentSiteCustomerId()?.let { put("X-Site-Customer-Id", it) }
        }
    }

    fun close() {
        synchronized(lifecycleLock) { listeners.toList() }.forEach { listener ->
            try {
                listener.onClosed()
            } catch (throwable: Throwable) {
                logger.e("unable to notify SDK feature about runtime close", throwable)
            }
        }
        scope.cancel()
        networkEngine.close()
    }

    fun setAppInForeground(value: Boolean) {
        if (appInForeground == value) return
        appInForeground = value
        val listenersToNotify = synchronized(lifecycleLock) { listeners.toList() }
        scope.launch {
            if (value) {
                runCatching { awaitConnected() }
                    .onFailure { logger.e("unable to restore SDK session in foreground", it) }
            }
            listenersToNotify.forEach { listener ->
                val event = if (value) "app foreground" else "app background"
                notifyListener(event) {
                    if (value) listener.onForeground() else listener.onBackground()
                }
            }
        }
    }

    fun isAppInForeground(): Boolean = appInForeground

    private suspend fun notifyListener(
        event: String,
        notify: suspend () -> Unit,
    ) {
        try {
            notify()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logger.e("unable to notify SDK feature about $event", throwable)
        }
    }

    private data class ConnectedSnapshot(
        val response: CrmConnectResponse,
        val pageContext: SessionPageContext,
        val user: SdkUserPayload,
    )
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CrmRuntimeListener {
    suspend fun onConnected(
        response: CrmConnectResponse,
        pageContext: SessionPageContext,
        user: SdkUserPayload,
    ) = Unit

    suspend fun onDisconnected() = Unit

    suspend fun onForeground() = Unit

    suspend fun onBackground() = Unit

    fun onClosed() = Unit
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CrmConnectResponse(
    val rawBody: String,
    val ts: String? = null,
    val user: SdkConnectedUser = SdkConnectedUser(),
    val session: SdkConnectedSession = SdkConnectedSession(),
    val behaviour: SdkBehaviour? = null,
)

@Serializable
private data class CrmConnectPayload(
    val ts: JsonElement? = null,
    val user: SdkConnectedUserDto = SdkConnectedUserDto(),
    val session: SdkConnectedSessionDto = SdkConnectedSessionDto(),
    val behaviour: SdkBehaviourDto? = null,
)
