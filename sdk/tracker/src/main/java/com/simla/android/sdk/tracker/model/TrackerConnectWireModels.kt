package com.simla.android.sdk.tracker.model

import com.simla.android.sdk.session.SdkBehaviour
import com.simla.android.sdk.session.SdkConnectRequest
import com.simla.android.sdk.session.SdkConnectedSession
import com.simla.android.sdk.session.SdkConnectedUser
import com.simla.android.sdk.tracker.serialization.asFlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class TrackerConnectRequestDto(
    val user: TrackerUserPayloadDto = TrackerUserPayloadDto(),
    @SerialName("page_title") val pageTitle: String? = null,
    val url: String? = null,
    @SerialName("refer_url") val referUrl: String? = null,
)

@Serializable
internal data class TrackerUserPayloadDto(
    val token: String? = null,
    val language: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
internal data class TrackerConnectResponseDto(
    val ts: JsonElement? = null,
    val user: TrackerConnectedUserDto = TrackerConnectedUserDto(),
    val session: TrackerConnectedSessionDto = TrackerConnectedSessionDto(),
    val behaviour: TrackerBehaviourDto = TrackerBehaviourDto(),
)

@Serializable
internal data class TrackerConnectedUserDto(
    val token: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
internal data class TrackerConnectedSessionDto(val id: JsonElement? = null)

@Serializable
internal data class TrackerBehaviourDto(
    @SerialName("is_offline") val isOffline: Boolean = false,
    val widget: Boolean? = null,
    val tracker: Boolean? = null,
    val gpc: Boolean? = null,
    @SerialName("consent_form") val consentForm: Boolean? = null,
)

internal fun SdkConnectRequest.toDto() = TrackerConnectRequestDto(
    user = TrackerUserPayloadDto(
        token = user.token,
        language = user.language,
        name = user.name,
        phone = user.phone,
        email = user.email,
    ),
    pageTitle = pageTitle,
    url = url,
    referUrl = referUrl,
)

internal fun TrackerConnectResponseDto.toDomain() = TrackerConnectResponse(
    ts = ts.asFlexibleString(),
    user = SdkConnectedUser(user.token, user.name, user.email, user.phone),
    session = SdkConnectedSession(session.id.asFlexibleString()),
    behaviour = SdkBehaviour(
        isOffline = behaviour.isOffline,
        widget = behaviour.widget,
        tracker = behaviour.tracker,
        gpc = behaviour.gpc,
        consentForm = behaviour.consentForm,
    ),
)
