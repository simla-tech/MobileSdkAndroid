package com.simla.android.sdk.notifications.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushMessageParserTest {
    @Test
    fun `parses sdk push with valid fingerprint and push id`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "1",
                "r_group_category" to " news ",
                "push_id" to "app_42",
                "title" to "Title",
                "body" to "Body",
                "image_url" to "https://example.test/push.png",
                "click_action" to "openOrder",
                "ca_orderId" to "123",
                "utm_source" to " push ",
                "utm_medium" to "mobile",
                "utm_campaign" to "october",
                "utm_term" to "jackets",
                "utm_content" to "banner",
                "buttons" to
                        """[{"title":"Open order","value":"sample-shop://orders/123"}]""",
            ),
        )

        requireNotNull(message)
        assertEquals("app_42", message.pushId)
        assertEquals("news", message.category)
        assertEquals("Title", message.title)
        assertEquals("Body", message.body)
        assertEquals("https://example.test/push.png", message.imageUrl)
        assertEquals("openOrder", message.clickAction)
        assertEquals(mapOf("orderId" to "123"), message.customAttributes)
        assertEquals(
            PushUtm("push", "mobile", "october", "jackets", "banner"),
            message.utm,
        )
        assertEquals(
            listOf(PushButton("Open order", "sample-shop://orders/123")),
            message.buttons,
        )
    }

    @Test
    fun `rejects push without sdk fingerprint`() {
        val message = PushMessageParser.parse(
            data = mapOf("push_id" to "app_42"),
        )

        assertNull(message)
    }

    @Test
    fun `rejects malformed push id`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "true",
                "push_id" to "invalid",
            ),
        )

        assertNull(message)
    }

    @Test
    fun `accepts silent sdk push`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "true",
                "push_id" to "app_42",
            ),
        )

        requireNotNull(message)
        assertNull(message.category)
        assertNull(message.title)
        assertNull(message.body)
    }

    @Test
    fun `treats blank push category as absent`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "1",
                "r_group_category" to "   ",
                "push_id" to "app_42",
            ),
        )

        requireNotNull(message)
        assertNull(message.category)
    }

    @Test
    fun `treats blank utm values as absent`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "1",
                "push_id" to "app_42",
                "utm_source" to "   ",
            ),
        )

        requireNotNull(message)
        assertNull(message.utm)
    }

    @Test
    fun `ignores malformed buttons`() {
        val message = PushMessageParser.parse(
            data = mapOf(
                "r_group_push" to "1",
                "push_id" to "app_42",
                "buttons" to "not-json",
            ),
        )

        requireNotNull(message)
        assertEquals(emptyList<PushButton>(), message.buttons)
    }
}
