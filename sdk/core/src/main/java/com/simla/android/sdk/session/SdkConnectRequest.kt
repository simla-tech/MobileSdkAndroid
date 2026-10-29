package com.simla.android.sdk.session

data class SdkConnectRequest(
    val user: SdkUserPayload = SdkUserPayload(),
    val pageTitle: String? = null,
    val url: String? = null,
    val referUrl: String? = null,
)
