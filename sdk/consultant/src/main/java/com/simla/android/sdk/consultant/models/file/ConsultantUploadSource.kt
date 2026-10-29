package com.simla.android.sdk.consultant.models.file

import android.net.Uri

internal data class ConsultantUploadSource(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val messageType: String,
    val bytes: ByteArray,
    val sizeBytes: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConsultantUploadSource

        if (uri != other.uri) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (messageType != other.messageType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (sizeBytes != other.sizeBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + sizeBytes.hashCode()
        return result
    }
}
