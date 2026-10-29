package com.simla.android.sdk.consultant.models.message.product

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageProductQuantity(
    val value: Double? = null,
    val unit: String? = null,
)
