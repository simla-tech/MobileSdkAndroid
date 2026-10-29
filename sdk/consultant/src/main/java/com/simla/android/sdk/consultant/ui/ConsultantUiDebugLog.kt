package com.simla.android.sdk.consultant.ui

import android.util.Log
import com.simla.android.sdk.consultant.BuildConfig

internal object ConsultantUiDebugLog {
    private const val TAG = "OnlineConsultantSDK"
    val isEnabled: Boolean = BuildConfig.ENABLE_CONSULTANT_UI_TRACE

    fun d(message: String) {
        if (isEnabled) {
            Log.d(TAG, "[messages-ui] $message")
        }
    }
}
