package com.simla.android.sdk.consultant.upload

import android.webkit.MimeTypeMap
import com.simla.android.sdk.consultant.localization.ConsultantStrings
import java.util.Locale

internal fun resolveUploadMimeType(
    declaredMimeType: String?,
    fileName: String,
    bytes: ByteArray,
): String {
    val extensionMimeType = mimeTypeFromFileName(fileName)
    val magicMimeType = mimeTypeFromImageSignature(bytes)
    val normalizedDeclared = declaredMimeType
        ?.takeIf { it.isNotBlank() && it != "application/octet-stream" }

    return when {
        normalizedDeclared?.startsWith("image/", ignoreCase = true) == true -> normalizedDeclared
        extensionMimeType?.startsWith("image/", ignoreCase = true) == true -> extensionMimeType
        magicMimeType != null -> magicMimeType
        normalizedDeclared != null -> normalizedDeclared
        extensionMimeType != null -> extensionMimeType
        else -> "application/octet-stream"
    }
}

internal fun isImageUpload(
    mimeType: String,
    fileName: String,
    bytes: ByteArray,
): Boolean {
    return mimeType.startsWith("image/", ignoreCase = true) ||
            mimeTypeFromFileName(fileName)?.startsWith("image/", ignoreCase = true) == true ||
            mimeTypeFromImageSignature(bytes) != null
}

internal fun mimeTypeFromFileName(fileName: String): String? {
    val extension = fileName
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.isNotBlank() }

    return extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
}

internal fun mimeTypeFromImageSignature(bytes: ByteArray): String? {
    return when {
        bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> "image/jpeg"

        bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() &&
                bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() &&
                bytes[7] == 0x0A.toByte() -> "image/png"

        bytes.size >= 6 &&
                bytes[0] == 0x47.toByte() &&
                bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() &&
                bytes[3] == 0x38.toByte() &&
                (bytes[4] == 0x37.toByte() || bytes[4] == 0x39.toByte()) &&
                bytes[5] == 0x61.toByte() -> "image/gif"

        bytes.size >= 12 &&
                bytes[0] == 0x52.toByte() &&
                bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() &&
                bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() &&
                bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() &&
                bytes[11] == 0x50.toByte() -> "image/webp"

        else -> null
    }
}

internal fun uploadSizeError(
    messageType: String,
    maxSizeBytes: Long,
    strings: ConsultantStrings,
): String {
    return strings.uploadSizeError(messageType == "image", maxSizeBytes / 1024 / 1024)
}
