package com.simla.android.sdk.consultant.ui

import kotlin.math.max

internal object ConsultantFileBubbleLayoutRules {
    fun resolve(input: ConsultantFileBubbleLayoutInput): ConsultantFileBubbleLayoutResult {
        val maxWidth = input.maxBubbleContentWidthPx.coerceAtLeast(0)
        val iconAndGapWidth = input.iconWidthPx.coerceAtLeast(0) +
                input.iconTextSpacingPx.coerceAtLeast(0)
        val fileNameWidth = input.fileNameWidthPx.coerceAtLeast(0)
        val fileSizeWidth = input.fileSizeWidthPx.coerceAtLeast(0)
        val baseFileTextColumnWidth = max(fileNameWidth, fileSizeWidth)
        val baseFileContentWidth = iconAndGapWidth + baseFileTextColumnWidth
        val authorWidth = input.authorWidthPx.coerceIn(0, maxWidth)
        val metaWidth = input.metaWidthPx.coerceIn(0, maxWidth)
        val inlineMetaWidth = input.meta.inlineStartSpacingPx + input.meta.contentWidthPx
        val inlineFileContentWidth = iconAndGapWidth + fileSizeWidth + inlineMetaWidth
        val canPlaceMetaInline = inlineFileContentWidth <= maxWidth
        val requiredFileContentWidth = if (canPlaceMetaInline) {
            max(baseFileContentWidth, inlineFileContentWidth)
        } else {
            baseFileContentWidth
        }
        val fileContentWidth = requiredFileContentWidth.coerceIn(0, maxWidth)
        val bubbleWidth = if (canPlaceMetaInline) {
            max(fileContentWidth, authorWidth)
        } else {
            max(fileContentWidth, max(authorWidth, metaWidth))
        }.coerceAtMost(maxWidth)

        return ConsultantFileBubbleLayoutResult(
            bubbleContentWidthPx = bubbleWidth,
            fileContentWidthPx = fileContentWidth,
            fileTextColumnWidthPx = (fileContentWidth - iconAndGapWidth).coerceAtLeast(0),
            authorWidthPx = authorWidth,
            metaPlacement = if (canPlaceMetaInline) {
                ConsultantMessageMetaPlacement.INLINE_OVERLAY
            } else {
                ConsultantMessageMetaPlacement.BELOW_TEXT
            },
            metaOverlayOffsetYPx = if (canPlaceMetaInline) {
                input.fileSizeLineBaselinePx + (input.meta.heightPx / 2) - input.fileSizeLineHeightPx
            } else {
                0
            },
        )
    }
}
