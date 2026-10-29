package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMeasuredAuthor
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredText
import com.simla.android.sdk.consultant.ui.ConsultantMessageBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantMessageBubbleLayoutResult
import com.simla.android.sdk.consultant.ui.ConsultantMessageBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.ConsultantMessageTextDirection
import kotlin.random.Random

internal fun resolveStable(
    messageBody: ConsultantMeasuredText,
    metaInlineWidthPx: Int,
    maxBubbleContentWidthPx: Int,
    authorPreferredWidthPx: Int = 96,
    authorLayoutWidthPx: Int = 96,
    authorCanWrap: Boolean = false,
    hasStatusIcon: Boolean = true,
    remeasureBody: (contentWidthPx: Int) -> ConsultantMeasuredText,
): ConsultantMessageBubbleLayoutResult {
    return ConsultantMessageBubbleLayoutRules.resolveStable(
        ConsultantMessageBubbleLayoutInput(
            author = ConsultantMeasuredAuthor(
                sampleText = "author",
                preferredSingleLineWidthPx = authorPreferredWidthPx,
                layoutWidthPx = authorLayoutWidthPx,
                canWrap = authorCanWrap,
            ),
            body = messageBody,
            meta = ConsultantMeasuredMeta(
                contentWidthPx = metaInlineWidthPx - 12,
                inlineStartSpacingPx = 12,
                heightPx = 16,
                hasStatusIcon = hasStatusIcon,
            ),
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
        remeasureBody = remeasureBody,
    )
}

internal fun resolve(
    authorPreferredWidthPx: Int,
    authorLayoutWidthPx: Int,
    messageSingleLineWidthPx: Int,
    messageWidthPx: Int,
    messageLastLineWidthPx: Int,
    messageLineCount: Int,
    metaInlineWidthPx: Int,
    maxBubbleContentWidthPx: Int,
    metaHeightPx: Int = 16,
    messageHeightPx: Int = 60,
    lastLineBaselinePx: Int = 48,
    authorCanWrap: Boolean = false,
    hasStatusIcon: Boolean = true,
    direction: ConsultantMessageTextDirection = ConsultantMessageTextDirection.LTR,
): ConsultantMessageBubbleLayoutResult {
    return ConsultantMessageBubbleLayoutRules.resolve(
        ConsultantMessageBubbleLayoutInput(
            author = ConsultantMeasuredAuthor(
                sampleText = "author",
                preferredSingleLineWidthPx = authorPreferredWidthPx,
                layoutWidthPx = authorLayoutWidthPx,
                canWrap = authorCanWrap,
            ),
            body = ConsultantMeasuredText(
                sampleText = "message",
                preferredSingleLineWidthPx = messageSingleLineWidthPx,
                lineCount = messageLineCount,
                heightPx = messageHeightPx,
                widestLineWidthPx = messageWidthPx,
                lastLineWidthPx = messageLastLineWidthPx,
                lastLineBaselinePx = lastLineBaselinePx,
                direction = direction,
            ),
            meta = ConsultantMeasuredMeta(
                contentWidthPx = metaInlineWidthPx - 12,
                inlineStartSpacingPx = 12,
                heightPx = metaHeightPx,
                hasStatusIcon = hasStatusIcon,
            ),
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )
}

internal fun measuredText(
    preferredSingleLineWidthPx: Int,
    lineCount: Int,
    heightPx: Int,
    widestLineWidthPx: Int,
    lastLineWidthPx: Int,
    lastLineBaselinePx: Int = 48,
    direction: ConsultantMessageTextDirection = ConsultantMessageTextDirection.LTR,
): ConsultantMeasuredText {
    return ConsultantMeasuredText(
        sampleText = "message",
        preferredSingleLineWidthPx = preferredSingleLineWidthPx,
        lineCount = lineCount,
        heightPx = heightPx,
        widestLineWidthPx = widestLineWidthPx,
        lastLineWidthPx = lastLineWidthPx,
        lastLineBaselinePx = lastLineBaselinePx,
        direction = direction,
    )
}

internal fun randomMessageWithSpaces(
    length: Int,
    random: Random,
): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val builder = StringBuilder(length + length / 6)

    while (builder.length < length) {
        val wordLength = random.nextInt(1, 12)
        repeat(wordLength.coerceAtMost(length - builder.length)) {
            builder.append(alphabet[random.nextInt(alphabet.length)])
        }
        if (builder.length < length) {
            builder.append(' ')
        }
    }

    return builder.toString().trim()
}
