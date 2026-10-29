package com.simla.android.sdk.consultant.models.widget

import com.simla.android.sdk.consultant.models.util.hasScheme
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantWidgetLink(
    val type: String,
    val url: String,
    val prefix: String? = null,
) {
    fun buildUrl(isMobile: Boolean = false): String {
        if (url.hasScheme()) {
            return url
        }

        val resolvedPrefix = when {
            !prefix.isNullOrBlank() -> "https://$prefix"
            isMobile && type == WIDGET_LINK_TYPE_WHATSAPP -> "whatsapp://send?phone="
            else -> linkTypePrefixes[type]
        }

        return if (resolvedPrefix == null) {
            url
        } else {
            resolvedPrefix + url
        }
    }
}
