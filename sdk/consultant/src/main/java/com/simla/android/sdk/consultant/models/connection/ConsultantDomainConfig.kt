package com.simla.android.sdk.consultant.models.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantDomainConfig(
    @SerialName("URL")
    val url: String? = null,
    val logo: String? = null,
    val free: Boolean = false,
    val footer: Boolean = false,
    @SerialName("image_workers")
    val imageWorkers: List<String> = emptyList(),
)
