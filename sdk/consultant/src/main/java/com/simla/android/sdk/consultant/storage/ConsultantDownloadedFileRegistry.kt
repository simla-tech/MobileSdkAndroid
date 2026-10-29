package com.simla.android.sdk.consultant.storage

internal interface ConsultantDownloadedFileRegistry {
    suspend fun get(sourceUrl: String): ConsultantDownloadedFileRecord?

    suspend fun put(record: ConsultantDownloadedFileRecord)

    suspend fun remove(sourceUrl: String)
}
