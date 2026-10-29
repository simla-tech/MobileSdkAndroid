package com.simla.android.sdk.consultant.ui

internal data class ConsultantMeasuredText(
    val sampleText: String,
    val preferredSingleLineWidthPx: Int,
    val lineCount: Int,
    val heightPx: Int,
    val widestLineWidthPx: Int,
    val lastLineWidthPx: Int,
    val lastLineBaselinePx: Int,
    val direction: ConsultantMessageTextDirection,
)
