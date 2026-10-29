package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class TextMessageTest {
    // Проверяет bubble-03: если после сужения bubble последняя строка стала короткой, meta нужно поднять inline.
    @Test
    fun stable_layout_remeasures_short_dangling_last_line_and_places_meta_inline() {
        val initialBody = measuredText(
            preferredSingleLineWidthPx = 520,
            lineCount = 3,
            heightPx = 60,
            widestLineWidthPx = 240,
            lastLineWidthPx = 250,
        )
        val finalBody = initialBody.copy(
            widestLineWidthPx = 240,
            lastLineWidthPx = 8,
        )

        val result = resolveStable(
            messageBody = initialBody,
            metaInlineWidthPx = 60,
            maxBubbleContentWidthPx = 280,
            remeasureBody = { finalBody },
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(240, result.bubbleContentWidthPx)
    }

    // Проверяет text-07: если после сужения bubble последняя строка заняла место под meta, meta должна уйти ниже.
    @Test
    fun stable_layout_remeasures_occupied_last_line_and_moves_meta_below() {
        val initialBody = measuredText(
            preferredSingleLineWidthPx = 520,
            lineCount = 3,
            heightPx = 60,
            widestLineWidthPx = 240,
            lastLineWidthPx = 100,
        )
        val finalBody = initialBody.copy(
            widestLineWidthPx = 240,
            lastLineWidthPx = 230,
        )

        val result = resolveStable(
            messageBody = initialBody,
            metaInlineWidthPx = 60,
            maxBubbleContentWidthPx = 280,
            remeasureBody = { finalBody },
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(240, result.bubbleContentWidthPx)
    }

    // Проверяет, что при inline-meta ширина bubble учитывает самую широкую строку message, а не только последнюю строку с meta.
    @Test
    fun e1_required_width_respects_widest_message_line_not_only_last_line_plus_meta() {
        val result = resolve(
            authorPreferredWidthPx = 96,
            authorLayoutWidthPx = 96,
            messageSingleLineWidthPx = 500,
            messageWidthPx = 240,
            messageLastLineWidthPx = 80,
            messageLineCount = 4,
            metaInlineWidthPx = 60,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(240, result.requiredWidthPx)
        assertEquals(240, result.bubbleContentWidthPx)
    }

    // Проверяет, что широкий author не запрещает inline-meta, если в последней строке message есть место.
    @Test
    fun wide_author_does_not_force_meta_below_if_last_line_has_room() {
        val result = resolve(
            authorPreferredWidthPx = 360,
            authorLayoutWidthPx = 360,
            authorCanWrap = false,
            messageWidthPx = 220,
            messageSingleLineWidthPx = 220,
            messageLastLineWidthPx = 90,
            messageLineCount = 3,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(280, result.authorWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(280, result.bubbleContentWidthPx)
    }

    // Проверяет, что message, который помещается в одну строку, не использует уже посчитанную переносную ширину.
    @Test
    fun single_line_message_that_fits_keeps_single_line_width() {
        val result = resolve(
            authorPreferredWidthPx = 72,
            authorLayoutWidthPx = 72,
            messageSingleLineWidthPx = 190,
            messageWidthPx = 140,
            messageLastLineWidthPx = 190,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(190, result.bodyWidthPx)
        assertEquals(238, result.bubbleContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
    }

    // Проверяет, что однострочный message не переносится ради inline-meta; meta уходит под текст.
    @Test
    fun single_line_message_keeps_single_line_and_moves_meta_below_when_inline_does_not_fit() {
        val result = resolve(
            authorPreferredWidthPx = 72,
            authorLayoutWidthPx = 72,
            messageSingleLineWidthPx = 250,
            messageWidthPx = 160,
            messageLastLineWidthPx = 250,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(250, result.bodyWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(250, result.bubbleContentWidthPx)
    }

    // Проверяет, что явные переносы строк в message учитываются через фактическую ширину последней строки.
    @Test
    fun explicit_line_breaks_use_actual_last_line_width_for_meta_decision() {
        val result = resolve(
            authorPreferredWidthPx = 70,
            authorLayoutWidthPx = 70,
            messageSingleLineWidthPx = 300,
            messageWidthPx = 200,
            messageLastLineWidthPx = 110,
            messageLineCount = 3,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(200, result.bubbleContentWidthPx)
    }

    // Проверяет сценарий E1: meta продолжает последнюю строку message, если там достаточно места.
    @Test
    fun multiline_message_with_space_in_last_line_uses_inline_overlay() {
        val result = resolve(
            authorPreferredWidthPx = 88,
            authorLayoutWidthPx = 88,
            messageSingleLineWidthPx = 420,
            messageWidthPx = 220,
            messageLastLineWidthPx = 150,
            messageLineCount = 3,
            metaInlineWidthPx = 48,
            metaHeightPx = 18,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(-3, result.metaOverlayOffsetYPx)
        assertEquals(220, result.bubbleContentWidthPx)
    }

    // Проверяет сценарий E2: meta переносится под message, если последняя строка не вмещает meta.
    @Test
    fun multiline_message_without_space_in_last_line_places_meta_below() {
        val result = resolve(
            authorPreferredWidthPx = 88,
            authorLayoutWidthPx = 88,
            messageSingleLineWidthPx = 420,
            messageWidthPx = 220,
            messageLastLineWidthPx = 250,
            messageLineCount = 3,
            metaInlineWidthPx = 48,
            metaHeightPx = 18,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(0, result.metaOverlayOffsetYPx)
        assertEquals(220, result.bubbleContentWidthPx)
    }
}
