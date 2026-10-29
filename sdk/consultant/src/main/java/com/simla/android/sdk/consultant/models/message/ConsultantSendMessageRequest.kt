package com.simla.android.sdk.consultant.models.message

import com.simla.android.sdk.session.SdkUserPayload
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantSendMessageRequest(
    val message: ConsultantSendMessageBody,
    val user: ConsultantUserPayloadDto = ConsultantUserPayloadDto(),
)

@Serializable
internal data class ConsultantUserPayloadDto(
    val token: String? = null,
    val language: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

internal fun SdkUserPayload.toDto() = ConsultantUserPayloadDto(
    token = token,
    language = language,
    name = name,
    phone = phone,
    email = email,
)
