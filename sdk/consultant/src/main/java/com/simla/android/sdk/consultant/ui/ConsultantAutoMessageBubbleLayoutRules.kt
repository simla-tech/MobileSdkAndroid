package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantAutoMessageBubbleLayoutRules {
    fun resolve(input: ConsultantAutoMessageBubbleLayoutInput): ConsultantAutoMessageBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val bodyWidth = resolveBodyWidthPx(input.body, maxWidth)
        val widestButtonWidth = input.buttonWidthsPx.maxOrNull() ?: 0
        val baseWidth = maxOf(
            input.authorWidthPx,
            input.titleWidthPx,
            bodyWidth,
            input.imageWidthPx,
            widestButtonWidth,
        ).coerceIn(0, maxWidth)
        val inlineMetaWidth = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val canPlaceMetaInline = input.buttonWidthsPx.isEmpty() &&
                input.body.lastLineWidthPx + inlineMetaWidth <= maxWidth
        val requiredWidth = if (canPlaceMetaInline) {
            max(baseWidth, input.body.lastLineWidthPx + inlineMetaWidth)
        } else {
            max(baseWidth, input.meta.contentWidthPx)
        }.coerceIn(0, maxWidth)

        return ConsultantAutoMessageBubbleLayoutResult(
            bubbleContentWidthPx = requiredWidth,
            bodyWidthPx = bodyWidth,
            metaPlacement = if (canPlaceMetaInline) {
                ConsultantMessageMetaPlacement.INLINE_OVERLAY
            } else {
                ConsultantMessageMetaPlacement.BELOW_TEXT
            },
            metaOverlayOffsetYPx = if (canPlaceMetaInline) {
                input.body.lastLineBaselinePx + (input.meta.heightPx / 2) - input.body.heightPx
            } else {
                0
            },
        )
    }

    private fun resolveBodyWidthPx(
        body: ConsultantMeasuredText,
        maxBubbleContentWidthPx: Int,
    ): Int {
        return if (body.preferredSingleLineWidthPx in 0..maxBubbleContentWidthPx) {
            body.preferredSingleLineWidthPx
        } else {
            body.widestLineWidthPx.coerceAtMost(maxBubbleContentWidthPx)
        }.coerceAtLeast(0)
    }
}
