package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantSendMessageResponse(
    val message: ConsultantSentMessage = ConsultantSentMessage(),
)
