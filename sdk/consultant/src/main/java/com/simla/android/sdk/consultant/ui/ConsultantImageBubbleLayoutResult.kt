package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.layout.ContentScale

internal data class ConsultantImageBubbleLayoutResult(
    val bubbleContentWidthPx: Int,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val metaPlacement: ConsultantMessageMetaPlacement,
    val contentScale: ContentScale,
)
