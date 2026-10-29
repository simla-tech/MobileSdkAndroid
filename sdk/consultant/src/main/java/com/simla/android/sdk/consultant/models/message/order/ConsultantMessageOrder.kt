package com.simla.android.sdk.consultant.models.message.order

import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductCost
import com.simla.android.sdk.consultant.serialization.asFlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantMessageOrder(
    val id: JsonElement? = null,
    @SerialName("external_id")
    val externalId: JsonElement? = null,
    val number: JsonElement? = null,
    val url: String? = null,
    val cost: ConsultantMessageProductCost? = null,
    val items: List<ConsultantMessageOrderItem> = emptyList(),
    val products: List<ConsultantMessageOrderItem> = emptyList(),
    val address: String? = null,
    val delivery: ConsultantMessageOrderDelivery? = null,
    val discount: ConsultantMessageProductCost? = null,
    val total: ConsultantMessageProductCost? = null,
    val sum: ConsultantMessageProductCost? = null,
    val amount: ConsultantMessageProductCost? = null,
    val payments: List<ConsultantMessageOrderPayment> = emptyList(),
    val status: ConsultantMessageOrderStatus? = null,
    val date: String? = null,
    val payment: String? = null,
    @SerialName("payment_status")
    val paymentStatus: String? = null,
    @SerialName("payment_type")
    val paymentType: String? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString() ?: externalId.asFlexibleString()

    val numberValue: String?
        get() = number.asFlexibleString()
}
