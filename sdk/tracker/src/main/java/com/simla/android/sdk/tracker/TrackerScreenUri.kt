package com.simla.android.sdk.tracker

import java.net.URI

internal fun normalizeScreenUri(value: String): String {
    val candidate = value.trim()
    require(candidate.isNotEmpty()) { "screenUri must not be blank" }

    val uri = runCatching { URI(candidate) }.getOrNull()
    require(uri?.isAbsolute == true && !uri.scheme.isNullOrBlank()) {
        "screenUri must be an absolute URI"
    }
    return candidate
}
