package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.ui.icons.AttachFile
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun FileMessageBubble(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    fileLayoutResult: ConsultantFileBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    fileContentWidth: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    fileNameStyle: androidx.compose.ui.text.TextStyle,
    fileSizeStyle: androidx.compose.ui.text.TextStyle,
    authorStyle: androidx.compose.ui.text.TextStyle,
    authorBubbleDirection: ConsultantMessageTextDirection,
    shouldShowAuthor: Boolean,
    onLayoutClick: ((String) -> Unit)?,
    onFileIconClick: (OnlineConsultantMessageUiModel) -> Unit,
    layoutDebug: String,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val shape = bubbleShapeFor(isVisitor, visualTheme.roundedElements)
    Surface(
        modifier = Modifier
            .width(bubbleWidth)
            .clip(shape)
            .then(
                if (onLayoutClick != null) {
                    Modifier.clickable { onLayoutClick(layoutDebug) }
                } else {
                    Modifier
                },
            ),
        color = bubbleColorFor(isVisitor, visualTheme),
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = if (isVisitor) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = FILE_BUBBLE_HORIZONTAL_PADDING,
                vertical = FILE_BUBBLE_VERTICAL_PADDING,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (shouldShowAuthor) {
                Text(
                    text = message.authorName,
                    color = bubbleContentColorFor(isVisitor, visualTheme),
                    style = authorStyle,
                    textAlign = bubbleTextAlignFor(authorBubbleDirection),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FileMessageContent(
                message = message,
                statusIcon = statusIcon,
                fileLayoutResult = fileLayoutResult,
                fileNameStyle = fileNameStyle,
                fileSizeStyle = fileSizeStyle,
                onFileIconClick = { onFileIconClick(message) },
                modifier = Modifier
                    .width(fileContentWidth)
                    .height(FILE_CONTENT_HEIGHT),
            )

            if (fileLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
                MessageMetaRow(
                    message = message,
                    statusIcon = statusIcon,
                    metaDirection = ConsultantMessageTextDirection.LTR,
                    inlineStartSpacing = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

    }
}

@Composable
internal fun FileMessageContent(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    fileLayoutResult: ConsultantFileBubbleLayoutResult,
    fileNameStyle: androidx.compose.ui.text.TextStyle,
    fileSizeStyle: androidx.compose.ui.text.TextStyle,
    onFileIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(FILE_ICON_BOX_SIZE)
                .clip(
                    if (visualTheme.roundedElements) RoundedCornerShape(8.dp) else RoundedCornerShape(
                        0.dp
                    )
                )
                .background(
                    if (message.author == OnlineConsultantMessageAuthor.VISITOR) {
                        visualTheme.mainTextColor
                    } else {
                        visualTheme.brandColor
                    },
                )
                .clickable(onClick = onFileIconClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ConsultantIcons.AttachFile,
                contentDescription = null,
                tint = if (message.author == OnlineConsultantMessageAuthor.VISITOR) {
                    visualTheme.brandColor
                } else {
                    visualTheme.mainIconColor
                },
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message.fileName ?: stringResource(R.string.crm_consultant_attachment),
                color = bubbleContentColorFor(message, visualTheme),
                style = fileNameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (fileLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message.fileSizeLabel
                            ?: stringResource(R.string.crm_consultant_file),
                        color = if (message.author == OnlineConsultantMessageAuthor.VISITOR) {
                            visualTheme.mainTextColor
                        } else {
                            Color(0xFF8A96A6)
                        },
                        style = fileSizeStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    MessageMetaRow(
                        message = message,
                        statusIcon = statusIcon,
                        metaDirection = ConsultantMessageTextDirection.LTR,
                        inlineStartSpacing = META_START_SPACING,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = fileLayoutResult.metaOverlayOffsetYPx,
                                )
                            },
                    )
                }
            } else {
                Text(
                    text = message.fileSizeLabel ?: stringResource(R.string.crm_consultant_file),
                    color = if (message.author == OnlineConsultantMessageAuthor.VISITOR) {
                        visualTheme.mainTextColor
                    } else {
                        Color(0xFF8A96A6)
                    },
                    style = fileSizeStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
