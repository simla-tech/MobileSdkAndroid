package com.simla.android.sdk.network

interface SdkWebSocketSession {
    fun close(code: Int, reason: String): Boolean
}
