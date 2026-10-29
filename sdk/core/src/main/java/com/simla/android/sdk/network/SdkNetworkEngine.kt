package com.simla.android.sdk.network

interface SdkNetworkEngine : SdkHttpNetworkEngine {
    fun openWebSocket(
        request: SdkWebSocketRequest,
        listener: SdkWebSocketSessionListener,
    ): SdkWebSocketSession

    override fun close()
}
