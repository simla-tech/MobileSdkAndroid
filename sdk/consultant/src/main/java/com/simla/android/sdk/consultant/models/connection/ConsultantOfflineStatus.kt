package com.simla.android.sdk.consultant.models.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantOfflineStatus(
    @SerialName("is_offline")
    val isOffline: Boolean = false,
)
