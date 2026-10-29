package com.simla.android.sdk.network

interface SdkWebSocketSessionListener {
    fun onOpen(response: SdkHttpResponse)

    fun onTextMessage(text: String)

    fun onBinaryMessage(payload: ByteArray)

    fun onClosing(code: Int, reason: String)

    fun onClosed(code: Int, reason: String)

    fun onFailure(
        throwable: Throwable,
        response: SdkHttpResponse?,
    )
}
