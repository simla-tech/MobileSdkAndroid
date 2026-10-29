package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult

internal fun DrawScope.drawTextRangePressedBackground(
    layout: TextLayoutResult?,
    start: Int,
    end: Int,
    color: Color,
    horizontalPaddingPx: Float,
    cornerRadiusPx: Float,
) {
    if (layout == null || start >= end || color == Color.Transparent) {
        return
    }

    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(end - 1)
    for (line in firstLine..lastLine) {
        val lineStart = maxOf(start, layout.getLineStart(line))
        val lineEnd = minOf(end, layout.getLineEnd(line, visibleEnd = true))
        if (lineStart >= lineEnd) {
            continue
        }

        val startBox = layout.getBoundingBox(lineStart)
        val endBox = layout.getBoundingBox(lineEnd - 1)
        val left = minOf(startBox.left, endBox.left) - horizontalPaddingPx
        val right = maxOf(startBox.right, endBox.right) + horizontalPaddingPx
        val top = minOf(startBox.top, endBox.top)
        val bottom = maxOf(startBox.bottom, endBox.bottom)
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(
                width = (right - left).coerceAtLeast(0f),
                height = (bottom - top).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        )
    }
}
