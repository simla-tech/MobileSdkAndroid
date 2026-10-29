package com.simla.android.sdk.consultant.models.widget

internal const val WIDGET_LINK_TYPE_FB = "fb"

internal const val WIDGET_LINK_TYPE_FB_MESSENGER = "fbmessenger"

internal const val WIDGET_LINK_TYPE_WHATSAPP = "whatsapp"

internal const val WIDGET_LINK_TYPE_TELEGRAM = "telegram"

internal const val WIDGET_LINK_TYPE_VK = "vk"

internal const val WIDGET_LINK_TYPE_VIBER = "viber"

internal const val WIDGET_LINK_TYPE_INSTAGRAM = "instagram"

internal const val WIDGET_LINK_TYPE_MAX = "max"

internal val linkTypePrefixes = mapOf(
    WIDGET_LINK_TYPE_FB to "https://fb.me/",
    WIDGET_LINK_TYPE_FB_MESSENGER to "https://m.me/",
    WIDGET_LINK_TYPE_WHATSAPP to "https://wa.me/",
    WIDGET_LINK_TYPE_TELEGRAM to "https://t.me/",
    WIDGET_LINK_TYPE_VK to "https://vk.me/",
    WIDGET_LINK_TYPE_VIBER to "viber://pa?chatURI=",
    WIDGET_LINK_TYPE_INSTAGRAM to "https://instagram.com/",
    WIDGET_LINK_TYPE_MAX to "https://max.ru/",
)
