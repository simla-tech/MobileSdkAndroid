package com.simla.android.sdk.network

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.withQueryParameters(vararg parameters: Pair<String, String>): String {
    if (parameters.isEmpty()) return this

    val fragmentIndex = indexOf('#')
    val url = if (fragmentIndex >= 0) substring(0, fragmentIndex) else this
    val fragment = if (fragmentIndex >= 0) substring(fragmentIndex) else ""
    val separator = when {
        '?' !in url -> "?"
        url.endsWith('?') || url.endsWith('&') -> ""
        else -> "&"
    }
    val query = parameters.joinToString("&") { (name, value) ->
        "${name.encodeQueryComponent()}=${value.encodeQueryComponent()}"
    }

    return "$url$separator$query$fragment"
}

private fun String.encodeQueryComponent(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")
