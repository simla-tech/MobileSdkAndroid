package com.simla.android.sdk.consultant.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.simla.android.sdk.consultant.R

internal fun openFileUri(
    context: android.content.Context,
    uri: Uri,
    mimeType: String,
) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        val chooser = Intent.createChooser(
            intent,
            context.getString(R.string.crm_consultant_open_file),
        ).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    } catch (throwable: ActivityNotFoundException) {
        throw IllegalStateException(
            context.getString(R.string.crm_consultant_no_file_handler),
            throwable,
        )
    }
}

internal fun openDownloads(context: android.content.Context) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    } catch (throwable: ActivityNotFoundException) {
        throw IllegalStateException(
            context.getString(R.string.crm_consultant_open_downloads_error),
            throwable,
        )
    }
}
