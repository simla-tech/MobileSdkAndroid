package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantSendMessageItem(
    val id: String,
    val caption: String? = null,
)
