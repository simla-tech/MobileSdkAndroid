package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.message.order.ConsultantMessageOrderPayment

internal fun ConsultantMessageOrderPayment.formatLabel(): String? {
    val parts = listOfNotNull(
        name?.trim().takeUnless { it.isNullOrEmpty() },
        amount?.formatMoneyLabel(),
        status?.name?.trim().takeUnless { it.isNullOrEmpty() },
    )
    return parts.joinToString(" ").takeUnless { it.isBlank() }
}
