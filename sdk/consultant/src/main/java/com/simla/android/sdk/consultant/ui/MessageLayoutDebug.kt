package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun buildTextLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    horizontalPaddingPx: Int,
    verticalPaddingPx: Int,
    contentMaxWidthPx: Int,
    authorWidthPx: Int,
    body: ConsultantMeasuredText,
    metaWidthPx: Int,
    metaHeightPx: Int,
    layoutResult: ConsultantMessageBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: text-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("horizontal padding: $horizontalPaddingPx px")
        appendLine("vertical padding: $verticalPaddingPx px")
        appendLine("content max width: $contentMaxWidthPx px")
        appendLine("author width: $authorWidthPx px")
        appendLine("body preferred width: ${body.preferredSingleLineWidthPx} px")
        appendLine("body widest line: ${body.widestLineWidthPx} px")
        appendLine("body last line: ${body.lastLineWidthPx} px")
        appendLine("body lines: ${body.lineCount}")
        appendLine("body height: ${body.heightPx} px")
        appendLine("body direction: ${body.direction}")
        appendLine("meta width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${layoutResult.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("required width: ${layoutResult.requiredWidthPx} px")
        appendLine("body width: ${layoutResult.bodyWidthPx} px")
        appendLine("author width: ${layoutResult.authorWidthPx} px")
        appendLine("meta placement: ${layoutResult.metaPlacement}")
        appendLine("meta overlay offset y: ${layoutResult.metaOverlayOffsetYPx} px")
        appendLine()
        appendLine("Text")
        append(message.text.ifEmpty { "<empty>" })
    }
}

internal fun buildImageLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    imageBubblePaddingPx: Int,
    imageContentMaxWidthPx: Int,
    imageContentMaxHeightPx: Int,
    imageWidthPx: Int,
    imageHeightPx: Int,
    authorWidthPx: Int,
    metaWidthPx: Int,
    metaContentWidthPx: Int,
    metaHeightPx: Int,
    result: ConsultantImageBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: image-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine("imageUrl: ${message.imageUrl ?: "n/a"}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("image bubble padding: $imageBubblePaddingPx px")
        appendLine("image content max width: $imageContentMaxWidthPx px")
        appendLine("image content max height: $imageContentMaxHeightPx px")
        appendLine("source image width: $imageWidthPx px")
        appendLine("source image height: $imageHeightPx px")
        appendLine("source aspect ratio: ${formatRatio(imageWidthPx, imageHeightPx)}")
        appendLine("author width: $authorWidthPx px")
        appendLine("meta content width: $metaContentWidthPx px")
        appendLine("meta overlay required width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${result.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("image width: ${result.imageWidthPx} px")
        appendLine("image height: ${result.imageHeightPx} px")
        appendLine("bubble aspect ratio: ${formatRatio(result.imageWidthPx, result.imageHeightPx)}")
        appendLine("meta placement: ${result.metaPlacement}")
        appendLine("content scale: ${result.contentScale}")
    }
}

internal fun buildFileLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    horizontalPaddingPx: Int,
    verticalPaddingPx: Int,
    contentMaxWidthPx: Int,
    fileNameWidthPx: Int,
    fileSizeWidthPx: Int,
    iconWidthPx: Int,
    iconTextSpacingPx: Int,
    fileContentHeightPx: Int,
    authorWidthPx: Int,
    metaWidthPx: Int,
    metaHeightPx: Int,
    result: ConsultantFileBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: file-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine("fileUrl: ${message.fileUrl ?: "n/a"}")
        appendLine("fileName: ${message.fileName ?: "n/a"}")
        appendLine("fileSizeLabel: ${message.fileSizeLabel ?: "n/a"}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("horizontal padding: $horizontalPaddingPx px")
        appendLine("vertical padding: $verticalPaddingPx px")
        appendLine("content max width: $contentMaxWidthPx px")
        appendLine("file name width: $fileNameWidthPx px")
        appendLine("file size width: $fileSizeWidthPx px")
        appendLine("file icon width: $iconWidthPx px")
        appendLine("file icon text spacing: $iconTextSpacingPx px")
        appendLine("file content height: $fileContentHeightPx px")
        appendLine("author width: $authorWidthPx px")
        appendLine("meta width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${result.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("file content width: ${result.fileContentWidthPx} px")
        appendLine("file text column width: ${result.fileTextColumnWidthPx} px")
        appendLine("author width: ${result.authorWidthPx} px")
        appendLine("meta placement: ${result.metaPlacement}")
        appendLine("meta overlay offset y: ${result.metaOverlayOffsetYPx} px")
    }
}

internal fun buildProductLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    horizontalPaddingPx: Int,
    contentMaxWidthPx: Int,
    productTitleWidthPx: Int,
    productInfoWidthPx: Int,
    productImageWidthPx: Int,
    productImageHeightPx: Int,
    metaWidthPx: Int,
    metaHeightPx: Int,
    result: ConsultantProductBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: product-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine("productName: ${message.productName ?: "n/a"}")
        appendLine("productUrl: ${message.productUrl ?: "n/a"}")
        appendLine("productImageUrl: ${message.productImageUrl ?: "n/a"}")
        appendLine("productPriceLabel: ${message.productPriceLabel ?: "n/a"}")
        appendLine("productQuantityLabel: ${message.productQuantityLabel ?: "n/a"}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("horizontal padding: $horizontalPaddingPx px")
        appendLine("content max width: $contentMaxWidthPx px")
        appendLine("product title width: $productTitleWidthPx px")
        appendLine("product info width: $productInfoWidthPx px")
        appendLine("product image width: $productImageWidthPx px")
        appendLine("product image height: $productImageHeightPx px")
        appendLine("meta width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${result.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("product content width: ${result.productContentWidthPx} px")
        appendLine("author width: ${result.authorWidthPx} px")
        appendLine("meta placement: ${result.metaPlacement}")
    }
}

internal fun buildOrderLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    horizontalPaddingPx: Int,
    contentMaxWidthPx: Int,
    orderContentWidthPx: Int,
    orderLastLineWidthPx: Int,
    metaWidthPx: Int,
    metaHeightPx: Int,
    result: ConsultantOrderBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: order-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine("orderItems: ${message.orderItems.size}")
        appendLine("orderFields: ${message.orderFields.size}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("horizontal padding: $horizontalPaddingPx px")
        appendLine("content max width: $contentMaxWidthPx px")
        appendLine("order content width: $orderContentWidthPx px")
        appendLine("order last line width: $orderLastLineWidthPx px")
        appendLine("meta width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${result.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("order content width: ${result.orderContentWidthPx} px")
        appendLine("author width: ${result.authorWidthPx} px")
        appendLine("meta placement: ${result.metaPlacement}")
    }
}

internal fun buildQuoteLayoutDebug(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidthPx: Int,
    horizontalPaddingPx: Int,
    contentMaxWidthPx: Int,
    body: ConsultantMeasuredText,
    quotePrimaryWidthPx: Int,
    quoteSecondaryWidthPx: Int,
    quoteContentWidthPx: Int,
    quoteHeightPx: Int,
    metaWidthPx: Int,
    metaHeightPx: Int,
    result: ConsultantQuoteBubbleLayoutResult,
    bubbleWidthPx: Int,
): String {
    return buildString {
        appendLine("type: quote-message")
        appendLine("id: ${message.id}")
        appendLine("author: ${message.author}")
        appendLine("authorName: ${message.authorName}")
        appendLine("timeLabel: ${message.timeLabel}")
        appendLine("deliveryStatus: ${message.deliveryStatus}")
        appendLine("quoteText: ${message.quoteText ?: "n/a"}")
        appendLine("quoteFileName: ${message.quoteFileName ?: "n/a"}")
        appendLine("quoteFileSizeLabel: ${message.quoteFileSizeLabel ?: "n/a"}")
        appendLine("quoteImageUrl: ${message.quoteImageUrl ?: "n/a"}")
        appendLine()
        appendLine("Input")
        appendLine("container max bubble width: $maxBubbleWidthPx px")
        appendLine("horizontal padding: $horizontalPaddingPx px")
        appendLine("content max width: $contentMaxWidthPx px")
        appendLine("body widest line: ${body.widestLineWidthPx} px")
        appendLine("body last line: ${body.lastLineWidthPx} px")
        appendLine("body height: ${body.heightPx} px")
        appendLine("quote primary width: $quotePrimaryWidthPx px")
        appendLine("quote secondary width: $quoteSecondaryWidthPx px")
        appendLine("quote content width: $quoteContentWidthPx px")
        appendLine("quote height: $quoteHeightPx px")
        appendLine("meta width: $metaWidthPx px")
        appendLine("meta height: $metaHeightPx px")
        appendLine()
        appendLine("Result")
        appendLine("bubble content width: ${result.bubbleContentWidthPx} px")
        appendLine("bubble width with padding: $bubbleWidthPx px")
        appendLine("body width: ${result.bodyWidthPx} px")
        appendLine("quote content width: ${result.quoteContentWidthPx} px")
        appendLine("author width: ${result.authorWidthPx} px")
        appendLine("meta placement: ${result.metaPlacement}")
        appendLine("meta overlay offset y: ${result.metaOverlayOffsetYPx} px")
    }
}

internal fun formatRatio(widthPx: Int, heightPx: Int): String {
    if (heightPx == 0) {
        return "n/a"
    }

    val ratio = widthPx.toFloat() / heightPx.toFloat()
    return "%.3f".format(java.util.Locale.US, ratio)
}
