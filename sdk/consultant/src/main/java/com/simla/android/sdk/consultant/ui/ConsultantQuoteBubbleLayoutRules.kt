package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantQuoteBubbleLayoutRules {
    fun resolveStable(
        input: ConsultantQuoteBubbleLayoutInput,
        remeasureBody: (contentWidthPx: Int) -> ConsultantMeasuredText,
    ): ConsultantQuoteBubbleLayoutResult {
        var currentInput = input
        var currentResult = resolve(currentInput)

        repeat(4) {
            val remeasuredBody = remeasureBody(currentResult.bubbleContentWidthPx)
            val nextInput = currentInput.copy(body = remeasuredBody)
            val nextResult = resolve(nextInput)
            if (nextResult == currentResult) {
                return nextResult
            }

            currentInput = nextInput
            currentResult = nextResult
        }

        return currentResult
    }

    fun resolve(input: ConsultantQuoteBubbleLayoutInput): ConsultantQuoteBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val authorWidth = input.authorWidthPx.coerceIn(0, maxWidth)
        val bodyWidth = resolveBodyWidthPx(input.body, maxWidth)
        val quoteWidth = input.quoteContentWidthPx.coerceIn(0, maxWidth)
        val inlineMetaWidth = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val canPlaceMetaInline = input.body.lastLineWidthPx + inlineMetaWidth <= maxWidth
        val requiredBodyWidth = if (canPlaceMetaInline) {
            max(bodyWidth, input.body.lastLineWidthPx + inlineMetaWidth)
        } else {
            max(bodyWidth, input.meta.contentWidthPx)
        }.coerceIn(0, maxWidth)
        val bubbleWidth = max(authorWidth, max(quoteWidth, requiredBodyWidth))
            .coerceAtMost(maxWidth)

        return ConsultantQuoteBubbleLayoutResult(
            bubbleContentWidthPx = bubbleWidth,
            bodyWidthPx = requiredBodyWidth,
            quoteContentWidthPx = bubbleWidth,
            authorWidthPx = authorWidth,
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
