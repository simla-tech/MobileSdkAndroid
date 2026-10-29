package com.simla.android.sdk.consultant.models.message.product

import com.simla.android.sdk.consultant.serialization.asFlexibleString

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantMessageProduct(
    val id: JsonElement? = null,
    val name: String? = null,
    val article: String? = null,
    val url: String? = null,
    val img: String? = null,
    val cost: ConsultantMessageProductCost? = null,
    val unit: String? = null,
    val quantity: ConsultantMessageProductQuantity? = null,
    val status: String? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString()
}
