package com.simla.android.sdk.consultant.models.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantWebSocketEnvelope(
    val type: String? = null,
    val data: JsonElement? = null,
)
