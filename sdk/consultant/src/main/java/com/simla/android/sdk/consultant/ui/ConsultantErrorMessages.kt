package com.simla.android.sdk.consultant.ui

import android.content.Context
import android.widget.Toast
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.error.OnlineConsultantUploadSizeException
import com.simla.android.sdk.error.SdkHttpException

internal fun showUploadSizeErrorToast(
    context: android.content.Context,
    throwable: Throwable,
): Boolean {
    if (throwable !is OnlineConsultantUploadSizeException) {
        return false
    }

    val message = throwable.message ?: return false

    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    return true
}

internal fun formatMessageSendErrorToast(context: Context, throwable: Throwable): String {
    return when (throwable) {
        is SdkHttpException -> {
            context.getString(R.string.crm_consultant_send_message_http_error, throwable.httpCode)
        }

        else -> throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.crm_consultant_send_message_error)
    }
}

internal fun formatInitialConnectErrorToast(context: Context, throwable: Throwable): String {
    return when (throwable) {
        is SdkHttpException -> {
            context.getString(R.string.crm_consultant_connect_http_error, throwable.httpCode)
        }

        else -> throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.crm_consultant_connect_error)
    }
}
