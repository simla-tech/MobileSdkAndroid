package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun TextLayoutResult.lineWidthPx(lineIndex: Int): Int {
    return (getLineRight(lineIndex) - getLineLeft(lineIndex))
        .coerceAtLeast(0f)
        .roundToInt()
}

internal fun TextLayoutResult.linkHighlightRects(range: TextRange): List<Rect> {
    if (range.collapsed) {
        return emptyList()
    }

    val rects = mutableListOf<Rect>()
    var currentRect: Rect? = null
    var currentLine = -1

    for (offset in range.start until range.end) {
        val charRect = getBoundingBox(offset)
        if (charRect.width <= 0f || charRect.height <= 0f) {
            continue
        }

        val line = getLineForOffset(offset)
        currentRect = if (currentRect == null) {
            currentLine = line
            charRect
        } else if (line == currentLine && charRect.left <= currentRect.right + 1f) {
            Rect(
                left = min(currentRect.left, charRect.left),
                top = min(currentRect.top, charRect.top),
                right = max(currentRect.right, charRect.right),
                bottom = max(currentRect.bottom, charRect.bottom),
            )
        } else {
            rects += currentRect
            currentLine = line
            charRect
        }
    }

    currentRect?.let(rects::add)
    return rects
}
