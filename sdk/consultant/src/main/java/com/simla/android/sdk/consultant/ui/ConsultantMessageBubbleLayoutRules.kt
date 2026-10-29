package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantMessageBubbleLayoutRules {
    fun resolveStable(
        input: ConsultantMessageBubbleLayoutInput,
        remeasureBody: (contentWidthPx: Int) -> ConsultantMeasuredText,
    ): ConsultantMessageBubbleLayoutResult {
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

    fun resolve(input: ConsultantMessageBubbleLayoutInput): ConsultantMessageBubbleLayoutResult {
        val maxBubbleContentWidthPx = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val bodyWidthPx = resolveBodyWidthPx(input.body, maxBubbleContentWidthPx)
        val authorWidthPx = resolveAuthorWidthPx(input.author, maxBubbleContentWidthPx)
        val baseBubbleWidthPx =
            max(authorWidthPx, bodyWidthPx).coerceAtMost(maxBubbleContentWidthPx)
        val inlineMetaWidthPx = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val canPlaceMetaInline = input.body.lastLineWidthPx + inlineMetaWidthPx <=
                maxBubbleContentWidthPx

        val requiredWidthPx = if (canPlaceMetaInline) {
            max(
                baseBubbleWidthPx,
                input.body.lastLineWidthPx + inlineMetaWidthPx,
            )
        } else {
            max(baseBubbleWidthPx, input.meta.contentWidthPx)
        }

        return ConsultantMessageBubbleLayoutResult(
            bubbleContentWidthPx = minOf(requiredWidthPx, maxBubbleContentWidthPx),
            requiredWidthPx = requiredWidthPx,
            bodyWidthPx = bodyWidthPx,
            authorWidthPx = authorWidthPx,
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

    private fun resolveAuthorWidthPx(
        author: ConsultantMeasuredAuthor,
        maxBubbleContentWidthPx: Int,
    ): Int {
        return when {
            author.preferredSingleLineWidthPx <= maxBubbleContentWidthPx -> {
                author.preferredSingleLineWidthPx
            }

            author.canWrap -> {
                author.layoutWidthPx.coerceAtMost(maxBubbleContentWidthPx)
            }

            else -> {
                maxBubbleContentWidthPx
            }
        }.coerceAtLeast(0)
    }
}
