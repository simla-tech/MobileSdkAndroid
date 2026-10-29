package com.simla.android.sdk.consultant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderItemUiModel

@Composable
internal fun quotePrimaryText(message: OnlineConsultantMessageUiModel): String {
    return message.quoteFileName
        ?: message.quoteImageUrl?.let {
            message.quoteText ?: stringResource(R.string.crm_consultant_photo)
        }
        ?: message.quoteText
        ?: stringResource(R.string.crm_consultant_quote)
}

@Composable
internal fun quoteSecondaryText(message: OnlineConsultantMessageUiModel): String? {
    return when {
        message.quoteFileName != null -> {
            message.quoteFileSizeLabel ?: stringResource(R.string.crm_consultant_file)
        }

        message.quoteImageUrl != null -> null
        else -> null
    }
}

internal fun productPriceText(message: OnlineConsultantMessageUiModel): String {
    return message.productPriceLabel?.takeIf { it.isNotBlank() }.orEmpty()
}

internal fun productTitleAnnotatedString(
    title: String,
    titleColor: Color,
    isTitleClickable: Boolean,
    url: String,
): AnnotatedString = buildAnnotatedString {
    if (isTitleClickable) {
        pushStringAnnotation(
            tag = PRODUCT_TITLE_LINK_TAG,
            annotation = url,
        )
    }
    pushStyle(SpanStyle(color = titleColor))
    append(title)
    pop()
    if (isTitleClickable) {
        pop()
    }
}

internal fun orderItemTitleText(item: OnlineConsultantOrderItemUiModel): String {
    return listOf(item.numberLabel, item.name).filter { it.isNotBlank() }.joinToString(" ")
}

internal fun orderItemTitleAnnotatedString(
    item: OnlineConsultantOrderItemUiModel,
    nameColor: Color,
    isNameClickable: Boolean,
): AnnotatedString = buildAnnotatedString {
    if (item.numberLabel.isNotBlank()) {
        append(item.numberLabel)
        append(" ")
    }

    if (isNameClickable) {
        pushStringAnnotation(
            tag = ORDER_ITEM_NAME_LINK_TAG,
            annotation = item.url.orEmpty(),
        )
    }
    pushStyle(
        SpanStyle(
            color = nameColor,
        ),
    )
    append(item.name)
    pop()
    if (isNameClickable) {
        pop()
    }
}

internal fun orderItemNameStartOffset(item: OnlineConsultantOrderItemUiModel): Int {
    return if (item.numberLabel.isNotBlank()) item.numberLabel.length + 1 else 0
}
