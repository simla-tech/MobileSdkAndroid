package com.simla.android.sdk.consultant.models.connection

import com.simla.android.sdk.consultant.models.message.ConsultantMessage
import com.simla.android.sdk.consultant.serialization.asFlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ConsultantConnectResponse(
    val ts: JsonElement? = null,
    val user: ConsultantConnectedUserDto = ConsultantConnectedUserDto(),
    val session: ConsultantConnectedSessionDto = ConsultantConnectedSessionDto(),
    val behaviour: ConsultantBehaviourDto? = null,
    val config: JsonObject? = null,
    val messages: List<ConsultantMessage> = emptyList(),
    val actions: JsonArray? = null,
    val events: JsonArray? = null,
)

@Serializable
internal data class ConsultantConnectedUserDto(
    val token: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
internal data class ConsultantConnectedSessionDto(
    val id: JsonElement? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString()
}

@Serializable
internal data class ConsultantBehaviourDto(
    @SerialName("is_offline")
    val isOffline: Boolean = false,
    val widget: Boolean? = null,
    val tracker: Boolean? = null,
    val gpc: Boolean? = null,
    @SerialName("consent_form")
    val consentForm: Boolean? = null,
)
