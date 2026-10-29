package com.simla.android.sdk.network

data class SdkHttpResponse(
    val code: Int,
    val message: String? = null,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String = "",
) {
    val isSuccessful: Boolean
        get() = code in 200..299
}
