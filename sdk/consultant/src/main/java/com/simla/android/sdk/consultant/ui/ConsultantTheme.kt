package com.simla.android.sdk.consultant.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

internal val DefaultCrmVisualTheme = ConsultantVisualTheme(
    brandColor = Color(0xFF005EEB),
    brandPressedColor = Color(0xFF0052CC),
    brandDarkestColor = Color(0xFF0044B7),
    brandAlphaColor = Color(0x1F005EEB),
    screenBackground = Color.White,
    bottomBarColor = Color.White,
    consultantBubbleColor = Color(0xFFF4F6F8),
    visitorBubbleColor = Color(0xFF005EEB),
    mainTextColor = Color.White,
    mainIconColor = Color.White,
    primaryText = Color(0xFF1E2248),
    secondaryText = Color(0xFF636F7F),
    messageDateColor = Color(0xFF8A96A6),
    mainGrayColor = Color(0xFF636F7F),
    placeholderText = Color(0xFFAFB9C3),
    iconColor = Color(0xFF8A96A6),
    dividerColor = Color(0xFFDEE2E6),
    areaBackgroundColor = Color.White,
    areaTextColor = Color.Black,
    areaIconColor = Color(0xFFAFB9C3),
    areaBorderColor = Color(0xFFDEE2E6),
    areaHoverBorderColor = Color(0xFFDEE2E6),
    roundedElements = true,
)

internal val LocalCrmVisualTheme = compositionLocalOf { DefaultCrmVisualTheme }

internal val ConsultantBrandColor = Color(0xFF005EEB)

internal val ConsultantBrandPressedColor = Color(0xFF004BBF)

internal val OnlineConsultantBubbleColor = Color(0xFFEEF1F3)

internal val ConsultantVisitorBubbleColor = Color(0xFFE8F0FF)

internal val ConsultantBottomBarColor = Color.White

internal val ConsultantDividerColor = Color(0xFFDEE2E6)

internal val ConsultantDisabledBackground = Color(0xFFDEE2E6)

internal val ConsultantPersonalDataWarningBackground = Color(0xFFFFF7E6)

internal val ConsultantPrimaryText = Color(0xFF1E2248)

internal val ConsultantSecondaryText = Color(0xFF636F7F)

internal val ConsultantTimeText = Color(0xFF8A96A6)

internal val ConsultantMetaOverlayColor = Color(0x801A1A1A)

internal val ConsultantQuoteBarColor = Color(0x78636F7F)

internal val ConsultantProductLinkPressedBackground = Color(0x1F005EEB)

internal val ConsultantProductPlaceholderColor = Color(0xFFD9DFE7)

internal val ConsultantProductPlaceholderIconColor = Color(0xFF8A96A6)

internal val ConsultantPlaceholderText = Color(0xFFAFB9C3)

internal val ConsultantIconColor = Color(0xFF8A96A6)

internal val ConsultantVisitorAvatarBg = Color(0xFF2B2F5B)

internal val ConsultantAvatarBorder = Color(0xFFD9DFE7)

internal val ConsultantBotIconTint = Color(0xFF2B2F5B)

internal val OnlineConsultantIconTint = Color(0xFF5E6B7D)

internal val ConsultantStatusSending = Color(0xFFB6BDC7)

internal val ConsultantStatusDelivered = Color(0xFF9EA7B3)

internal val ConsultantStatusRead = Color(0xFF24B37A)

internal val ConsultantStatusError = Color(0xFFFF5C56)
