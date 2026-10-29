package com.simla.android.sdk.network

interface SdkHttpNetworkEngine {
    suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse

    suspend fun postRaw(request: SdkRawHttpRequest): SdkHttpResponse

    suspend fun getJson(request: SdkHttpRequest): SdkHttpResponse

    fun close()
}
