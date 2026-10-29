package com.simla.android.sdk.tracker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TrackerUtm(
    @SerialName("utm_source")
    val source: String? = null,
    @SerialName("utm_medium")
    val medium: String? = null,
    @SerialName("utm_campaign")
    val campaign: String? = null,
    @SerialName("utm_keyword")
    val keyword: String? = null,
    @SerialName("utm_content")
    val content: String? = null,
)
