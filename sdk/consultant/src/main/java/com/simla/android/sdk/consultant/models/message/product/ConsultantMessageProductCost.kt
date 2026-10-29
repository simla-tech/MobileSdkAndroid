package com.simla.android.sdk.consultant.models.message.product

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageProductCost(
    val value: Double? = null,
    val currency: String? = null,
)
