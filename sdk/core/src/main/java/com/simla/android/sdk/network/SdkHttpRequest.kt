package com.simla.android.sdk.network

data class SdkHttpRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
)
