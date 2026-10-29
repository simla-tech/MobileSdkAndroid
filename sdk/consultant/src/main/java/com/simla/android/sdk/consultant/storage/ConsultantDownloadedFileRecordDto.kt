package com.simla.android.sdk.consultant.storage

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsultantDownloadedFileRecordDto(
    val sourceUrl: String,
    val fileName: String,
    val uri: String,
    val mimeType: String,
    val savedAtMillis: Long,
) {
    fun toRecord(): ConsultantDownloadedFileRecord {
        return ConsultantDownloadedFileRecord(
            sourceUrl = sourceUrl,
            fileName = fileName,
            uri = uri,
            mimeType = mimeType,
            savedAtMillis = savedAtMillis,
        )
    }

    companion object {
        fun fromRecord(record: ConsultantDownloadedFileRecord): ConsultantDownloadedFileRecordDto {
            return ConsultantDownloadedFileRecordDto(
                sourceUrl = record.sourceUrl,
                fileName = record.fileName,
                uri = record.uri,
                mimeType = record.mimeType,
                savedAtMillis = record.savedAtMillis,
            )
        }
    }
}
