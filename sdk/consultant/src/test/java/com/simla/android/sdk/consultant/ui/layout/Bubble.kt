package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BubbleTest {
    // Проверяет, что bubble не становится шире максимальной допустимой ширины.
    @Test
    fun very_long_message_without_spaces_caps_bubble_width() {
        val result = resolve(
            authorPreferredWidthPx = 80,
            authorLayoutWidthPx = 80,
            messageSingleLineWidthPx = 640,
            messageWidthPx = 320,
            messageLastLineWidthPx = 280,
            messageLineCount = 3,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(280, result.bodyWidthPx)
        assertEquals(280, result.bubbleContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
    }

    // Проверяет, что ровное попадание в MAX_BUBBLE_WIDTH считается валидным и не вызывает лишний перенос.
    @Test
    fun exact_max_width_boundary_is_valid_without_extra_wrapping() {
        val result = resolve(
            authorPreferredWidthPx = 80,
            authorLayoutWidthPx = 80,
            messageSingleLineWidthPx = 220,
            messageWidthPx = 180,
            messageLastLineWidthPx = 232,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(280, result.bubbleContentWidthPx)
    }

    // Проверяет на серии случайных сообщений, что ширина bubble не выходит за максимум, а короткий message не переносится.
    @Test
    fun random_messages_with_spaces_keep_invariants_for_batch_range() {
        val random = Random(42)

        for (length in 10..1000 step 5) {
            val text = randomMessageWithSpaces(length, random)
            val singleLineWidth = (text.length * 7).coerceAtLeast(20)
            val lineCount = if (singleLineWidth <= 280) 1 else ((singleLineWidth + 279) / 280)
            val wrappedWidth = minOf(singleLineWidth, 280)
            val lastLineWidth = if (lineCount == 1) {
                singleLineWidth
            } else {
                ((singleLineWidth % 280).takeIf { it != 0 } ?: 180).coerceAtMost(280)
            }

            val result = resolve(
                authorPreferredWidthPx = 96,
                authorLayoutWidthPx = 96,
                messageSingleLineWidthPx = singleLineWidth,
                messageWidthPx = wrappedWidth,
                messageLastLineWidthPx = lastLineWidth,
                messageLineCount = lineCount,
                metaInlineWidthPx = 48,
                maxBubbleContentWidthPx = 280,
            )

            assertTrue(
                "bubble width exceeds max for length=$length",
                result.bubbleContentWidthPx <= 280
            )
            if (singleLineWidth <= 280) {
                assertEquals(
                    "single-line message was not preserved for length=$length",
                    singleLineWidth,
                    result.bodyWidthPx,
                )
            }
        }
    }
}
