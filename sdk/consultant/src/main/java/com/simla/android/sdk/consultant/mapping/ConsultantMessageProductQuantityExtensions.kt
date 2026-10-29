package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.localization.ConsultantStrings
import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductQuantity
import java.util.Locale

internal fun ConsultantMessageProductQuantity.formatQuantityLabel(
    defaultUnit: String?,
    strings: ConsultantStrings,
): String? {
    val amount = value ?: return null
    val unitLabel = unit?.takeIf { it.isNotBlank() } ?: defaultUnit?.takeIf { it.isNotBlank() }
    val amountLabel = if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        "%.2f".format(Locale.US, amount).trimEnd('0').trimEnd('.')
    }
    return if (amount > 0.0) {
        listOf(strings.inStock, amountLabel, unitLabel).filterNotNull().joinToString(" ")
    } else {
        strings.outOfStock
    }
}

internal fun ConsultantMessageProductQuantity.formatOrderQuantityLabel(defaultUnit: String?): String? {
    val amount = value ?: return null
    val unitLabel = unit?.takeIf { it.isNotBlank() } ?: defaultUnit?.takeIf { it.isNotBlank() }
    val amountLabel = if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        "%.2f".format(Locale.US, amount).trimEnd('0').trimEnd('.')
    }
    return listOf(amountLabel, unitLabel).filterNotNull().joinToString(" ")
}
