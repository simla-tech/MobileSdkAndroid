package com.simla.android.sdk.consultant.models.message

import com.simla.android.sdk.consultant.serialization.asFlexibleString

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ConsultantMessageItem(
    val id: JsonElement? = null,
    val url: String? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    val src: String? = null,
    val type: String? = null,
    val caption: String? = null,
    val name: String? = null,
    val filename: String? = null,
    @SerialName("file_name")
    val fileName: String? = null,
    val size: Long? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    val idValue: String?
        get() = id.asFlexibleString()

    val imageUrl: String?
        get() = listOf(url, downloadUrl, src).firstOrNull { !it.isNullOrBlank() }

    val fileUrl: String?
        get() = listOf(downloadUrl, url, src).firstOrNull { !it.isNullOrBlank() }

    val displayFileName: String?
        get() = listOf(caption, name, filename, fileName, idValue)
            .firstOrNull { !it.isNullOrBlank() }

    val fileSizeBytes: Long?
        get() = fileSize ?: size

    val isImage: Boolean
        get() = type?.contains("image", ignoreCase = true) != false

    val isFile: Boolean
        get() = type?.contains("file", ignoreCase = true) == true
}
