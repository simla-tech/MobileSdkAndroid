package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import com.simla.android.sdk.consultant.ui.ConsultantMessageTextDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaTest {
    // Проверяет, что входящее сообщение без status использует те же правила размещения meta.
    @Test
    fun incoming_message_without_status_still_uses_same_meta_rules() {
        val inlineResult = resolve(
            authorPreferredWidthPx = 90,
            authorLayoutWidthPx = 90,
            messageSingleLineWidthPx = 320,
            messageWidthPx = 210,
            messageLastLineWidthPx = 160,
            messageLineCount = 2,
            metaInlineWidthPx = 32,
            hasStatusIcon = false,
            maxBubbleContentWidthPx = 280,
        )

        val belowResult = resolve(
            authorPreferredWidthPx = 90,
            authorLayoutWidthPx = 90,
            messageSingleLineWidthPx = 320,
            messageWidthPx = 210,
            messageLastLineWidthPx = 260,
            messageLineCount = 2,
            metaInlineWidthPx = 32,
            hasStatusIcon = false,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, inlineResult.metaPlacement)
        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, belowResult.metaPlacement)
        assertTrue(!inlineResult.metaPlacement.name.contains("STATUS"))
    }

    // Проверяет, что для входящего сообщения большой inline-отступ между message и timestamp учитывается при выборе E1/E2.
    @Test
    fun incoming_message_moves_meta_below_when_large_inline_gap_with_timestamp_no_longer_fits() {
        val result = resolve(
            authorPreferredWidthPx = 90,
            authorLayoutWidthPx = 90,
            messageSingleLineWidthPx = 400,
            messageWidthPx = 240,
            messageLastLineWidthPx = 225,
            messageLineCount = 2,
            metaInlineWidthPx = 44,
            hasStatusIcon = false,
            maxBubbleContentWidthPx = 256,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
    }

    // Проверяет, что LTR и RTL используют одни и те же инварианты rule-engine для ширины bubble и размещения meta.
    @Test
    fun ltr_and_rtl_follow_same_rule_engine_invariants() {
        val ltr = resolve(
            direction = ConsultantMessageTextDirection.LTR,
            authorPreferredWidthPx = 90,
            authorLayoutWidthPx = 90,
            messageSingleLineWidthPx = 330,
            messageWidthPx = 240,
            messageLastLineWidthPx = 160,
            messageLineCount = 2,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )
        val rtl = resolve(
            direction = ConsultantMessageTextDirection.RTL,
            authorPreferredWidthPx = 90,
            authorLayoutWidthPx = 90,
            messageSingleLineWidthPx = 330,
            messageWidthPx = 240,
            messageLastLineWidthPx = 160,
            messageLineCount = 2,
            metaInlineWidthPx = 48,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(ltr.metaPlacement, rtl.metaPlacement)
        assertEquals(ltr.bubbleContentWidthPx, rtl.bubbleContentWidthPx)
    }
}
