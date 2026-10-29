package com.simla.android.sdk.consultant.models.message.order

import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductCost
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageOrderPayment(
    val amount: ConsultantMessageProductCost? = null,
    val name: String? = null,
    val status: ConsultantMessageOrderPaymentStatus? = null,
)
