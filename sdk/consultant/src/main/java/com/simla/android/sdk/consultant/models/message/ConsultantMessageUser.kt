package com.simla.android.sdk.consultant.models.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantMessageUser(
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    val avatar: String? = null,
) {
    val displayName: String?
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .trim()
            .ifBlank { null }
}
