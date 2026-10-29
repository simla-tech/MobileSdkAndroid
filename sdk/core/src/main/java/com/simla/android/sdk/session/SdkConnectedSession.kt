package com.simla.android.sdk.session

data class SdkConnectedSession(
    val id: String? = null,
) {
    val idValue: String?
        get() = id
}
