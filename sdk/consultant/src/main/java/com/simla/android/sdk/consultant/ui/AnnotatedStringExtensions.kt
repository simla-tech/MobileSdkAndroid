package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult

internal fun AnnotatedString.hasOrderItemNameLinkAt(
    layout: TextLayoutResult,
    position: Offset,
): Boolean {
    return hasStringAnnotationAt(
        tag = ORDER_ITEM_NAME_LINK_TAG,
        layout = layout,
        position = position,
    )
}

internal fun AnnotatedString.hasProductTitleLinkAt(
    layout: TextLayoutResult,
    position: Offset,
): Boolean {
    return hasStringAnnotationAt(
        tag = PRODUCT_TITLE_LINK_TAG,
        layout = layout,
        position = position,
    )
}

internal fun AnnotatedString.hasStringAnnotationAt(
    tag: String,
    layout: TextLayoutResult,
    position: Offset,
): Boolean {
    val offset = layout.getOffsetForPosition(position)
    return getStringAnnotations(tag = tag, start = offset, end = offset).isNotEmpty()
}
