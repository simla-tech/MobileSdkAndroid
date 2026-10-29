package com.simla.android.sdk.serialization

import com.simla.android.sdk.session.SdkBehaviour
import com.simla.android.sdk.session.SdkConnectRequest
import com.simla.android.sdk.session.SdkConnectedSession
import com.simla.android.sdk.session.SdkConnectedUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class SdkConnectRequestDto(
    val user: SdkUserPayloadDto = SdkUserPayloadDto(),
    @SerialName("page_title")
    val pageTitle: String? = null,
    val url: String? = null,
    @SerialName("refer_url")
    val referUrl: String? = null,
)

@Serializable
internal data class SdkUserPayloadDto(
    val token: String? = null,
    val language: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
internal data class SdkConnectedUserDto(
    val token: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
internal data class SdkConnectedSessionDto(
    val id: JsonElement? = null,
)

@Serializable
internal data class SdkBehaviourDto(
    @SerialName("is_offline")
    val isOffline: Boolean = false,
    val widget: Boolean? = null,
    val tracker: Boolean? = null,
    val gpc: Boolean? = null,
    @SerialName("consent_form")
    val consentForm: Boolean? = null,
)

internal fun SdkConnectRequest.toDto() = SdkConnectRequestDto(
    user = SdkUserPayloadDto(
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

internal fun SdkConnectedUserDto.toDomain() = SdkConnectedUser(
    token = token,
    name = name,
    email = email,
    phone = phone,
)

internal fun SdkConnectedSessionDto.toDomain() = SdkConnectedSession(
    id = id.asFlexibleString(),
)

internal fun SdkBehaviourDto.toDomain() = SdkBehaviour(
    isOffline = isOffline,
    widget = widget,
    tracker = tracker,
    gpc = gpc,
    consentForm = consentForm,
)
