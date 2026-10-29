package com.simla.android.sdk.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SdkUrlExtensionsTest {
    @Test
    fun `appends encoded parameters to an existing query before fragment`() {
        assertEquals(
            "https://example.test/path?existing=1&user=user%20token&redirect=a%2Fb#section",
            "https://example.test/path?existing=1#section".withQueryParameters(
                "user" to "user token",
                "redirect" to "a/b",
            ),
        )
    }

    @Test
    fun `returns the original URL when there are no parameters`() {
        assertEquals(
            "https://example.test/path#section",
            "https://example.test/path#section".withQueryParameters(),
        )
    }
}
