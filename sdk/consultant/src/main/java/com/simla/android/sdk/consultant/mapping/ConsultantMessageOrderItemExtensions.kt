package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.message.order.ConsultantMessageOrderItem

internal fun ConsultantMessageOrderItem.formatDetails(): String {
    val quantityLabel = quantity?.formatOrderQuantityLabel(unit)
    val priceLabel = (price ?: cost)?.formatMoneyLabel()
    return listOfNotNull(quantityLabel, priceLabel?.let { "x $it" })
        .joinToString(" ")
        .ifBlank { article?.trim().orEmpty() }
}
