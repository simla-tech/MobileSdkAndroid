package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageButtonUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantQuickResponseUiModel

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AutoMessageBubble(
    message: OnlineConsultantMessageUiModel,
    imagePainter: AsyncImagePainter?,
    statusIcon: StatusIconSpec?,
    layoutResult: ConsultantAutoMessageBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    contentWidth: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    authorStyle: androidx.compose.ui.text.TextStyle,
    titleStyle: androidx.compose.ui.text.TextStyle,
    bodyBubbleDirection: ConsultantMessageTextDirection,
    shouldShowAuthor: Boolean,
    onLayoutClick: ((String) -> Unit)?,
    onQuickResponseClick: (String) -> Unit,
    layoutDebug: String,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val shape = bubbleShapeFor(isVisitor, visualTheme.roundedElements)
    val messageLinkColor = remember(isVisitor, visualTheme) {
        if (isVisitor) bubbleContentColorFor(true, visualTheme) else visualTheme.brandColor
    }
    val bodyAnnotatedText = remember(message.text, messageLinkColor) {
        messageHtmlAnnotatedString(
            source = message.text,
            linkColor = messageLinkColor,
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shouldShowAuthor) {
                    Text(
                        text = message.authorName,
                        color = bubbleContentColorFor(isVisitor, visualTheme),
                        style = authorStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(contentWidth),
                    )
                }

                if (!message.autoMessageImageUrl.isNullOrBlank() && imagePainter != null) {
                    val imageHeight = autoMessageImageHeight(
                        width = contentWidth,
                        sourceWidthPx = message.autoMessageImageWidthPx,
                        sourceHeightPx = message.autoMessageImageHeightPx,
                    )
                    Image(
                        painter = imagePainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(contentWidth)
                            .height(imageHeight)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }

                message.autoMessageTitle?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        text = title,
                        color = bubbleContentColorFor(isVisitor, visualTheme),
                        style = titleStyle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(contentWidth),
                    )
                }

                Box(modifier = Modifier.width(contentWidth)) {
                    AnnotatedLinkText(
                        text = bodyAnnotatedText,
                        color = bubbleContentColorFor(isVisitor, visualTheme),
                        style = bodyStyle,
                        textAlign = bubbleTextAlignFor(bodyBubbleDirection),
                        linkHighlightColor = messageLinkColor,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (message.buttons.isEmpty() &&
                        layoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY
                    ) {
                        MessageMetaRow(
                            message = message,
                            statusIcon = statusIcon,
                            metaDirection = bodyBubbleDirection,
                            inlineStartSpacing = META_START_SPACING,
                            modifier = Modifier
                                .align(bubbleMetaAlignmentFor(bodyBubbleDirection))
                                .offset {
                                    IntOffset(
                                        x = 0,
                                        y = layoutResult.metaOverlayOffsetYPx,
                                    )
                                },
                        )
                    }
                }

                if (message.buttons.isEmpty() &&
                    layoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT
                ) {
                    MessageMetaRow(
                        message = message,
                        statusIcon = statusIcon,
                        metaDirection = bodyBubbleDirection,
                        inlineStartSpacing = 0.dp,
                        modifier = Modifier.width(contentWidth),
                    )
                }

                if (message.buttons.isNotEmpty()) {
                    Column(
                        modifier = Modifier.width(contentWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        message.buttons.forEach { button ->
                            AutoMessageButton(
                                button = button,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    MessageMetaRow(
                        message = message,
                        statusIcon = statusIcon,
                        metaDirection = bodyBubbleDirection,
                        inlineStartSpacing = 0.dp,
                        modifier = Modifier.width(contentWidth),
                    )
                }
            }
        }

        if (message.quickResponses.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.width(bubbleWidth),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message.quickResponses.forEach { response ->
                    QuickResponseChip(
                        response = response,
                        onClick = onQuickResponseClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AutoMessageButton(
    button: OnlineConsultantMessageButtonUiModel,
    modifier: Modifier = Modifier,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier
            .height(CONTACT_FORM_BUTTON_HEIGHT)
            .clip(
                if (visualTheme.roundedElements) {
                    RoundedCornerShape(8.dp)
                } else {
                    RoundedCornerShape(0.dp)
                },
            ),
        shape = if (visualTheme.roundedElements) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp),
        color = visualTheme.brandColor,
        onClick = {
            button.url?.takeIf { it.isNotBlank() }?.let(uriHandler::openUri)
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = button.label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AUTO_MESSAGE_BUTTON_HORIZONTAL_PADDING),
            )
        }
    }
}

@Composable
internal fun QuickResponseChip(
    response: OnlineConsultantQuickResponseUiModel,
    onClick: (String) -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
        shape = RoundedCornerShape(if (visualTheme.roundedElements) 8.dp else 2.dp),
        color = visualTheme.brandAlphaColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, visualTheme.brandDarkestColor),
        onClick = { onClick(response.title) },
    ) {
        Text(
            text = response.title,
            color = visualTheme.brandDarkestColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

internal fun autoMessageImageHeight(
    width: androidx.compose.ui.unit.Dp,
    sourceWidthPx: Int?,
    sourceHeightPx: Int?,
): androidx.compose.ui.unit.Dp {
    val sourceWidth = sourceWidthPx?.takeIf { it > 0 } ?: return 128.dp
    val sourceHeight = sourceHeightPx?.takeIf { it > 0 } ?: return 128.dp
    val ratio = sourceHeight.toFloat() / sourceWidth.toFloat()
    return (width.value * ratio).dp.coerceIn(96.dp, 180.dp)
}
