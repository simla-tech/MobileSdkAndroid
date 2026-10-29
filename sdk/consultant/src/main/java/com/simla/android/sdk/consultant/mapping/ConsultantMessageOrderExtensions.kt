package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.localization.ConsultantStrings
import com.simla.android.sdk.consultant.models.message.order.ConsultantMessageOrder
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderFieldUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderItemUiModel

internal fun ConsultantMessageOrder.toUiItems(): List<OnlineConsultantOrderItemUiModel> {
    val orderItems = items.takeIf { it.isNotEmpty() } ?: products
    return orderItems.mapIndexedNotNull { index, item ->
        val name =
            item.name?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapIndexedNotNull null
        OnlineConsultantOrderItemUiModel(
            numberLabel = "${index + 1}.",
            name = name,
            details = item.formatDetails(),
            url = item.url,
        )
    }
}

internal fun ConsultantMessageOrder.formatOrderNumberLabel(strings: ConsultantStrings): String? {
    val number = numberValue?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    return strings.orderNumber(number)
}

internal fun ConsultantMessageOrder.toUiFields(strings: ConsultantStrings): List<OnlineConsultantOrderFieldUiModel> {
    return buildList {
        (delivery?.address ?: address)?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            add(OnlineConsultantOrderFieldUiModel(title = strings.orderAddress, value = it))
        }
        discount?.formatMoneyLabel()?.let {
            add(OnlineConsultantOrderFieldUiModel(title = strings.orderDiscount, value = it))
        }
        (cost ?: total ?: sum ?: amount)?.formatMoneyLabel()?.let {
            add(OnlineConsultantOrderFieldUiModel(title = strings.orderTotal, value = it))
        }
        formatPaymentLabel()?.let {
            add(OnlineConsultantOrderFieldUiModel(title = strings.orderPayment, value = it))
        }
    }
}

internal fun ConsultantMessageOrder.formatPaymentLabel(): String? {
    val paymentLabels = payments.mapNotNull { it.formatLabel() }
    if (paymentLabels.isNotEmpty()) {
        return paymentLabels.joinToString("\n")
    }

    return listOf(paymentStatus, paymentType, payment)
        .mapNotNull { it?.trim().takeUnless { value -> value.isNullOrEmpty() } }
        .distinct()
        .joinToString(", ")
        .takeUnless { it.isBlank() }
}
