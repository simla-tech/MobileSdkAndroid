package com.simla.android.sdk.error

class SdkHttpException(
    message: String,
    val httpCode: Int,
    val responseBody: String,
    cause: Throwable? = null,
) : CrmSdkException(message, cause)
