package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.graphics.Color

internal fun String?.normalizedQuoteText(): String? {
    return this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotEmpty() }
}

internal fun String?.toColorOrNull(): Color? {
    val normalized = this
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return null

    return runCatching {
        val value = normalized.toLong(16)
        Color(if (normalized.length == 6) 0xFF000000 or value else value)
    }.getOrNull()
}
