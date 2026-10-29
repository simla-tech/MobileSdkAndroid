package com.simla.android.sdk.consultant

import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.serialization.SdkJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigDecodeTest {
    @Test
    fun decodes_visual_widget_settings_from_connect_config() {
        val config = SdkJson.decodeFromString<ConsultantWidgetConfig>(
            """
            {
              "name": "Support chat",
              "position": "left-bottom",
              "bg_color": "dark",
              "inverse": true,
              "element_shape": "rectangular form",
              "logo_type": "custom",
              "logo_url": "https://example.com/widget.png",
              "logo_stretch": true,
              "manager_display_type": "custom",
              "manager_name": "Anna",
              "manager_logo": "https://example.com/manager.png",
              "elements_color": "FF5500",
              "use_sound_alerts": true,
              "use_unread_counter": false,
              "always_show_links_on_desktops": true,
              "links": [
                { "type": "telegram", "url": "crm", "prefix": "teleg.run/" },
                { "type": "whatsapp", "url": "79990000000" }
              ],
              "links_mode": "black",
              "links_orientation": "horizontal",
              "domain_config": {
                "URL": "https://www.retailcrm.ru",
                "logo": "https://example.com/logo_horiz.svg",
                "free": true,
                "footer": true,
                "image_workers": ["https://images.example.com"]
              },
              "z_index": 123,
              "offset": { "bottom": 20, "side": 20 }
            }
            """.trimIndent(),
        )

        assertEquals("Support chat", config.name)
        assertEquals("left-bottom", config.position)
        assertEquals("dark", config.backgroundTheme)
        assertTrue(config.inverse)
        assertEquals("rectangular form", config.elementShape)
        assertEquals("custom", config.logoType)
        assertEquals("https://example.com/widget.png", config.logoUrl)
        assertTrue(config.logoStretch)
        assertEquals("custom", config.managerDisplayType)
        assertEquals("Anna", config.managerName)
        assertEquals("https://example.com/manager.png", config.managerLogo)
        assertEquals("FF5500", config.elementsColor)
        assertTrue(config.useSoundAlerts)
        assertFalse(config.useUnreadCounter)
        assertTrue(config.alwaysShowLinksOnDesktops)
        assertEquals(2, config.links.size)
        assertEquals("telegram", config.links[0].type)
        assertEquals("crm", config.links[0].url)
        assertEquals("teleg.run/", config.links[0].prefix)
        assertEquals("black", config.linksMode)
        assertEquals("horizontal", config.linksOrientation)
        assertEquals("https://www.retailcrm.ru", config.domainConfig?.url)
        assertEquals("https://example.com/logo_horiz.svg", config.domainConfig?.logo)
        assertTrue(config.domainConfig?.free == true)
        assertTrue(config.domainConfig?.footer == true)
        assertEquals(listOf("https://images.example.com"), config.domainConfig?.imageWorkers)
    }

    @Test
    fun uses_mobile_defaults_when_visual_settings_are_absent() {
        val config = SdkJson.decodeFromString<ConsultantWidgetConfig>("{}")

        assertEquals(null, config.name)
        assertEquals(null, config.position)
        assertEquals(null, config.backgroundTheme)
        assertFalse(config.inverse)
        assertEquals(null, config.elementShape)
        assertEquals(null, config.logoType)
        assertEquals(null, config.logoUrl)
        assertFalse(config.logoStretch)
        assertEquals(null, config.managerDisplayType)
        assertEquals(null, config.managerName)
        assertEquals(null, config.managerLogo)
        assertEquals(null, config.elementsColor)
        assertFalse(config.useSoundAlerts)
        assertTrue(config.useUnreadCounter)
        assertFalse(config.alwaysShowLinksOnDesktops)
        assertTrue(config.links.isEmpty())
        assertEquals(null, config.linksMode)
        assertEquals(null, config.linksOrientation)
        assertEquals(null, config.domainConfig)
    }

    @Test
    fun builds_widget_link_urls_for_supported_channels() {
        val config = SdkJson.decodeFromString<ConsultantWidgetConfig>(
            """
            {
              "links": [
                { "type": "telegram", "url": "crm", "prefix": "teleg.run/" },
                { "type": "whatsapp", "url": "79990000000" },
                { "type": "fbmessenger", "url": "crm" },
                { "type": "unknown", "url": "https://example.com/chat" }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("https://teleg.run/crm", config.links[0].buildUrl())
        assertEquals("https://wa.me/79990000000", config.links[1].buildUrl())
        assertEquals("whatsapp://send?phone=79990000000", config.links[1].buildUrl(isMobile = true))
        assertEquals("https://m.me/crm", config.links[2].buildUrl())
        assertEquals("https://example.com/chat", config.links[3].buildUrl())
    }

    @Test
    fun decodes_and_serializes_marketing_consent() {
        val config = SdkJson.decodeFromString<ConsultantWidgetConfig>(
            """
            {
              "contact_form": {
                "marketingConsent": { "required": true }
              },
              "offline_form": {
                "marketingConsent": { "required": false }
              }
            }
            """.trimIndent(),
        )

        assertTrue(config.contactForm.marketingConsent?.required == true)
        assertFalse(config.offlineForm.marketingConsent?.required == true)
        assertEquals(
            "{}",
            SdkJson.encodeToString(ConsultantContactFormRequest()),
        )
        assertEquals(
            """{"marketingConsent":false}""",
            SdkJson.encodeToString(
                ConsultantContactFormRequest(marketingConsent = false),
            ),
        )
        assertEquals(
            """{"marketingConsent":true}""",
            SdkJson.encodeToString(
                ConsultantContactFormRequest(marketingConsent = true),
            ),
        )
    }
}
