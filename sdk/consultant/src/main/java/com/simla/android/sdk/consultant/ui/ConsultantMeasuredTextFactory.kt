package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max
import kotlin.math.roundToInt

internal fun toMeasuredText(
    layout: TextLayoutResult,
    sampleText: String,
    direction: ConsultantMessageTextDirection,
): ConsultantMeasuredText {
    var widestLineWidth = 0
    for (lineIndex in 0 until layout.lineCount) {
        widestLineWidth = max(widestLineWidth, layout.lineWidthPx(lineIndex))
    }

    return ConsultantMeasuredText(
        sampleText = sampleText,
        preferredSingleLineWidthPx = widestLineWidth,
        lineCount = layout.lineCount,
        heightPx = layout.size.height,
        widestLineWidthPx = widestLineWidth,
        lastLineWidthPx = layout.lineWidthPx(layout.lineCount - 1),
        lastLineBaselinePx = layout.getLineBaseline(layout.lineCount - 1).roundToInt(),
        direction = direction,
    )
}
