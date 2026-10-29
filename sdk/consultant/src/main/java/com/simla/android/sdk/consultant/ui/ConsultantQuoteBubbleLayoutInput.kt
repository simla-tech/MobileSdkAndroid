package com.simla.android.sdk.consultant.ui

internal data class ConsultantQuoteBubbleLayoutInput(
    val authorWidthPx: Int,
    val body: ConsultantMeasuredText,
    val quoteContentWidthPx: Int,
    val meta: ConsultantMeasuredMeta,
    val maxBubbleContentWidthPx: Int,
)
