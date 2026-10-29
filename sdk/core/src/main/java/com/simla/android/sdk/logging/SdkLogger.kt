package com.simla.android.sdk.logging

interface SdkLogger {
    fun d(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
