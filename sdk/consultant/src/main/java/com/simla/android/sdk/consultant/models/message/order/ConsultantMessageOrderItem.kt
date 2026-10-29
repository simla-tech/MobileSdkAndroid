package com.simla.android.sdk.consultant.models.message.order

import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductCost
import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductQuantity
import com.simla.android.sdk.consultant.serialization.asFlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantMessageOrderItem(
    val id: JsonElement? = null,
    @SerialName("external_id")
    val externalId: JsonElement? = null,
    val name: String? = null,
    val article: String? = null,
    val url: String? = null,
    val img: String? = null,
    val unit: String? = null,
    val cost: ConsultantMessageProductCost? = null,
    val price: ConsultantMessageProductCost? = null,
    val quantity: ConsultantMessageProductQuantity? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString() ?: externalId.asFlexibleString()
}
