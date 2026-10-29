package com.simla.android.sdk.network

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import okhttp3.Request

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Request.Builder.headers(headers: Map<String, String>): Request.Builder = apply {
    headers.forEach { (name, value) -> addHeader(name, value) }
}
