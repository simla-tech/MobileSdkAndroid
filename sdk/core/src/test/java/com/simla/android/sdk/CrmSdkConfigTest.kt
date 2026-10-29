package com.simla.android.sdk

import com.simla.android.sdk.core.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class CrmSdkConfigTest {
    @Test
    fun `uses publisher-specific default base URL`() {
        val expectedBaseUrl = when (BuildConfig.FLAVOR) {
            "simla" -> "https://c.simla.com"
            "retailcrm" -> "https://c.retailcrm.tech"
            else -> error("Unexpected publisher flavour: ${BuildConfig.FLAVOR}")
        }

        assertEquals(expectedBaseUrl, CrmSdkConfig(siteToken = "site-token").baseUrl)
    }
}
