package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.models.widget.WIDGET_MANAGER_DISPLAY_CUSTOM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsultantManagerAvatarTest {
    @Test
    fun custom_manager_logo_overrides_message_author_avatar() {
        val config = ConsultantWidgetConfig(
            managerDisplayType = WIDGET_MANAGER_DISPLAY_CUSTOM,
            managerLogo = " https://example.com/custom-manager.png ",
        )

        assertEquals(
            "https://example.com/custom-manager.png",
            resolveManagerAvatarUrl(
                config = config,
                fallbackAvatarUrl = "https://example.com/message-author.png",
            ),
        )
    }

    @Test
    fun message_author_avatar_is_used_for_standard_manager_display() {
        val config = ConsultantWidgetConfig(
            managerDisplayType = "standard",
            managerLogo = "https://example.com/ignored.png",
        )

        assertEquals(
            "https://example.com/message-author.png",
            resolveManagerAvatarUrl(
                config = config,
                fallbackAvatarUrl = " https://example.com/message-author.png ",
            ),
        )
    }

    @Test
    fun blank_avatar_urls_are_ignored() {
        val config = ConsultantWidgetConfig(
            managerDisplayType = WIDGET_MANAGER_DISPLAY_CUSTOM,
            managerLogo = "  ",
        )

        assertNull(resolveManagerAvatarUrl(config = config, fallbackAvatarUrl = " "))
    }
}
