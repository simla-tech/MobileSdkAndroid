package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorTest {
    // Проверяет, что широкий author задает ширину bubble, если message и meta уже, чем author.
    @Test
    fun author_wider_than_short_message_defines_bubble_width() {
        val result = resolve(
            authorPreferredWidthPx = 140,
            authorLayoutWidthPx = 140,
            messageSingleLineWidthPx = 40,
            messageWidthPx = 40,
            messageLastLineWidthPx = 40,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(140, result.authorWidthPx)
        assertEquals(40, result.bodyWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(140, result.bubbleContentWidthPx)
    }

    // Проверяет, что author с пробелами использует фактическую ширину после переноса, когда однострочно он шире лимита.
    @Test
    fun long_wrappable_author_uses_wrapped_layout_width_after_hitting_limit() {
        val result = resolve(
            authorPreferredWidthPx = 360,
            authorLayoutWidthPx = 200,
            authorCanWrap = true,
            messageSingleLineWidthPx = 70,
            messageWidthPx = 70,
            messageLastLineWidthPx = 70,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(200, result.authorWidthPx)
        assertEquals(200, result.bubbleContentWidthPx)
    }

    // Проверяет, что author без пробелов не переносится и ограничивается шириной bubble под ellipsize.
    @Test
    fun long_non_wrappable_author_uses_ellipsis_and_caps_at_max_width() {
        val result = resolve(
            authorPreferredWidthPx = 360,
            authorLayoutWidthPx = 360,
            authorCanWrap = false,
            messageSingleLineWidthPx = 70,
            messageWidthPx = 70,
            messageLastLineWidthPx = 70,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(280, result.authorWidthPx)
        assertEquals(280, result.bubbleContentWidthPx)
    }

    // Проверяет, что пустой author не ломает расчет ширины сообщения.
    @Test
    fun empty_author_does_not_break_required_width_calculation() {
        val result = resolve(
            authorPreferredWidthPx = 0,
            authorLayoutWidthPx = 0,
            messageSingleLineWidthPx = 160,
            messageWidthPx = 160,
            messageLastLineWidthPx = 160,
            messageLineCount = 1,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(208, result.bubbleContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
    }
}
