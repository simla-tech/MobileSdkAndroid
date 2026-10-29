package com.simla.android.sdk.consultant.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.simla.android.sdk.consultant.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

internal object ConsultantImageFileActions {
    private const val CACHE_SUBDIRECTORY = "crm-consultant/images"

    suspend fun findDownloadedImage(
        context: Context,
        imageUrl: String,
    ): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext null
        }

        val fileName = imageFileName(imageUrl)
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
        )
        val selection =
            "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(
            fileName,
            "${Environment.DIRECTORY_DOWNLOADS}/",
        )

        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@withContext null
            }

            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            Uri.withAppendedPath(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                cursor.getLong(idIndex).toString(),
            )
        }
    }

    suspend fun saveImageToDownloads(
        context: Context,
        imageUrl: String,
    ): Uri = withContext(Dispatchers.IO) {
        findDownloadedImage(context, imageUrl)?.let { return@withContext it }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext shareCacheUri(context, imageUrl)
        }

        val fileName = imageFileName(imageUrl)
        val mimeType = mimeType(fileName)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS,
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error(context.getString(R.string.crm_consultant_download_image_error))

        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                openImageInput(context, imageUrl).use { input ->
                    input.copyTo(output)
                }
            } ?: error(context.getString(R.string.crm_consultant_download_image_error))
        }.onFailure {
            resolver.delete(uri, null, null)
            throw it
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        uri
    }

    suspend fun shareCacheUri(
        context: Context,
        imageUrl: String,
    ): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY).apply { mkdirs() }
        val file = File(directory, imageFileName(imageUrl))
        if (!file.exists() || file.length() == 0L) {
            file.outputStream().use { output ->
                openImageInput(context, imageUrl).use { input ->
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

    fun mimeType(imageUrl: String): String {
        val extension = imageUrl
            .substringBefore('?')
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)
            .takeIf { it.isNotBlank() }

        return extension
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: "image/jpeg"
    }

    fun imageFileName(imageUrl: String): String {
        val rawName = runCatching {
            imageUrl.toUri().lastPathSegment
        }.getOrNull()
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?.substringBefore('?')
            ?.takeIf { it.contains('.') }
            ?: "crm-image-${imageUrl.hashCode().toUInt()}.jpg"

        return rawName.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }

    private fun openImageInput(
        context: Context,
        imageUrl: String,
    ) = when {
        imageUrl.startsWith("http://", ignoreCase = true) ||
                imageUrl.startsWith("https://", ignoreCase = true) -> {
            URL(imageUrl).openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }.getInputStream()
        }

        else -> {
            context.contentResolver.openInputStream(imageUrl.toUri())
                ?: error(context.getString(R.string.crm_consultant_download_image_error))
        }
    }
}
