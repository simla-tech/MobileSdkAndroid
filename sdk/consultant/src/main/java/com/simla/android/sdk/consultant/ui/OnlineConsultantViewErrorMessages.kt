package com.simla.android.sdk.consultant.ui

import android.content.Context
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.error.SdkHttpException

internal fun formatViewMessageSendErrorToast(context: Context, throwable: Throwable): String {
    return when (throwable) {
        is SdkHttpException -> {
            context.getString(R.string.crm_consultant_send_message_http_error, throwable.httpCode)
        }

        else -> throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.crm_consultant_send_message_error)
    }
}

internal fun formatViewInitialConnectErrorToast(context: Context, throwable: Throwable): String {
    return when (throwable) {
        is SdkHttpException -> {
            context.getString(R.string.crm_consultant_connect_http_error, throwable.httpCode)
        }

        else -> throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.crm_consultant_connect_error)
    }
}
