package com.simla.android.sdk.consultant.models.realtime

import com.simla.android.sdk.consultant.models.message.ConsultantMessage
import kotlinx.serialization.json.JsonElement

internal sealed interface ConsultantWebSocketEvent {
    data class Connected(
        val code: Int,
        val headers: Map<String, List<String>>,
    ) : ConsultantWebSocketEvent

    data class Event(
        val type: String?,
        val data: JsonElement?,
        val rawText: String,
        val message: ConsultantMessage? = null,
    ) : ConsultantWebSocketEvent

    data class Binary(
        val payload: ByteArray,
    ) : ConsultantWebSocketEvent

    data class Closing(
        val code: Int,
        val reason: String,
    ) : ConsultantWebSocketEvent

    data class Closed(
        val code: Int,
        val reason: String,
    ) : ConsultantWebSocketEvent

    data class Failure(
        val throwable: Throwable,
        val responseCode: Int? = null,
        val responseMessage: String? = null,
    ) : ConsultantWebSocketEvent

    data class Reconnecting(
        val attempt: Int,
        val delayMs: Long,
    ) : ConsultantWebSocketEvent
}
