package com.simla.android.sdk.consultant.models.file

import com.simla.android.sdk.consultant.serialization.asFlexibleString

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantFileLinkResponse(
    val id: JsonElement? = null,
    val url: String? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString()
}
