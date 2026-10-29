package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageButton(
    val type: String? = null,
    val label: String? = null,
    val url: String? = null,
)
