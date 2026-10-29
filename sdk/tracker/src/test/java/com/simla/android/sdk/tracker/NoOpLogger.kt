package com.simla.android.sdk.tracker

import com.simla.android.sdk.logging.SdkLogger

internal object NoOpLogger : SdkLogger {
    override fun d(message: String) = Unit
    override fun e(message: String, throwable: Throwable?) = Unit
}
