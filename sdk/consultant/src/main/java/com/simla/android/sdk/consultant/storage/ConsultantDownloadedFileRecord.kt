package com.simla.android.sdk.consultant.storage

internal data class ConsultantDownloadedFileRecord(
    val sourceUrl: String,
    val fileName: String,
    val uri: String,
    val mimeType: String,
    val savedAtMillis: Long,
)
