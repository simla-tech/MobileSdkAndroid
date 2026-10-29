package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantSentMessage(
    val id: String? = null,
    val time: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val status: String? = null,
)
