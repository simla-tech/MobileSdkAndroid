package com.simla.android.sdk.consultant.models.message.order

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageOrderStatus(
    val code: String? = null,
    val name: String? = null,
)
