package com.simla.android.sdk.consultant.models.message.order

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageOrderPaymentStatus(
    val name: String? = null,
    val payed: Boolean? = null,
)
