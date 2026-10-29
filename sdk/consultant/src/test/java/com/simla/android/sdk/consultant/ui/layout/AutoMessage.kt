package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantAutoMessageBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantAutoMessageBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoMessageTest {
    @Test
    fun auto_message_uses_image_width_when_image_is_widest() {
        val result = resolveAutoMessage(
            titleWidthPx = 120,
            bodyWidthPx = 180,
            imageWidthPx = 280,
            buttonWidthsPx = listOf(160),
            maxBubbleContentWidthPx = 320,
        )

        assertEquals(280, result.bubbleContentWidthPx)
        assertEquals(180, result.bodyWidthPx)
    }

    @Test
    fun auto_message_uses_widest_button_width_when_buttons_are_wider_than_text() {
        val result = resolveAutoMessage(
            titleWidthPx = 120,
            bodyWidthPx = 180,
            imageWidthPx = 0,
            buttonWidthsPx = listOf(140, 260),
            maxBubbleContentWidthPx = 320,
        )

        assertEquals(260, result.bubbleContentWidthPx)
    }

    @Test
    fun auto_message_places_meta_inline_when_last_line_has_room() {
        val result = resolveAutoMessage(
            bodyWidthPx = 220,
            lastLineWidthPx = 120,
            metaInlineWidthPx = 52,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(220, result.bubbleContentWidthPx)
    }

    @Test
    fun auto_message_moves_meta_below_when_last_line_is_full() {
        val result = resolveAutoMessage(
            bodyWidthPx = 220,
            lastLineWidthPx = 250,
            metaInlineWidthPx = 52,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(220, result.bubbleContentWidthPx)
    }

    @Test
    fun auto_message_moves_meta_below_when_buttons_are_inside_bubble() {
        val result = resolveAutoMessage(
            bodyWidthPx = 180,
            lastLineWidthPx = 80,
            buttonWidthsPx = listOf(238),
            metaInlineWidthPx = 52,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(238, result.bubbleContentWidthPx)
    }

    @Test
    fun auto_message_respects_max_bubble_content_width() {
        val result = resolveAutoMessage(
            titleWidthPx = 360,
            bodyWidthPx = 340,
            imageWidthPx = 360,
            buttonWidthsPx = listOf(400),
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(280, result.bubbleContentWidthPx)
        assertEquals(280, result.bodyWidthPx)
    }

    private fun resolveAutoMessage(
        authorWidthPx: Int = 80,
        titleWidthPx: Int = 0,
        bodyWidthPx: Int = 180,
        lastLineWidthPx: Int = bodyWidthPx,
        imageWidthPx: Int = 0,
        buttonWidthsPx: List<Int> = emptyList(),
        metaInlineWidthPx: Int = 52,
        maxBubbleContentWidthPx: Int = 280,
    ) = ConsultantAutoMessageBubbleLayoutRules.resolve(
        ConsultantAutoMessageBubbleLayoutInput(
            authorWidthPx = authorWidthPx,
            titleWidthPx = titleWidthPx,
            body = measuredText(
                preferredSingleLineWidthPx = bodyWidthPx,
                lineCount = 2,
                heightPx = 40,
                widestLineWidthPx = bodyWidthPx,
                lastLineWidthPx = lastLineWidthPx,
                lastLineBaselinePx = 32,
            ),
            imageWidthPx = imageWidthPx,
            buttonWidthsPx = buttonWidthsPx,
            meta = ConsultantMeasuredMeta(
                contentWidthPx = metaInlineWidthPx - 12,
                inlineStartSpacingPx = 12,
                heightPx = 16,
                hasStatusIcon = false,
            ),
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )
}
