package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredText
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import com.simla.android.sdk.consultant.ui.ConsultantMessageTextDirection
import com.simla.android.sdk.consultant.ui.ConsultantQuoteBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantQuoteBubbleLayoutRules
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteMessageTest {
    // Проверяет, что bubble quote-message выбирает ширину по самому широкому элементу: author, quote или message.
    @Test
    fun quote_message_width_uses_widest_author_quote_or_message_content() {
        val result = resolveQuote(
            body = measuredBody(
                preferredSingleLineWidthPx = 120,
                widestLineWidthPx = 120,
                lastLineWidthPx = 40,
            ),
            quoteContentWidthPx = 170,
            maxBubbleContentWidthPx = 260,
        )

        assertEquals(170, result.bubbleContentWidthPx)
        assertEquals(170, result.quoteContentWidthPx)
        assertEquals(120, result.bodyWidthPx)
    }

    // Проверяет, что meta наезжает на нижнюю строку message, если в этой строке хватает места.
    @Test
    fun quote_message_places_meta_over_message_last_line_when_there_is_free_space() {
        val result = resolveQuote(
            body = measuredBody(
                preferredSingleLineWidthPx = 120,
                widestLineWidthPx = 120,
                lastLineWidthPx = 80,
                lastLineBaselinePx = 32,
                heightPx = 40,
            ),
            quoteContentWidthPx = 170,
            maxBubbleContentWidthPx = 260,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(0, result.metaOverlayOffsetYPx)
    }

    // Проверяет, что meta переносится под message, если на нижней строке message нет места.
    @Test
    fun quote_message_places_meta_below_message_when_last_line_has_no_room() {
        val result = resolveQuote(
            body = measuredBody(
                preferredSingleLineWidthPx = 190,
                widestLineWidthPx = 190,
                lastLineWidthPx = 150,
            ),
            quoteContentWidthPx = 170,
            maxBubbleContentWidthPx = 190,
        )

        assertEquals(190, result.bubbleContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
    }

    // Проверяет, что длинная нижняя строка quote не запрещает inline-meta, если в последней строке message место есть.
    @Test
    fun quote_message_ignores_quote_last_line_when_resolving_meta_position() {
        val result = resolveQuote(
            body = measuredBody(
                preferredSingleLineWidthPx = 120,
                widestLineWidthPx = 120,
                lastLineWidthPx = 80,
            ),
            quoteContentWidthPx = 190,
            maxBubbleContentWidthPx = 190,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
    }

    // Проверяет, что ширина author тоже участвует в расчете общего размера quote bubble.
    @Test
    fun quote_message_grows_to_fit_author() {
        val result = resolveQuote(
            authorWidthPx = 220,
            body = measuredBody(
                preferredSingleLineWidthPx = 120,
                widestLineWidthPx = 120,
                lastLineWidthPx = 80,
            ),
            quoteContentWidthPx = 170,
            maxBubbleContentWidthPx = 260,
        )

        assertEquals(220, result.bubbleContentWidthPx)
        assertEquals(220, result.authorWidthPx)
    }

    @Test
    fun quote_message_stretches_quote_content_to_bubble_width() {
        val result = resolveQuote(
            body = measuredBody(
                preferredSingleLineWidthPx = 210,
                widestLineWidthPx = 210,
                lastLineWidthPx = 80,
            ),
            quoteContentWidthPx = 120,
            maxBubbleContentWidthPx = 260,
        )

        assertEquals(210, result.bubbleContentWidthPx)
        assertEquals(210, result.quoteContentWidthPx)
    }

    private fun resolveQuote(
        authorWidthPx: Int = 0,
        body: ConsultantMeasuredText,
        quoteContentWidthPx: Int,
        maxBubbleContentWidthPx: Int,
    ) = ConsultantQuoteBubbleLayoutRules.resolve(
        ConsultantQuoteBubbleLayoutInput(
            authorWidthPx = authorWidthPx,
            body = body,
            quoteContentWidthPx = quoteContentWidthPx,
            meta = ConsultantMeasuredMeta(
                contentWidthPx = 51,
                inlineStartSpacingPx = 12,
                heightPx = 16,
                hasStatusIcon = true,
            ),
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )

    private fun measuredBody(
        preferredSingleLineWidthPx: Int,
        widestLineWidthPx: Int,
        lastLineWidthPx: Int,
        lastLineBaselinePx: Int = 32,
        heightPx: Int = 40,
    ) = ConsultantMeasuredText(
        sampleText = "Message",
        preferredSingleLineWidthPx = preferredSingleLineWidthPx,
        lineCount = 2,
        heightPx = heightPx,
        widestLineWidthPx = widestLineWidthPx,
        lastLineWidthPx = lastLineWidthPx,
        lastLineBaselinePx = lastLineBaselinePx,
        direction = ConsultantMessageTextDirection.LTR,
    )
}
