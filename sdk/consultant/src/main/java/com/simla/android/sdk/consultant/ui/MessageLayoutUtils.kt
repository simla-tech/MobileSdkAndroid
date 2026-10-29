package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import kotlin.math.roundToInt

internal fun composeTextDirectionFor(text: String): TextDirection {
    return when (bubbleTextDirectionFor(text)) {
        ConsultantMessageTextDirection.LTR -> TextDirection.ContentOrLtr
        ConsultantMessageTextDirection.RTL -> TextDirection.ContentOrRtl
    }
}

internal fun bubbleTextAlignFor(direction: ConsultantMessageTextDirection): TextAlign {
    return when (direction) {
        ConsultantMessageTextDirection.LTR -> TextAlign.Left
        ConsultantMessageTextDirection.RTL -> TextAlign.Right
    }
}

internal fun bubbleTextDirectionFor(text: String): ConsultantMessageTextDirection {
    for (char in text) {
        when (Character.getDirectionality(char)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return ConsultantMessageTextDirection.LTR
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> {
                return ConsultantMessageTextDirection.RTL
            }
        }
    }
    return ConsultantMessageTextDirection.LTR
}

internal fun bubbleMetaAlignmentFor(direction: ConsultantMessageTextDirection): Alignment {
    return if (direction == ConsultantMessageTextDirection.RTL) {
        Alignment.BottomStart
    } else {
        Alignment.BottomEnd
    }
}

internal fun imageMetaOverlayAlignment(): Alignment {
    return Alignment.BottomEnd
}

internal fun imageBubbleContentMaxWidth(
    maxBubbleWidthPx: Int,
    imageBubblePaddingPx: Int,
): Int {
    return (maxBubbleWidthPx - imageBubblePaddingPx * 2).coerceAtLeast(0)
}

internal fun imageMessageLayoutSourceSizePx(
    sourceSizePx: Int?,
    densityScale: Float,
    fallbackSizePx: Int,
): Int {
    val source = sourceSizePx?.takeIf { it > 0 } ?: return fallbackSizePx.coerceAtLeast(1)
    val scale = densityScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (source * scale).roundToInt().coerceAtLeast(1)
}

internal fun imageMetaOverlayRequiredWidth(
    metaContentWidthPx: Int,
    overlayEdgePaddingPx: Int,
    overlayHorizontalPaddingPx: Int,
): Int {
    return (
            metaContentWidthPx +
                    overlayEdgePaddingPx * 2 +
                    overlayHorizontalPaddingPx * 2
            ).coerceAtLeast(0)
}
