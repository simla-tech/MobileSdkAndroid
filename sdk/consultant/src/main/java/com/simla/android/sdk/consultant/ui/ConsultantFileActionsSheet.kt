package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRecord
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.icons.Download
import com.simla.android.sdk.consultant.ui.icons.FolderOpen
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileMessageActionsSheet(
    message: OnlineConsultantMessageUiModel,
    downloadedFileRegistry: ConsultantDownloadedFileRegistry,
    onDismiss: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileUrl = message.fileUrl
    val fileName = message.fileName
    var downloadedRecord by remember(fileUrl) {
        mutableStateOf<ConsultantDownloadedFileRecord?>(null)
    }
    var isHandlingAction by remember { mutableStateOf(false) }

    LaunchedEffect(fileUrl, downloadedFileRegistry) {
        downloadedRecord = fileUrl?.let {
            ConsultantFileActions.findDownloadedFile(
                context = context,
                registry = downloadedFileRegistry,
                fileUrl = it,
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FileActionRow(
                icon = ConsultantIcons.FolderOpen,
                label = stringResource(R.string.crm_consultant_open),
                enabled = fileUrl != null && !isHandlingAction,
                onClick = {
                    val resolvedFileUrl = fileUrl ?: return@FileActionRow
                    scope.launch {
                        isHandlingAction = true
                        runCatching {
                            val uri = ConsultantFileActions.saveFileToCache(
                                context = context,
                                fileUrl = resolvedFileUrl,
                                fileName = fileName,
                            )
                            openFileUri(
                                context = context,
                                uri = uri,
                                mimeType = ConsultantFileActions.mimeType(
                                    ConsultantFileActions.safeFileName(resolvedFileUrl, fileName),
                                ),
                            )
                            onDismiss()
                        }.onFailure(onError)
                        isHandlingAction = false
                    }
                },
            )

            val savedRecord = downloadedRecord
            FileActionRow(
                icon = if (savedRecord == null) ConsultantIcons.Download else ConsultantIcons.FolderOpen,
                label = if (savedRecord == null) {
                    stringResource(R.string.crm_consultant_save)
                } else {
                    stringResource(R.string.crm_consultant_open_downloads)
                },
                enabled = fileUrl != null && !isHandlingAction,
                onClick = {
                    val resolvedFileUrl = fileUrl ?: return@FileActionRow
                    scope.launch {
                        isHandlingAction = true
                        runCatching {
                            val currentRecord = downloadedRecord
                            if (currentRecord != null) {
                                openDownloads(context)
                            } else {
                                downloadedRecord = ConsultantFileActions.saveFileToDownloads(
                                    context = context,
                                    registry = downloadedFileRegistry,
                                    fileUrl = resolvedFileUrl,
                                    fileName = fileName,
                                )
                            }
                        }.onFailure(onError)
                        isHandlingAction = false
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun FileActionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) ConsultantPrimaryText else ConsultantSecondaryText,
        )
        Text(
            text = label,
            color = if (enabled) ConsultantPrimaryText else ConsultantSecondaryText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
