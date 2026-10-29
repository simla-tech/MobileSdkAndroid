package com.simla.android.sdk.consultant.ui

internal data class ConsultantFileBubbleLayoutInput(
    val authorWidthPx: Int,
    val fileNameWidthPx: Int,
    val fileSizeWidthPx: Int,
    val iconWidthPx: Int,
    val iconTextSpacingPx: Int,
    val fileSizeLineHeightPx: Int,
    val fileSizeLineBaselinePx: Int,
    val meta: ConsultantMeasuredMeta,
    val metaWidthPx: Int,
    val maxBubbleContentWidthPx: Int,
)
