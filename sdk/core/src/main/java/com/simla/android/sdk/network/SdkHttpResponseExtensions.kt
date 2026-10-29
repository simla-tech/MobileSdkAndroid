package com.simla.android.sdk.network

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SdkHttpResponse.retryAfterMsOrNull(): Long? {
    val headerValue = headers.entries
        .firstOrNull { (key, _) -> key.equals("Retry-After", ignoreCase = true) }
        ?.value
        ?.firstOrNull()
        ?.trim()
        ?: return null

    return headerValue.toLongOrNull()?.times(1000)
}
