package com.simla.android.sdk.consultant.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRecord
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

internal object ConsultantFileActions {
    private const val CACHE_SUBDIRECTORY = "crm-consultant/files/tmp"

    suspend fun findDownloadedFile(
        context: Context,
        registry: ConsultantDownloadedFileRegistry,
        fileUrl: String,
    ): ConsultantDownloadedFileRecord? = withContext(Dispatchers.IO) {
        val record = registry.get(fileUrl) ?: return@withContext null
        val uri = record.uri.toUri()
        val exists = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length != 0L
            } ?: false
        }.getOrDefault(false)

        if (exists) {
            record
        } else {
            registry.remove(fileUrl)
            null
        }
    }

    suspend fun saveFileToCache(
        context: Context,
        fileUrl: String,
        fileName: String?,
    ): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY).apply { mkdirs() }
        val file = File(directory, safeFileName(fileUrl, fileName))
        if (!file.exists() || file.length() == 0L) {
            file.outputStream().use { output ->
                openFileInput(context, fileUrl).use { input ->
                    input.copyTo(output)
                }
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.crm_consultant_file_provider",
            file,
        )
    }

    suspend fun saveFileToDownloads(
        context: Context,
        registry: ConsultantDownloadedFileRegistry,
        fileUrl: String,
        fileName: String?,
    ): ConsultantDownloadedFileRecord = withContext(Dispatchers.IO) {
        findDownloadedFile(context, registry, fileUrl)?.let { return@withContext it }

        val resolvedFileName = safeFileName(fileUrl, fileName)
        val mimeType = mimeType(resolvedFileName)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveFileToMediaStoreDownloads(
                context = context,
                fileUrl = fileUrl,
                fileName = resolvedFileName,
                mimeType = mimeType,
            )
        } else {
            saveFileToLegacyDownloads(
                context = context,
                fileUrl = fileUrl,
                fileName = resolvedFileName,
            )
        }

        ConsultantDownloadedFileRecord(
            sourceUrl = fileUrl,
            fileName = resolvedFileName,
            uri = uri.toString(),
            mimeType = mimeType,
            savedAtMillis = System.currentTimeMillis(),
        ).also { registry.put(it) }
    }

    fun mimeType(fileName: String): String {
        val extension = fileName
            .substringBefore('?')
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)
            .takeIf { it.isNotBlank() }

        return extension
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: "application/octet-stream"
    }

    fun safeFileName(fileUrl: String, fileName: String?): String {
        val rawName = fileName
            ?.takeIf { it.isNotBlank() }
            ?: runCatching {
                fileUrl.toUri().lastPathSegment
            }.getOrNull()
                ?.let { URLDecoder.decode(it, "UTF-8") }
                ?.substringBefore('?')
                ?.takeIf { it.isNotBlank() }
            ?: "crm-file-${fileUrl.hashCode().toUInt()}"

        return rawName.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileToMediaStoreDownloads(
        context: Context,
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error(context.getString(R.string.crm_consultant_file_upload_error))

        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                openFileInput(context, fileUrl).use { input ->
                    input.copyTo(output)
                }
            } ?: error(context.getString(R.string.crm_consultant_file_upload_error))
        }.onFailure {
            resolver.delete(uri, null, null)
            throw it
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveFileToLegacyDownloads(
        context: Context,
        fileUrl: String,
        fileName: String,
    ): Uri {
        val directory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS,
        ).apply { mkdirs() }
        val file = File(directory, fileName)
        if (!file.exists() || file.length() == 0L) {
            file.outputStream().use { output ->
                openFileInput(context, fileUrl).use { input ->
                    input.copyTo(output)
                }
            }
        }
        return Uri.fromFile(file)
    }

    private fun openFileInput(
        context: Context,
        fileUrl: String,
    ) = when {
        fileUrl.startsWith("http://", ignoreCase = true) ||
                fileUrl.startsWith("https://", ignoreCase = true) -> {
            URL(fileUrl).openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }.getInputStream()
        }

        else -> {
            context.contentResolver.openInputStream(fileUrl.toUri())
                ?: error(context.getString(R.string.crm_consultant_no_file_handler))
        }
    }
}
