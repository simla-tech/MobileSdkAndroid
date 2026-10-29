package com.simla.android.sdk.notifications.status

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PushStatusRequest(
    @SerialName("push_id")
    val pushId: String,
    val status: String,
    @SerialName("changed_at")
    val changedAt: Long,
    @SerialName("button_hash")
    val buttonHash: String? = null,
)
