package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantContactFormBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantContactFormBubbleLayoutRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactFormMessageTest {
    @Test
    fun contact_form_message_uses_figma_form_width_when_message_is_shorter() {
        val result = resolveContactForm(
            messageWidthPx = 180,
            inputWidthPx = 312,
            buttonWidthPx = 312,
            maxBubbleContentWidthPx = 360,
        )

        assertEquals(312, result.formContentWidthPx)
        assertEquals(312, result.bubbleContentWidthPx)
    }

    @Test
    fun contact_form_message_grows_to_message_width_when_text_is_wider_than_form() {
        val result = resolveContactForm(
            messageWidthPx = 340,
            inputWidthPx = 312,
            buttonWidthPx = 312,
            maxBubbleContentWidthPx = 360,
        )

        assertEquals(312, result.formContentWidthPx)
        assertEquals(340, result.bubbleContentWidthPx)
    }

    @Test
    fun contact_form_message_respects_max_bubble_content_width() {
        val result = resolveContactForm(
            messageWidthPx = 420,
            inputWidthPx = 312,
            buttonWidthPx = 312,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(280, result.formContentWidthPx)
        assertEquals(280, result.bubbleContentWidthPx)
    }

    private fun resolveContactForm(
        messageWidthPx: Int,
        inputWidthPx: Int,
        buttonWidthPx: Int,
        maxBubbleContentWidthPx: Int,
    ) = ConsultantContactFormBubbleLayoutRules.resolve(
        ConsultantContactFormBubbleLayoutInput(
            messageWidthPx = messageWidthPx,
            inputWidthPx = inputWidthPx,
            buttonWidthPx = buttonWidthPx,
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )
}
