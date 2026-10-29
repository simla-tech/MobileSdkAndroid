package com.simla.android.sdk.tracker

import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.serialization.SdkJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

internal object TrackerOutgoingEventAdapter {
    fun serialize(events: List<TrackerEvent>): String {
        return events.joinToString(separator = ",", prefix = "[", postfix = "]") { event ->
            serializeEvent(event)
        }
    }

    fun prepareBatch(
        events: List<TrackerEvent>,
        maxEventCount: Int,
        maxBodyBytes: Int,
    ): TrackerOutgoingBatch {
        require(maxEventCount > 0) { "Tracker batch size must be positive" }
        require(maxBodyBytes >= EMPTY_ARRAY_BYTES) {
            "Tracker request body limit must fit an empty JSON array"
        }

        val selectedEvents = ArrayList<TrackerEvent>(minOf(events.size, maxEventCount))
        val serializedEvents = ArrayList<String>(minOf(events.size, maxEventCount))
        var bodyBytes = EMPTY_ARRAY_BYTES

        for (event in events.take(maxEventCount)) {
            val serializedEvent = serializeEvent(event)
            val separatorBytes = if (selectedEvents.isEmpty()) 0 else COMMA_BYTES
            val nextBodyBytes = bodyBytes + separatorBytes + serializedEvent.utf8Size()
            if (nextBodyBytes > maxBodyBytes) {
                return TrackerOutgoingBatch(
                    events = selectedEvents,
                    body = serializedEvents.joinToString(
                        separator = ",",
                        prefix = "[",
                        postfix = "]",
                    ),
                    oversizedEvent = event.takeIf { selectedEvents.isEmpty() },
                )
            }

            selectedEvents += event
            serializedEvents += serializedEvent
            bodyBytes = nextBodyBytes
        }

        return TrackerOutgoingBatch(
            events = selectedEvents,
            body = serializedEvents.joinToString(separator = ",", prefix = "[", postfix = "]"),
        )
    }

    private fun serializeEvent(event: TrackerEvent): String {
        val fields = SdkJson.encodeToJsonElement(
            TrackerEvent.serializer(),
            event,
        ).jsonObject
        val outgoingEvent = JsonObject(
            fields + (PLATFORM_FIELD to JsonPrimitive(ANDROID_PLATFORM)),
        )
        return SdkJson.encodeToString(JsonObject.serializer(), outgoingEvent)
    }

    private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

    private const val PLATFORM_FIELD = "platform"
    private const val ANDROID_PLATFORM = "android"
    private const val EMPTY_ARRAY_BYTES = 2
    private const val COMMA_BYTES = 1
}

internal data class TrackerOutgoingBatch(
    val events: List<TrackerEvent>,
    val body: String,
    val oversizedEvent: TrackerEvent? = null,
)
