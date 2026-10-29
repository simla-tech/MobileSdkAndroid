package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantProductBubbleLayoutRules {
    fun resolve(input: ConsultantProductBubbleLayoutInput): ConsultantProductBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val authorWidth = input.authorWidthPx.coerceIn(0, maxWidth)
        val titleWidth = input.productTitleWidthPx.coerceIn(0, maxWidth)
        val infoWidth = input.productInfoWidthPx.coerceIn(0, maxWidth)
        val imageWidth = input.productImageWidthPx.coerceIn(0, maxWidth)
        val imageHeight = input.productImageHeightPx.coerceAtLeast(0)
        val inlineMetaWidth = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val inlinePriceLineWidth = infoWidth + inlineMetaWidth
        val canPlaceMetaInline = inlinePriceLineWidth <= maxWidth
        val productWidth = if (canPlaceMetaInline) {
            max(max(titleWidth, imageWidth), inlinePriceLineWidth)
        } else {
            max(max(max(titleWidth, infoWidth), imageWidth), input.meta.contentWidthPx)
        }.coerceIn(0, maxWidth)

        return ConsultantProductBubbleLayoutResult(
            bubbleContentWidthPx = max(authorWidth, productWidth).coerceAtMost(maxWidth),
            productContentWidthPx = productWidth,
            productImageWidthPx = imageWidth,
            productImageHeightPx = imageHeight,
            authorWidthPx = authorWidth,
            metaPlacement = if (canPlaceMetaInline) {
                ConsultantMessageMetaPlacement.INLINE_OVERLAY
            } else {
                ConsultantMessageMetaPlacement.BELOW_TEXT
            },
        )
    }
}
