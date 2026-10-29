package com.simla.android.sdk.consultant.models.message

import com.simla.android.sdk.consultant.models.message.order.ConsultantMessageOrder
import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProduct
import com.simla.android.sdk.consultant.serialization.asFlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantMessage(
    val id: JsonElement? = null,
    val type: String? = null,
    val content: String? = null,
    val product: ConsultantMessageProduct? = null,
    val order: ConsultantMessageOrder? = null,
    val items: List<ConsultantMessageItem> = emptyList(),
    @SerialName("quote_content")
    val quoteContent: String? = null,
    @SerialName("quote_message_id")
    val quoteMessageId: JsonElement? = null,
    @SerialName("quote_id")
    val quoteId: JsonElement? = null,
    val time: String? = null,
    val status: String? = null,
    val attachments: ConsultantMessageAttachments? = null,
    val user: ConsultantMessageUser? = null,
    @SerialName("from_me")
    val fromMe: Boolean? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString()

    val quoteMessageIdValue: String?
        get() = quoteMessageId.asFlexibleString() ?: quoteId.asFlexibleString()

    val realtimeMarker: String?
        get() = updatedAt ?: time
}
