package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.message.product.ConsultantMessageProductCost
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal fun ConsultantMessageProductCost.formatMoneyLabel(): String? {
    val amount = value ?: return null
    val currencyCode = currency?.takeIf { it.isNotBlank() }
    return runCatching {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            if (currencyCode != null) {
                this.currency = Currency.getInstance(currencyCode)
            }
        }.format(amount)
    }.getOrElse {
        val suffix = currencyCode?.let { " $it" }.orEmpty()
        "%,.2f%s".format(Locale.US, amount, suffix)
    }
}
