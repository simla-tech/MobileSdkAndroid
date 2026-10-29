package com.simla.android.sdk.consultant.models.message.order

import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductCost
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageOrderDelivery(
    val address: String? = null,
    val comment: String? = null,
    val name: String? = null,
    val price: ConsultantMessageProductCost? = null,
)
