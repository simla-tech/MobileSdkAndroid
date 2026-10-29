package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.graphics.Color
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsultantInitialLoadingColorTest {
    @Test
    fun config_is_not_loaded_uses_host_primary_color() {
        val hostPrimaryColor = Color(0xFFB85042)

        assertEquals(
            hostPrimaryColor,
            initialLoadingIndicatorColor(
                widgetConfig = null,
                hostPrimaryColor = hostPrimaryColor,
            ),
        )
    }

    @Test
    fun loaded_config_uses_consultant_brand_color() {
        val consultantColor = Color(0xFFFF6B2C)

        assertEquals(
            consultantColor,
            initialLoadingIndicatorColor(
                widgetConfig = ConsultantWidgetConfig(elementsColor = "#ff6b2c"),
                hostPrimaryColor = Color(0xFFB85042),
            ),
        )
    }
}
