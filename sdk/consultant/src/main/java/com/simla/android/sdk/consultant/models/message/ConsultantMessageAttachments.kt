package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageAttachments(
    val suggestions: List<ConsultantMessageSuggestion> = emptyList(),
    val links: List<ConsultantMessageButton> = emptyList(),
)
