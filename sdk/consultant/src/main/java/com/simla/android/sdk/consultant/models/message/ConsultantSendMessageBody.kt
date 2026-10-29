package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantSendMessageBody(
    val type: String,
    val text: String? = null,
    val items: List<ConsultantSendMessageItem>? = null,
)
