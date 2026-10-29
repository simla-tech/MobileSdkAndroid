package com.simla.android.sdk.tracker

import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.retryAfterMsOrNull
import com.simla.android.sdk.network.withQueryParameters
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.tracker.model.TrackerCustomerIdentity
import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.serialization.SdkJson
import com.simla.android.sdk.tracker.storage.IncrementalTrackerStorage
import com.simla.android.sdk.tracker.storage.TrackerStorage
import com.simla.android.sdk.util.toIso8601EpochMillis
import com.simla.android.sdk.util.toIso8601Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

internal class ConsultantTrackerDispatcher(
    private val config: TrackerRuntimeConfig,
    private val logger: SdkLogger,
    private val networkEngine: SdkHttpNetworkEngine,
    private val trackerStorage: TrackerStorage,
    private val sdkScope: CoroutineScope,
    private val sessionProvider: suspend () -> SdkSession?,
    private val authorizedHeadersProvider: suspend () -> Map<String, String>,
    private val customerIdentityProvider: () -> TrackerCustomerIdentity,
    private val endpointResolver: (String) -> String,
    private val currentPageContextProvider: suspend () -> SessionPageContext?,
    private val observer: ConsultantTrackerObserver,
) {
    private val mutex = Mutex()
    private val flushMutex = Mutex()
    private val dispatchMutex = Mutex()
    private var pendingEvents = mutableListOf<TrackerEvent>()
    private var timeSyncState: TrackerTimeSyncState? = null
    private var rateLimitUntilMs = 0L
    private var periodicFlushJob: Job? = null
    private var startupEvents = mutableListOf<TrackerEvent>()

    @Volatile
    private var dispatchState = DispatchState.AWAITING_CONNECTION

    init {
        restoreState()
        observer.onQueueSizeChanged(pendingEvents.size)
    }

    suspend fun onConnect(
        serverTs: String?,
        trackerEnabled: Boolean = true,
        awaitEnablement: Boolean = false,
    ) = dispatchMutex.withLock {
        if (dispatchState == DispatchState.CLOSED) {
            return@withLock
        }
        dispatchState = DispatchState.AWAITING_CONNECTION
        periodicFlushJob?.cancel()
        periodicFlushJob = null

        var bufferedEvents = emptyList<TrackerEvent>()
        flushMutex.withLock {
            val session = sessionProvider()
            var scopeChanged = false
            mutex.withLock {
                scopeChanged = if (
                    session != null
                ) {
                    trackerStorage.activateScope(session.siteToken, session.userToken)
                } else {
                    false
                }
                if (scopeChanged) {
                    withContext(Dispatchers.IO) { restoreState() }
                    rateLimitUntilMs = 0L
                }
            }
            if (scopeChanged) {
                observer.onQueueSizeChanged(pendingEvents.size)
                observer.onRateLimited(null)
            }
            if (!serverTs.isNullOrBlank()) {
                setTimeSyncState(serverTs)
            }
            mutex.withLock {
                dispatchState = when {
                    trackerEnabled -> DispatchState.ENABLED
                    awaitEnablement -> DispatchState.AWAITING_CONNECTION
                    else -> DispatchState.DISABLED
                }
                if (!awaitEnablement) {
                    bufferedEvents = startupEvents.toList()
                    startupEvents.clear()
                }
            }
            if (trackerEnabled) {
                periodicFlushJob = sdkScope.launch {
                    while (isActive) {
                        delay(config.trackerFlushIntervalMs)
                        flush()
                    }
                }
            }
        }

        when {
            trackerEnabled && bufferedEvents.isNotEmpty() -> {
                val pageContext = currentPageContextProvider()
                val normalizedEvents = bufferedEvents.map { normalizeEvent(it, pageContext) }
                enqueueEnabledEvents(normalizedEvents)
            }

            !trackerEnabled && bufferedEvents.isNotEmpty() -> {
                val pageContext = currentPageContextProvider()
                val droppedEvents = bufferedEvents.map { normalizeEvent(it, pageContext) }
                observer.onQueueSizeChanged(mutex.withLock { pendingEvents.size })
                observer.onEventUpdate(
                    TrackerEventUpdate.Dropped(
                        droppedEvents.toReferences(),
                        "Tracker is disabled",
                    ),
                )
            }
        }
    }

    suspend fun track(events: List<TrackerEvent>) {
        if (events.isEmpty()) {
            return
        }

        dispatchMutex.withLock {
            when (dispatchState) {
                DispatchState.AWAITING_CONNECTION -> {
                    bufferStartupEvents(events)
                }

                DispatchState.DISABLED,
                DispatchState.CLOSED,
                    -> Unit

                DispatchState.ENABLED -> {
                    val pageContext = currentPageContextProvider()
                    val normalizedEvents = events.map { normalizeEvent(it, pageContext) }
                    enqueueEnabledEvents(normalizedEvents)
                }
            }
        }
    }

    private suspend fun bufferStartupEvents(events: List<TrackerEvent>) {
        var queueSize = 0
        var droppedEvents = emptyList<TrackerEvent>()
        mutex.withLock {
            startupEvents.addAll(events)
            val overflow = (startupEvents.size - config.trackerMaxQueueSize).coerceAtLeast(0)
            if (overflow > 0) {
                droppedEvents = startupEvents.take(overflow)
                startupEvents = startupEvents.drop(overflow).toMutableList()
            }
            queueSize = pendingEvents.size + startupEvents.size
        }

        observer.onQueueSizeChanged(queueSize)
        if (droppedEvents.isNotEmpty()) {
            val pageContext = currentPageContextProvider()
            val normalizedDroppedEvents = droppedEvents.map { normalizeEvent(it, pageContext) }
            observer.onEventUpdate(
                TrackerEventUpdate.Dropped(
                    normalizedDroppedEvents.toReferences(),
                    "Tracker startup buffer capacity exceeded",
                ),
            )
        }
    }

    private suspend fun enqueueEnabledEvents(normalizedEvents: List<TrackerEvent>) {
        var shouldFlushNow = false
        var queueSize = 0
        val droppedEvents = mutableListOf<TrackerEvent>()

        mutex.withLock {
            val merged =
                LinkedHashMap<String, TrackerEvent>(pendingEvents.size + normalizedEvents.size)
            pendingEvents.forEach { event ->
                event.id?.let { merged[it] = event }
            }
            normalizedEvents.forEach { event ->
                event.id?.let { merged[it] = event }
            }

            val mergedEvents = merged.values.toList()
            val trimmed = mergedEvents.takeLast(config.trackerMaxQueueSize)
            droppedEvents += mergedEvents.dropLast(trimmed.size)
            val retainedIds = trimmed.mapNotNullTo(mutableSetOf()) { it.id }
            val upsertedEvents = normalizedEvents
                .associateBy { it.id }
                .values
                .filter { it.id in retainedIds }
            val removedEventIds = droppedEvents.mapNotNullTo(mutableSetOf()) { it.id }
            persistPendingEventsLocked(trimmed, upsertedEvents, removedEventIds)
            pendingEvents = trimmed.toMutableList()
            shouldFlushNow = pendingEvents.size >= config.trackerBatchSize
            queueSize = pendingEvents.size
        }

        observer.onQueueSizeChanged(queueSize)
        observer.onEventUpdate(TrackerEventUpdate.Queued(normalizedEvents.toReferences()))
        if (droppedEvents.isNotEmpty()) {
            observer.onEventUpdate(
                TrackerEventUpdate.Dropped(
                    droppedEvents.toReferences(),
                    "Tracker queue capacity exceeded",
                ),
            )
        }

        if (shouldFlushNow) {
            sdkScope.launch(start = CoroutineStart.UNDISPATCHED) {
                flush()
            }
        }
    }

    suspend fun flush() = flushMutex.withLock {
        if (dispatchState != DispatchState.ENABLED || isRateLimited()) {
            return@withLock
        }

        refreshTimeSyncIfNeeded()
        var maxBatchCount = config.trackerBatchSize.coerceAtLeast(1)
        while (true) {
            val candidates = mutex.withLock {
                pendingEvents.take(maxBatchCount)
            }

            if (candidates.isEmpty()) {
                return@withLock
            }

            val session = sessionProvider() ?: return@withLock
            val customerIdentity = customerIdentityProvider()
            val flushableCandidates = candidates.map { event ->
                event.copy(
                    systemCustomerId = event.systemCustomerId?.takeIf { it > 0 }
                        ?: customerIdentity.systemId,
                    systemCustomerExternalId = event.systemCustomerExternalId
                        ?.takeIf { it.isNotBlank() }
                        ?: customerIdentity.siteCustomerId,
                )
            }
            val outgoingBatch = TrackerOutgoingEventAdapter.prepareBatch(
                events = flushableCandidates,
                maxEventCount = maxBatchCount,
                maxBodyBytes = MAX_REQUEST_BODY_BYTES,
            )
            if (outgoingBatch.oversizedEvent != null) {
                val oversizedEvent = candidates.first()
                rejectBatch(listOf(oversizedEvent), OVERSIZED_EVENT_REASON)
                continue
            }

            val batch = candidates.take(outgoingBatch.events.size)
            val flushableBatch = outgoingBatch.events
            val requestBody = outgoingBatch.body
            val customerHeaders = customerIdentity.siteCustomerId
                ?.let { mapOf("X-Site-Customer-Id" to it) }
                .orEmpty()
            val response = runCatching {
                observer.onEventUpdate(
                    TrackerEventUpdate.Sending(
                        flushableBatch.toReferences(),
                        payload = requestBody
                    ),
                )
                networkEngine.postJson(
                    SdkHttpRequest(
                        url = collectorEndpoint("event", session),
                        headers = authorizedHeadersProvider() + customerHeaders,
                        body = requestBody,
                    ),
                )
            }.onFailure { throwable ->
                logger.e("tracker flush failed", throwable)
                val message = throwable.message ?: throwable::class.java.simpleName
                observer.onError(message)
                observer.onEventUpdate(
                    TrackerEventUpdate.RetryScheduled(batch.toReferences(), message),
                )
            }.getOrNull() ?: return@withLock

            if (response.code == HTTP_PAYLOAD_TOO_LARGE) {
                val message = "HTTP $HTTP_PAYLOAD_TOO_LARGE: tracker request body is too large"
                observer.onError(message)
                if (batch.size == 1) {
                    rejectBatch(batch, message)
                } else {
                    maxBatchCount = maxOf(1, batch.size / 2)
                }
                continue
            }

            val retryAfterMs = response.retryAfterMsOrNull()
            if (response.code == 429) {
                if (retryAfterMs != null) {
                    applyRateLimit(retryAfterMs)
                    val untilEpochMs = System.currentTimeMillis() + retryAfterMs
                    observer.onRateLimited(untilEpochMs)
                }
                val message = if (retryAfterMs != null) {
                    "HTTP 429; retry after ${retryAfterMs}ms"
                } else {
                    "HTTP 429"
                }
                observer.onError(message)
                observer.onEventUpdate(
                    TrackerEventUpdate.RetryScheduled(batch.toReferences(), message),
                )
                return@withLock
            }

            if (!response.isSuccessful) {
                val backendError = parseTopLevelError(response.body)
                val message = buildString {
                    append("HTTP ${response.code}")
                    if (!backendError.isNullOrBlank()) {
                        append(": $backendError")
                    }
                }
                observer.onError(message)
                logger.d("tracker flush postponed: $message")
                observer.onEventUpdate(
                    TrackerEventUpdate.RetryScheduled(batch.toReferences(), message),
                )
                return@withLock
            }

            val attemptedIds = batch.mapNotNull { it.id }.toSet()
            val failures = parseEventFailures(response.body, attemptedIds)
            val retryableIds = failures.retryableIds
            val rejectedIds = failures.rejectedReasons.keys
            val deliveredIds = attemptedIds - retryableIds - rejectedIds
            var remainingQueueSize = 0

            mutex.withLock {
                val remainingEvents = pendingEvents.filterNot { event ->
                    val eventId = event.id ?: return@filterNot false
                    deliveredIds.contains(eventId) || rejectedIds.contains(eventId)
                }
                persistPendingEventsLocked(
                    pendingSnapshot = remainingEvents,
                    upsertedEvents = emptyList(),
                    removedEventIds = deliveredIds + rejectedIds,
                )
                pendingEvents = remainingEvents.toMutableList()
                remainingQueueSize = pendingEvents.size
            }

            observer.onQueueSizeChanged(remainingQueueSize)
            val deliveredEvents = batch.filter { it.id in deliveredIds }
            if (deliveredEvents.isNotEmpty()) {
                observer.onEventUpdate(
                    TrackerEventUpdate.Delivered(deliveredEvents.toReferences()),
                )
                observer.onFlushSucceeded(System.currentTimeMillis())
            }
            failures.rejectedReasons.forEach { (eventId, reason) ->
                val rejectedEvent = batch.firstOrNull { it.id == eventId }
                    ?: return@forEach
                observer.onEventUpdate(
                    TrackerEventUpdate.Rejected(
                        event = rejectedEvent.toReference(),
                        reason = reason,
                    ),
                )
            }
            if (retryableIds.isNotEmpty()) {
                val retryableEvents = batch.filter { it.id in retryableIds }
                observer.onEventUpdate(
                    TrackerEventUpdate.RetryScheduled(
                        retryableEvents.toReferences(),
                        "Backend requested retry",
                    ),
                )
            }

            logger.d("tracker flushed ${deliveredIds.size} events for session ${session.sessionId}")
            return@withLock
        }
    }

    suspend fun clear() = dispatchMutex.withLock {
        val (clearedEvents, clearedStartupEvents) = mutex.withLock {
            val events = pendingEvents.toList()
            val bufferedEvents = startupEvents.toList()
            pendingEvents.clear()
            startupEvents.clear()
            timeSyncState = null
            rateLimitUntilMs = 0L
            withContext(Dispatchers.IO) { trackerStorage.clear() }
            events to bufferedEvents
        }
        observer.onQueueSizeChanged(0)
        observer.onRateLimited(null)
        if (clearedEvents.isNotEmpty()) {
            observer.onEventUpdate(
                TrackerEventUpdate.Dropped(
                    clearedEvents.toReferences(),
                    "Tracker queue cleared"
                )
            )
        }
        if (clearedStartupEvents.isNotEmpty()) {
            val pageContext = currentPageContextProvider()
            val normalizedEvents = clearedStartupEvents.map { normalizeEvent(it, pageContext) }
            observer.onEventUpdate(
                TrackerEventUpdate.Dropped(
                    normalizedEvents.toReferences(),
                    "Tracker startup buffer cleared",
                ),
            )
        }
    }

    fun close() {
        dispatchState = DispatchState.CLOSED
        periodicFlushJob?.cancel()
    }

    private suspend fun refreshTimeSyncIfNeeded() {
        if (isRateLimited()) {
            return
        }

        val shouldSync = mutex.withLock {
            val state = timeSyncState ?: return@withLock true
            val ageMs = System.currentTimeMillis() - state.syncedAtEpochMs
            ageMs >= config.trackerTimeSyncIntervalMs
        }

        if (!shouldSync) {
            return
        }

        val response = runCatching {
            val session = sessionProvider() ?: return
            networkEngine.getJson(
                SdkHttpRequest(
                    url = collectorEndpoint("ts", session),
                    headers = authorizedHeadersProvider(),
                ),
            )
        }.onFailure { throwable ->
            logger.e("tracker time sync failed", throwable)
        }.getOrNull() ?: return

        val retryAfterMs = response.retryAfterMsOrNull()
        if (response.code == 429 && retryAfterMs != null) {
            applyRateLimit(retryAfterMs)
            return
        }

        if (!response.isSuccessful) {
            return
        }

        val serverTs = parseServerTs(response.body) ?: return
        setTimeSyncState(serverTs)
    }

    private suspend fun setTimeSyncState(serverTs: String) {
        mutex.withLock {
            timeSyncState = TrackerTimeSyncState(
                serverTimestamp = serverTs,
                syncedAtEpochMs = System.currentTimeMillis(),
            )
            persistTimeSyncStateLocked()
        }
    }

    private suspend fun normalizeEvent(
        event: TrackerEvent,
        pageContext: SessionPageContext?,
    ): TrackerEvent {
        return event.copy(
            id = event.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            pageUrl = event.pageUrl ?: pageContext?.url.orEmpty(),
            pageTitle = event.pageTitle ?: pageContext?.title.orEmpty(),
            referrerUrl = event.referrerUrl ?: pageContext?.referUrl.orEmpty(),
            timestamp = event.timestamp ?: resolveTimestamp(),
            data = normalizeEventData(event.data),
        )
    }

    private suspend fun resolveTimestamp(): String {
        val state = mutex.withLock { timeSyncState }
        if (state == null) {
            return System.currentTimeMillis().toIso8601Timestamp()
        }

        val syncedAt = state.syncedAtEpochMs
        val baseTimestampMs = state.serverTimestamp.toIso8601EpochMillis()
            ?: return System.currentTimeMillis().toIso8601Timestamp()
        val driftMs = (System.currentTimeMillis() - syncedAt).coerceAtLeast(0L)
        return (baseTimestampMs + driftMs).toIso8601Timestamp()
    }

    private fun normalizeEventData(data: JsonObject?): JsonObject? {
        return data?.let { JsonObject(it.toMap()) }
    }

    private fun parseServerTs(body: String): String? {
        if (body.isBlank()) {
            return null
        }

        return runCatching {
            SdkJson.parseToJsonElement(body).jsonObject["ts"]?.let { element ->
                when {
                    element.toString().startsWith('"') -> element.toString().trim('"')
                    else -> element.toString()
                }
            }
        }.getOrNull()
    }

    private fun parseEventFailures(body: String, attemptedIds: Set<String>): EventFailures {
        if (body.isBlank()) {
            return EventFailures(emptySet(), emptyMap())
        }

        return runCatching {
            val json = SdkJson.parseToJsonElement(body).jsonObject
            val topLevelError = json["error"]?.toString()?.trim('"')?.lowercase()
            if (topLevelError != null) {
                return@runCatching if (topLevelError == TRY_AGAIN_LATER) {
                    EventFailures(attemptedIds, emptyMap())
                } else {
                    EventFailures(emptySet(), attemptedIds.associateWith { topLevelError })
                }
            }

            val retryableIds = linkedSetOf<String>()
            val rejectedReasons = linkedMapOf<String, String>()
            json.forEach { (eventId, value) ->
                if (!attemptedIds.contains(eventId)) {
                    return@forEach
                }
                val errors = runCatching {
                    value.jsonObject.map { (field, problem) ->
                        "${field}: ${problem.toString().trim('"')}"
                    }
                }.getOrElse { listOf(value.toString()) }
                val isRetryable = errors.any { error ->
                    error.substringAfter(": ").lowercase() == TRY_AGAIN_LATER
                }
                if (isRetryable) {
                    retryableIds += eventId
                } else {
                    rejectedReasons[eventId] = errors.joinToString()
                }
            }
            EventFailures(retryableIds, rejectedReasons)
        }.getOrElse {
            EventFailures(emptySet(), emptyMap())
        }
    }

    private fun parseTopLevelError(body: String): String? {
        if (body.isBlank()) {
            return null
        }

        return runCatching {
            val response = SdkJson.parseToJsonElement(body).jsonObject
            response["error"]?.jsonPrimitive?.contentOrNull
                ?: response["errors"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString()
        }.getOrNull()
    }

    private fun collectorEndpoint(path: String, session: SdkSession): String =
        endpointResolver(path).withQueryParameters(
            "user" to session.userToken,
            "session" to session.sessionId,
        )

    private suspend fun rejectBatch(batch: List<TrackerEvent>, reason: String) {
        val rejectedIds = batch.mapNotNull { it.id }.toSet()
        val remainingQueueSize = mutex.withLock {
            val remainingEvents = pendingEvents.filterNot { it.id in rejectedIds }
            persistPendingEventsLocked(
                pendingSnapshot = remainingEvents,
                upsertedEvents = emptyList(),
                removedEventIds = rejectedIds,
            )
            pendingEvents = remainingEvents.toMutableList()
            pendingEvents.size
        }
        observer.onQueueSizeChanged(remainingQueueSize)
        batch.forEach { event ->
            observer.onEventUpdate(TrackerEventUpdate.Rejected(event.toReference(), reason))
        }
    }

    private data class EventFailures(
        val retryableIds: Set<String>,
        val rejectedReasons: Map<String, String>,
    )

    private enum class DispatchState {
        AWAITING_CONNECTION,
        ENABLED,
        DISABLED,
        CLOSED,
    }

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitUntilMs
    }

    private fun applyRateLimit(retryAfterMs: Long) {
        rateLimitUntilMs = maxOf(rateLimitUntilMs, System.currentTimeMillis() + retryAfterMs)
    }

    private fun restoreState() {
        pendingEvents = runCatching {
            trackerStorage.getPendingEvents()
                ?.takeIf { it.isNotBlank() }
                ?.let { SdkJson.decodeFromString<List<TrackerEvent>>(it) }
                ?.toMutableList()
                ?: mutableListOf()
        }.getOrElse {
            logger.e("unable to restore tracker queue", it)
            mutableListOf()
        }

        timeSyncState = runCatching {
            trackerStorage.getTimeSyncState()
                ?.takeIf { it.isNotBlank() }
                ?.let { SdkJson.decodeFromString<TrackerTimeSyncState>(it) }
        }.getOrElse {
            logger.e("unable to restore tracker time sync state", it)
            null
        }
    }

    private suspend fun persistPendingEventsLocked(
        pendingSnapshot: List<TrackerEvent>,
        upsertedEvents: List<TrackerEvent>,
        removedEventIds: Set<String>,
    ) {
        if (pendingSnapshot.isEmpty()) {
            withContext(Dispatchers.IO) { trackerStorage.setPendingEvents(null) }
            return
        }

        withContext(Dispatchers.IO) {
            val persistentEvents = upsertedEvents.map { event -> event.withoutUtm() }
            val incrementalStorage = trackerStorage as? IncrementalTrackerStorage
            if (incrementalStorage != null) {
                incrementalStorage.updatePendingEvents(persistentEvents, removedEventIds)
            } else {
                trackerStorage.setPendingEvents(
                    SdkJson.encodeToString(pendingSnapshot.map { event -> event.withoutUtm() }),
                )
            }
        }
    }

    private fun TrackerEvent.withoutUtm(): TrackerEvent =
        if (utm == null) this else copy(utm = null)

    private suspend fun persistTimeSyncStateLocked() {
        val state = timeSyncState
        if (state == null) {
            withContext(Dispatchers.IO) { trackerStorage.setTimeSyncState(null) }
            return
        }

        val encodedState = SdkJson.encodeToString(state)
        withContext(Dispatchers.IO) { trackerStorage.setTimeSyncState(encodedState) }
    }

    @Serializable
    private data class TrackerTimeSyncState(
        val serverTimestamp: String,
        val syncedAtEpochMs: Long,
    )

    private companion object {
        private const val TRY_AGAIN_LATER = "try again later"
        private const val HTTP_PAYLOAD_TOO_LARGE = 413
        private const val MAX_REQUEST_BODY_BYTES = 1024 * 1024 - 16 * 1024
        private const val OVERSIZED_EVENT_REASON =
            "Tracker event exceeds the maximum request body size"
    }
}
