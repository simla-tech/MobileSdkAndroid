package com.simla.android.sdk.consultant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FooterBrandingTest {
    @Test
    fun normalizes_supported_footer_urls() {
        assertEquals("https://example.com", consultantFooterTargetUrl(" https://example.com "))
        assertEquals("https://example.com", consultantFooterTargetUrl("//example.com"))
        assertNull(consultantFooterTargetUrl("example.com"))
        assertNull(consultantFooterTargetUrl(" "))
    }

    @Test
    fun selects_dark_logo_variant() {
        assertEquals(
            "https://example.com/logo_horiz_white.svg",
            consultantFooterLogoUrl("https://example.com/logo_horiz.svg", darkTheme = true),
        )
        assertEquals(
            "https://example.com/logo_horiz.svg",
            consultantFooterLogoUrl("https://example.com/logo_horiz.svg", darkTheme = false),
        )
    }

    @Test
    fun derives_fallback_brand_from_domain() {
        assertEquals("Crm", consultantFooterBrandName("https://www.crm.com"))
        assertEquals("retailCRM", consultantFooterBrandName("https://www.retailcrm.ru"))
    }
}
