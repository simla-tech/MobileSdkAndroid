package com.simla.android.sdk.session

data class SdkUserPayload(
    val token: String? = null,
    val language: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
)
