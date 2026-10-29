package com.simla.android.sdk.consultant.ui

internal data class ConsultantMessageBubbleLayoutInput(
    val author: ConsultantMeasuredAuthor,
    val body: ConsultantMeasuredText,
    val meta: ConsultantMeasuredMeta,
    val maxBubbleContentWidthPx: Int,
)
