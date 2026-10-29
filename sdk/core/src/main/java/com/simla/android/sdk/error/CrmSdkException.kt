package com.simla.android.sdk.error

open class CrmSdkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
