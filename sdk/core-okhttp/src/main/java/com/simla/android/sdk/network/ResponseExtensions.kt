package com.simla.android.sdk.network

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import okhttp3.Response

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Response.toSdkHttpResponse(
    body: String = this.body?.string().orEmpty(),
): SdkHttpResponse {
    return SdkHttpResponse(
        code = code,
        message = message,
        headers = headers.toMultimap(),
        body = body,
    )
}
