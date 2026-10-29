package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.graphics.Color
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.models.widget.WIDGET_BACKGROUND_DARK
import com.simla.android.sdk.consultant.models.widget.WIDGET_ELEMENT_SHAPE_RECTANGULAR

internal fun ConsultantWidgetConfig?.toVisualTheme(): ConsultantVisualTheme {
    this ?: return DefaultCrmVisualTheme
    val brandColor = elementsColor.toColorOrNull() ?: DefaultCrmVisualTheme.brandColor
    val mainTextColor = if (inverse) Color(0xFF1E2248) else Color.White
    val darkTheme = backgroundTheme == WIDGET_BACKGROUND_DARK
    return DefaultCrmVisualTheme.copy(
        brandColor = brandColor,
        brandPressedColor = brandColor.darken(0.05f),
        brandDarkestColor = brandColor.darken(0.10f),
        brandAlphaColor = brandColor.copy(alpha = 0.12f),
        screenBackground = if (darkTheme) Color(0xFF0A0A0A) else Color.White,
        bottomBarColor = if (darkTheme) Color(0xFF0A0A0A) else Color.White,
        consultantBubbleColor = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color(0xFFF4F6F8),
        visitorBubbleColor = brandColor,
        mainTextColor = mainTextColor,
        mainIconColor = mainTextColor,
        primaryText = if (darkTheme) Color.White else Color(0xFF1E2248),
        secondaryText = if (darkTheme) Color(0xFFAFB9C3) else Color(0xFF636F7F),
        messageDateColor = if (darkTheme) Color(0xFFAFB9C3) else Color(0xFF8A96A6),
        mainGrayColor = if (darkTheme) Color(0xFFAFB9C3) else Color(0xFF636F7F),
        placeholderText = Color(0xFFAFB9C3),
        iconColor = if (darkTheme) Color.White.copy(alpha = 0.40f) else Color(0xFFAFB9C3),
        dividerColor = if (darkTheme) Color.Transparent else Color(0xFFDEE2E6),
        areaBackgroundColor = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color.White,
        areaTextColor = if (darkTheme) Color.White else Color.Black,
        areaIconColor = if (darkTheme) Color.White.copy(alpha = 0.40f) else Color(0xFFAFB9C3),
        areaBorderColor = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color(0xFFDEE2E6),
        areaHoverBorderColor = if (darkTheme) Color.White.copy(alpha = 0.20f) else Color(0xFFDEE2E6),
        roundedElements = elementShape != WIDGET_ELEMENT_SHAPE_RECTANGULAR,
    )
}

internal fun initialLoadingIndicatorColor(
    widgetConfig: ConsultantWidgetConfig?,
    hostPrimaryColor: Color,
): Color = widgetConfig?.toVisualTheme()?.brandColor ?: hostPrimaryColor
