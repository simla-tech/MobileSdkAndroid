package com.simla.android.sdk.tracker

import android.util.Log

/** Explicit diagnostics at INFO, including outgoing JSON with potentially personal data. */
class DefaultTrackerEventUpdateListener @JvmOverloads constructor(
    private val tag: String = "CrmSdkTracker",
) : TrackerEventUpdateListener {
    override fun onEventUpdate(update: TrackerEventUpdate) {
        update.toLogMessages().forEach { message -> Log.i(tag, message) }
    }
}

internal fun TrackerEventUpdate.toLogMessages(): List<String> {
    val status = when (this) {
        is TrackerEventUpdate.Queued -> "queued"
        is TrackerEventUpdate.Sending -> "sending"
        is TrackerEventUpdate.Delivered -> "delivered"
        is TrackerEventUpdate.RetryScheduled -> "retry_scheduled"
        is TrackerEventUpdate.Rejected -> "rejected"
        is TrackerEventUpdate.Dropped -> "dropped"
    }
    val reason = when (this) {
        is TrackerEventUpdate.RetryScheduled -> " reason=$reason"
        is TrackerEventUpdate.Rejected -> " reason=$reason"
        is TrackerEventUpdate.Dropped -> " reason=$reason"
        else -> ""
    }
    // One line per event keeps large batches from truncating event IDs in Logcat.
    val messages =
        events.map { event -> "$status event={id=${event.id}, type=${event.kind}}$reason" }
            .ifEmpty { listOf("$status events=[]$reason") }
    if (this !is TrackerEventUpdate.Sending) return messages

    // <= 1000 UTF-16 units stays below Logcat's byte limit even for non-ASCII JSON.
    val parts = mutableListOf<String>()
    var start = 0
    while (start < payload.length) {
        var end = (start + 1000).coerceAtMost(payload.length)
        if (end < payload.length && payload[end - 1].isHighSurrogate()) end--
        parts += payload.substring(start, end)
        start = end
    }
    val firstEventId = events.firstOrNull()?.id
    return messages + parts.mapIndexed { index, part ->
        "sending payload[${index + 1}/${parts.size}] firstEventId=$firstEventId: $part"
    }
}
