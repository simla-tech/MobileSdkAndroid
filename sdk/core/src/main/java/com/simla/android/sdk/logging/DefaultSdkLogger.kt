package com.simla.android.sdk.logging

import android.util.Log

class DefaultSdkLogger(
    private val enabled: Boolean,
) : SdkLogger {
    private val tag = "SimlaSDK"

    override fun d(message: String) {
        if (enabled) {
            Log.d(tag, message)
        }
    }

    override fun e(message: String, throwable: Throwable?) {
        if (enabled) {
            Log.e(tag, message, throwable)
        }
    }
}
