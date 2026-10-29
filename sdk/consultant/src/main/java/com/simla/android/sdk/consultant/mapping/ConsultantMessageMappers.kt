package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.localization.ConsultantStrings
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.models.widget.WIDGET_MANAGER_DISPLAY_CUSTOM
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus
import com.simla.android.sdk.util.toIso8601EpochMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun fallbackMessageText(type: String?, strings: ConsultantStrings): String? {
    return when (type) {
        null, "text" -> null
        "product" -> strings.productMessage
        "order" -> strings.orderMessage
        "file", "image" -> strings.attachment
        else -> strings.unsupportedMessage
    }
}

internal fun resolveManagerAuthorName(
    config: ConsultantWidgetConfig?,
    strings: ConsultantStrings,
    fallbackName: String? = null,
): String {
    return config
        ?.takeIf { it.managerDisplayType == WIDGET_MANAGER_DISPLAY_CUSTOM }
        ?.managerName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: fallbackName?.trim()?.takeIf { it.isNotEmpty() }
        ?: strings.agent
}

internal fun resolveManagerAvatarUrl(
    config: ConsultantWidgetConfig?,
    fallbackAvatarUrl: String? = null,
): String? {
    return config
        ?.takeIf { it.managerDisplayType == WIDGET_MANAGER_DISPLAY_CUSTOM }
        ?.managerLogo
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: fallbackAvatarUrl?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun mapDeliveryStatus(status: String?): OnlineConsultantMessageDeliveryStatus {
    return when (status) {
        null, "new", "undefined", "sending" -> OnlineConsultantMessageDeliveryStatus.SENDING
        "sent", "delivered", "received" -> OnlineConsultantMessageDeliveryStatus.DELIVERED
        "seen" -> OnlineConsultantMessageDeliveryStatus.READ
        "failed" -> OnlineConsultantMessageDeliveryStatus.ERROR
        else -> OnlineConsultantMessageDeliveryStatus.NONE
    }
}

internal fun formatMessageTime(value: String?, strings: ConsultantStrings): String {
    if (value.isNullOrBlank()) {
        return strings.now
    }

    return runCatching {
        val epochMillis = requireNotNull(value.toIso8601EpochMillis())
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }.getOrElse {
        val timePart = value.substringAfter('T', missingDelimiterValue = value)
        timePart.take(5).takeIf { it.length == 5 && ':' in it } ?: strings.now
    }
}
