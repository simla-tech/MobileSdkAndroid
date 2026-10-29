package com.simla.android.sdk.network

data class SdkWebSocketRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
