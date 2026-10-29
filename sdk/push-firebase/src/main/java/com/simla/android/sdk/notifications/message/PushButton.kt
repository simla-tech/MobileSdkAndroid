package com.simla.android.sdk.notifications.message

import kotlinx.serialization.Serializable

@Serializable
internal data class PushButton(
    val title: String,
    val value: String,
)
