package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object ConsultantImageBubbleLayoutRules {
    fun resolve(input: ConsultantImageBubbleLayoutInput): ConsultantImageBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val maxHeight = input.maxBubbleContentHeightPx.coerceAtLeast(0)
        if (maxWidth == 0 || maxHeight == 0) {
            return ConsultantImageBubbleLayoutResult(
                bubbleContentWidthPx = 0,
                imageWidthPx = 0,
                imageHeightPx = 0,
                metaPlacement = ConsultantMessageMetaPlacement.BELOW_TEXT,
                contentScale = ContentScale.Fit,
            )
        }

        val sourceWidth = input.imageWidthPx.coerceAtLeast(1)
        val sourceHeight = input.imageHeightPx.coerceAtLeast(1)
        val scale = min(
            1f,
            min(
                maxWidth.toFloat() / sourceWidth.toFloat(),
                maxHeight.toFloat() / sourceHeight.toFloat(),
            ),
        )
        var imageWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
        var imageHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
        val minWidth = max(input.authorWidthPx, input.metaWidthPx).coerceAtLeast(1)
        val minHeight = input.metaHeightPx.coerceAtLeast(1)

        if (imageWidth < minWidth) {
            val resizedHeight =
                (imageHeight * (minWidth.toFloat() / imageWidth.toFloat())).roundToInt()
            if (resizedHeight <= maxHeight) {
                imageWidth = minWidth
                imageHeight = resizedHeight.coerceAtLeast(1)
            } else {
                imageWidth = minWidth.coerceAtMost(maxWidth)
                imageHeight = maxHeight
            }
        }

        if (imageHeight < minHeight) {
            val resizedWidth =
                (imageWidth * (minHeight.toFloat() / imageHeight.toFloat())).roundToInt()
            if (resizedWidth <= maxWidth) {
                imageWidth = resizedWidth.coerceAtLeast(1)
                imageHeight = minHeight
            } else {
                imageWidth = maxWidth
                imageHeight = minHeight.coerceAtMost(maxHeight)
            }
        }

        imageWidth = imageWidth.coerceAtMost(maxWidth)
        imageHeight = imageHeight.coerceAtMost(maxHeight)
        val metaPlacement = if (imageHeight >= input.metaHeightPx) {
            ConsultantMessageMetaPlacement.INLINE_OVERLAY
        } else {
            ConsultantMessageMetaPlacement.BELOW_TEXT
        }
        val originalRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
        val bubbleRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val contentScale = if (abs(originalRatio - bubbleRatio) <= IMAGE_ASPECT_RATIO_EPSILON) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        }

        return ConsultantImageBubbleLayoutResult(
            bubbleContentWidthPx = imageWidth,
            imageWidthPx = imageWidth,
            imageHeightPx = imageHeight,
            metaPlacement = metaPlacement,
            contentScale = contentScale,
        )
    }

    private const val IMAGE_ASPECT_RATIO_EPSILON = 0.01f
}
