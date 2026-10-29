package com.simla.android.sdk.recommendations

import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkRawHttpRequest

internal class RecordingRecommendationNetworkEngine(
    private val response: SdkHttpResponse,
) : SdkHttpNetworkEngine {
    lateinit var request: SdkHttpRequest

    override suspend fun postJson(request: SdkHttpRequest): SdkHttpResponse {
        this.request = request
        return response
    }

    override suspend fun postRaw(request: SdkRawHttpRequest) = error("not expected")

    override suspend fun getJson(request: SdkHttpRequest) = error("not expected")

    override fun close() = Unit
}
