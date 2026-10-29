package com.simla.android.sdk.consultant.ui

internal data class ConsultantMessageBubbleLayoutResult(
    val bubbleContentWidthPx: Int,
    val requiredWidthPx: Int,
    val bodyWidthPx: Int,
    val authorWidthPx: Int,
    val metaPlacement: ConsultantMessageMetaPlacement,
    val metaOverlayOffsetYPx: Int,
)
