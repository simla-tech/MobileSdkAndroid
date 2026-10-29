package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantContactFormBubbleLayoutRules {
    fun resolve(input: ConsultantContactFormBubbleLayoutInput): ConsultantContactFormBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val formWidth = max(input.inputWidthPx, input.buttonWidthPx).coerceIn(0, maxWidth)
        val bubbleWidth = max(input.messageWidthPx.coerceAtLeast(0), formWidth)
            .coerceIn(0, maxWidth)

        return ConsultantContactFormBubbleLayoutResult(
            bubbleContentWidthPx = bubbleWidth,
            formContentWidthPx = formWidth.coerceAtMost(bubbleWidth),
        )
    }
}
