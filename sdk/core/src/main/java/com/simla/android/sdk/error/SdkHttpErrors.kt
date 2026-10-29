package com.simla.android.sdk.error

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import com.simla.android.sdk.network.SdkHttpResponse

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun sdkHttpException(
    prefix: String,
    response: SdkHttpResponse,
): SdkHttpException {
    return SdkHttpException(
        message = "$prefix with HTTP ${response.code}: ${response.body}",
        httpCode = response.code,
        responseBody = response.body,
    )
}
