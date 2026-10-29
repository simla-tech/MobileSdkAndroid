package com.simla.android.sdk.consultant.ui

import android.app.DownloadManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.ui.icons.Close
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.icons.Download
import com.simla.android.sdk.consultant.ui.icons.FolderOpen
import com.simla.android.sdk.consultant.ui.icons.MoreVert
import com.simla.android.sdk.consultant.ui.icons.Share
import kotlinx.coroutines.launch

@Composable
internal fun FullscreenImageViewer(
    imageUrl: String?,
    onClose: () -> Unit,
) {
    var displayedImageUrl by remember { mutableStateOf<String?>(null) }
    val transitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            displayedImageUrl = imageUrl
            transitionState.targetState = true
        } else {
            transitionState.targetState = false
        }
    }

    LaunchedEffect(transitionState.isIdle, transitionState.currentState) {
        if (transitionState.isIdle && !transitionState.currentState) {
            displayedImageUrl = null
        }
    }

    val currentImageUrl = displayedImageUrl ?: return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val painter = rememberAsyncImagePainter(model = currentImageUrl)
            var menuExpanded by remember(currentImageUrl) { mutableStateOf(false) }
            var isWorking by remember(currentImageUrl) { mutableStateOf(false) }
            var downloadedUri by remember(currentImageUrl) { mutableStateOf<android.net.Uri?>(null) }
            var scale by remember(currentImageUrl) { mutableFloatStateOf(1f) }
            var offsetX by remember(currentImageUrl) { mutableFloatStateOf(0f) }
            var offsetY by remember(currentImageUrl) { mutableFloatStateOf(0f) }

            LaunchedEffect(currentImageUrl) {
                downloadedUri =
                    ConsultantImageFileActions.findDownloadedImage(context, currentImageUrl)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentImageUrl) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = nextScale
                                if (nextScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                )

                if (painter.state is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }

                FullscreenImageToolbar(
                    currentImageUrl = currentImageUrl,
                    downloadedUri = downloadedUri,
                    isWorking = isWorking,
                    menuExpanded = menuExpanded,
                    onClose = onClose,
                    onMenuExpandedChange = { menuExpanded = it },
                    onDownloadedUriChange = { downloadedUri = it },
                    onWorkingChange = { isWorking = it },
                ) {
                    scope
                }
            }
        }
    }
}

@Composable
internal fun FullscreenImageToolbar(
    currentImageUrl: String,
    downloadedUri: android.net.Uri?,
    isWorking: Boolean,
    menuExpanded: Boolean,
    onClose: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onDownloadedUriChange: (android.net.Uri?) -> Unit,
    onWorkingChange: (Boolean) -> Unit,
    scopeProvider: () -> kotlinx.coroutines.CoroutineScope,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val context = LocalContext.current
    val scope = scopeProvider()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = ConsultantIcons.Close,
                contentDescription = stringResource(R.string.crm_consultant_close_image),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            enabled = !isWorking,
            onClick = {
                scope.launch {
                    onWorkingChange(true)
                    runCatching {
                        val shareUri = ConsultantImageFileActions.shareCacheUri(
                            context = context,
                            imageUrl = currentImageUrl,
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = ConsultantImageFileActions.mimeType(currentImageUrl)
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.crm_consultant_share_image),
                            ),
                        )
                    }.onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.crm_consultant_share_image_error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    onWorkingChange(false)
                }
            },
        ) {
            Icon(
                imageVector = ConsultantIcons.Share,
                contentDescription = stringResource(R.string.crm_consultant_share_image),
                tint = Color.White,
            )
        }

        Box {
            IconButton(
                enabled = !isWorking,
                onClick = { onMenuExpandedChange(true) },
            ) {
                Icon(
                    imageVector = ConsultantIcons.MoreVert,
                    contentDescription = stringResource(R.string.crm_consultant_image_options),
                    tint = Color.White,
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (downloadedUri == null) {
                                stringResource(R.string.crm_consultant_download)
                            } else {
                                stringResource(R.string.crm_consultant_open_downloads)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (downloadedUri == null) {
                                ConsultantIcons.Download
                            } else {
                                ConsultantIcons.FolderOpen
                            },
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onMenuExpandedChange(false)
                        if (downloadedUri != null) {
                            runCatching {
                                context.startActivity(
                                    Intent(DownloadManager.ACTION_VIEW_DOWNLOADS),
                                )
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.crm_consultant_open_downloads_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        } else {
                            scope.launch {
                                onWorkingChange(true)
                                runCatching {
                                    onDownloadedUriChange(
                                        ConsultantImageFileActions.saveImageToDownloads(
                                            context = context,
                                            imageUrl = currentImageUrl,
                                        ),
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.crm_consultant_image_saved),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.crm_consultant_download_image_error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                onWorkingChange(false)
                            }
                        }
                    },
                )
            }
        }
    }
}
