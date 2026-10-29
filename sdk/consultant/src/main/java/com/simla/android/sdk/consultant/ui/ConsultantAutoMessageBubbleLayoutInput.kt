package com.simla.android.sdk.consultant.ui

internal data class ConsultantAutoMessageBubbleLayoutInput(
    val authorWidthPx: Int,
    val titleWidthPx: Int,
    val body: ConsultantMeasuredText,
    val imageWidthPx: Int,
    val buttonWidthsPx: List<Int>,
    val meta: ConsultantMeasuredMeta,
    val maxBubbleContentWidthPx: Int,
)
