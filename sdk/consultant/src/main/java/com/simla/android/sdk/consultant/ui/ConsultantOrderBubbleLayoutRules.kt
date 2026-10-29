package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantOrderBubbleLayoutRules {
    fun resolve(input: ConsultantOrderBubbleLayoutInput): ConsultantOrderBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val authorWidth = input.authorWidthPx.coerceIn(0, maxWidth)
        val contentWidth = input.orderContentWidthPx.coerceIn(0, maxWidth)
        val lastLineWidth = input.lastLineWidthPx.coerceIn(0, maxWidth)
        val inlineMetaWidth = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val inlineLastLineWidth = lastLineWidth + inlineMetaWidth
        val canPlaceMetaInline = inlineLastLineWidth <= maxWidth
        val orderWidth = if (canPlaceMetaInline) {
            max(contentWidth, inlineLastLineWidth)
        } else {
            max(contentWidth, input.meta.contentWidthPx)
        }.coerceIn(0, maxWidth)

        return ConsultantOrderBubbleLayoutResult(
            bubbleContentWidthPx = max(authorWidth, orderWidth).coerceAtMost(maxWidth),
            orderContentWidthPx = orderWidth,
            authorWidthPx = authorWidth,
            metaPlacement = if (canPlaceMetaInline) {
                ConsultantMessageMetaPlacement.INLINE_OVERLAY
            } else {
                ConsultantMessageMetaPlacement.BELOW_TEXT
            },
        )
    }
}
