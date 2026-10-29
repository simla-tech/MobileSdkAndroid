package com.simla.android.sdk.consultant.ui.layout

import androidx.compose.ui.graphics.Color
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import com.simla.android.sdk.consultant.ui.ConsultantOrderBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantOrderBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.ORDER_ITEM_NAME_LINK_TAG
import com.simla.android.sdk.consultant.ui.ORDER_ITEM_NAME_MAX_LINES
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderItemUiModel
import com.simla.android.sdk.consultant.ui.orderItemNameStartOffset
import com.simla.android.sdk.consultant.ui.orderItemTitleAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMessageTest {
    @Test
    fun order_message_width_uses_widest_order_element() {
        val result = resolveOrder(
            orderContentWidthPx = 210,
            lastLineWidthPx = 70,
            maxBubbleContentWidthPx = 250,
        )

        assertEquals(210, result.bubbleContentWidthPx)
        assertEquals(210, result.orderContentWidthPx)
    }

    @Test
    fun order_message_partially_overlaps_meta_with_last_value_when_there_is_free_space() {
        val result = resolveOrder(
            orderContentWidthPx = 120,
            lastLineWidthPx = 80,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 150,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(143, result.orderContentWidthPx)
    }

    @Test
    fun order_message_places_meta_below_when_last_value_has_no_room() {
        val result = resolveOrder(
            orderContentWidthPx = 120,
            lastLineWidthPx = 100,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 150,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(120, result.orderContentWidthPx)
    }

    @Test
    fun order_message_grows_to_fit_author() {
        val result = resolveOrder(
            authorWidthPx = 220,
            orderContentWidthPx = 140,
            lastLineWidthPx = 80,
            maxBubbleContentWidthPx = 260,
        )

        assertEquals(220, result.bubbleContentWidthPx)
        assertEquals(220, result.authorWidthPx)
    }

    @Test
    fun order_item_name_allows_two_lines_before_ellipsis() {
        assertEquals(2, ORDER_ITEM_NAME_MAX_LINES)
    }

    @Test
    fun order_item_title_link_span_excludes_item_number() {
        val item = OnlineConsultantOrderItemUiModel(
            numberLabel = "1.",
            name = "Very long product title",
            details = "2 pcs x 1 000 ₽",
            url = "https://example.com/product",
        )
        val title = orderItemTitleAnnotatedString(
            item = item,
            nameColor = Color.Blue,
            isNameClickable = true,
        )

        assertEquals(3, orderItemNameStartOffset(item))
        assertTrue(title.getStringAnnotations(ORDER_ITEM_NAME_LINK_TAG, 0, 0).isEmpty())
        assertTrue(title.getStringAnnotations(ORDER_ITEM_NAME_LINK_TAG, 2, 2).isEmpty())
        assertEquals(
            "https://example.com/product",
            title.getStringAnnotations(ORDER_ITEM_NAME_LINK_TAG, 3, 3).single().item,
        )
    }

    private fun resolveOrder(
        authorWidthPx: Int = 0,
        orderContentWidthPx: Int,
        lastLineWidthPx: Int,
        metaWidthPx: Int = 51,
        maxBubbleContentWidthPx: Int,
    ) = ConsultantOrderBubbleLayoutRules.resolve(
        ConsultantOrderBubbleLayoutInput(
            authorWidthPx = authorWidthPx,
            orderContentWidthPx = orderContentWidthPx,
            lastLineWidthPx = lastLineWidthPx,
            meta = ConsultantMeasuredMeta(
                contentWidthPx = metaWidthPx,
                inlineStartSpacingPx = 12,
                heightPx = 16,
                hasStatusIcon = true,
            ),
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )
}
