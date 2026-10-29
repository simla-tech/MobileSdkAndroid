package com.simla.android.sdk.consultant.ui.layout

import androidx.compose.ui.graphics.Color
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import com.simla.android.sdk.consultant.ui.ConsultantProductBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantProductBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.PRODUCT_TITLE_LINK_TAG
import com.simla.android.sdk.consultant.ui.productTitleAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMessageTest {
    @Test
    fun product_message_width_uses_widest_product_element() {
        val result = resolveProduct(
            productTitleWidthPx = 180,
            productInfoWidthPx = 90,
            productImageWidthPx = 80,
            maxBubbleContentWidthPx = 240,
        )

        assertEquals(180, result.bubbleContentWidthPx)
        assertEquals(180, result.productContentWidthPx)
    }

    @Test
    fun product_message_partially_overlaps_meta_with_product_when_there_is_free_space() {
        val result = resolveProduct(
            productTitleWidthPx = 120,
            productInfoWidthPx = 90,
            productImageWidthPx = 80,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 180,
        )

        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(153, result.productContentWidthPx)
    }

    @Test
    fun product_message_places_meta_below_when_price_row_has_no_room() {
        val result = resolveProduct(
            productTitleWidthPx = 120,
            productInfoWidthPx = 90,
            productImageWidthPx = 80,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 120,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(120, result.bubbleContentWidthPx)
    }

    @Test
    fun product_message_places_meta_below_even_when_image_row_has_room() {
        val result = resolveProduct(
            productTitleWidthPx = 90,
            productInfoWidthPx = 90,
            productImageWidthPx = 20,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 120,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
        assertEquals(90, result.bubbleContentWidthPx)
    }

    @Test
    fun product_message_grows_to_fit_author() {
        val result = resolveProduct(
            authorWidthPx = 210,
            productTitleWidthPx = 120,
            productInfoWidthPx = 90,
            productImageWidthPx = 80,
            maxBubbleContentWidthPx = 240,
        )

        assertEquals(210, result.bubbleContentWidthPx)
        assertEquals(210, result.authorWidthPx)
    }

    @Test
    fun product_message_uses_available_width_for_wrapped_title() {
        val result = resolveProduct(
            productTitleWidthPx = 240,
            productInfoWidthPx = 90,
            productImageWidthPx = 80,
            maxBubbleContentWidthPx = 240,
        )

        assertEquals(240, result.productContentWidthPx)
        assertEquals(240, result.bubbleContentWidthPx)
    }

    @Test
    fun product_title_link_span_covers_only_product_title() {
        val title = productTitleAnnotatedString(
            title = "Very long product title",
            titleColor = Color.Blue,
            isTitleClickable = true,
            url = "https://example.com/product",
        )

        assertEquals(
            "https://example.com/product",
            title.getStringAnnotations(PRODUCT_TITLE_LINK_TAG, 0, 0).single().item,
        )
        assertEquals(
            "https://example.com/product",
            title.getStringAnnotations(PRODUCT_TITLE_LINK_TAG, title.length - 1, title.length - 1)
                .single()
                .item,
        )
        assertTrue(
            title.getStringAnnotations(PRODUCT_TITLE_LINK_TAG, title.length, title.length).isEmpty()
        )
    }

    private fun resolveProduct(
        authorWidthPx: Int = 0,
        productTitleWidthPx: Int,
        productInfoWidthPx: Int,
        productImageWidthPx: Int,
        metaWidthPx: Int = 51,
        maxBubbleContentWidthPx: Int,
    ) = ConsultantProductBubbleLayoutRules.resolve(
        ConsultantProductBubbleLayoutInput(
            authorWidthPx = authorWidthPx,
            productTitleWidthPx = productTitleWidthPx,
            productInfoWidthPx = productInfoWidthPx,
            productImageWidthPx = productImageWidthPx,
            productImageHeightPx = 80,
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
