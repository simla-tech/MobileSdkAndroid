package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageSuggestion(
    val type: String? = null,
    val title: String? = null,
)
