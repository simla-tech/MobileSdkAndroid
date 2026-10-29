package com.simla.android.sdk.consultant.storage

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.simla.android.sdk.consultant.serialization.SdkJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

internal class ConsultantSharedPreferencesDownloadedFileRegistry(
    context: Context,
) : ConsultantDownloadedFileRegistry {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun get(sourceUrl: String): ConsultantDownloadedFileRecord? =
        withContext(Dispatchers.IO) {
            preferences.getString(key(sourceUrl), null)
                ?.let { encoded ->
                    runCatching {
                        SdkJson.decodeFromString<ConsultantDownloadedFileRecordDto>(encoded)
                    }.getOrNull()
                }
                ?.toRecord()
        }

    override suspend fun put(record: ConsultantDownloadedFileRecord) {
        withContext(Dispatchers.IO) {
            preferences.edit {
                putString(
                    key(record.sourceUrl),
                    SdkJson.encodeToString(
                        ConsultantDownloadedFileRecordDto.fromRecord(
                            record
                        )
                    ),
                )
            }
        }
    }

    override suspend fun remove(sourceUrl: String) {
        withContext(Dispatchers.IO) {
            preferences.edit { remove(key(sourceUrl)) }
        }
    }

    private fun key(sourceUrl: String): String {
        val encodedUrl = Base64.encodeToString(
            sourceUrl.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return "downloaded_file_$encodedUrl"
    }

    companion object {
        private const val PREFERENCES_NAME = "crm_consultant_downloaded_files"
    }
}
