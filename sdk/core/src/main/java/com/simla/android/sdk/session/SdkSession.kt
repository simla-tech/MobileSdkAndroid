package com.simla.android.sdk.session

data class SdkSession(
    val userToken: String,
    val sessionId: String,
    val siteToken: String,
    val platformName: String,
)
