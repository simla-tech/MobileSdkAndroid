package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun QuoteMessageBubble(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    quoteLayoutResult: ConsultantQuoteBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    quoteContentWidth: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    fileSizeStyle: androidx.compose.ui.text.TextStyle,
    authorStyle: androidx.compose.ui.text.TextStyle,
    authorBubbleDirection: ConsultantMessageTextDirection,
    bodyBubbleDirection: ConsultantMessageTextDirection,
    shouldShowAuthor: Boolean,
    onLayoutClick: ((String) -> Unit)?,
    onQuoteClick: (OnlineConsultantMessageUiModel) -> Unit,
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
            modifier = Modifier.padding(QUOTE_BUBBLE_PADDING),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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

            QuoteMessageContent(
                message = message,
                textStyle = fileSizeStyle,
                modifier = Modifier
                    .width(quoteContentWidth)
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onQuoteClick(message) },
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (message.text.isNotBlank()) {
                    val messageLinkColor = if (isVisitor) {
                        bubbleContentColorFor(true, visualTheme)
                    } else {
                        visualTheme.brandColor
                    }
                    val bodyAnnotatedText = remember(message.text, messageLinkColor) {
                        messageHtmlAnnotatedString(
                            source = message.text,
                            linkColor = messageLinkColor,
                        )
                    }
                    AnnotatedLinkText(
                        text = bodyAnnotatedText,
                        color = bubbleContentColorFor(isVisitor, visualTheme),
                        style = bodyStyle,
                        textAlign = bubbleTextAlignFor(bodyBubbleDirection),
                        linkHighlightColor = messageLinkColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (quoteLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY) {
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
                                    y = quoteLayoutResult.metaOverlayOffsetYPx,
                                )
                            },
                    )
                }
            }

            if (quoteLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
                MessageMetaRow(
                    message = message,
                    statusIcon = statusIcon,
                    metaDirection = bodyBubbleDirection,
                    inlineStartSpacing = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun QuoteMessageContent(
    message: OnlineConsultantMessageUiModel,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val quoteImageUrl = message.quoteImageUrl
    val painter = quoteImageUrl?.let { rememberAsyncImagePainter(model = it) }
    val primaryText = quotePrimaryText(message)
    val secondaryText = quoteSecondaryText(message)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(QUOTE_BAR_CONTENT_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(QUOTE_BAR_WIDTH)
                .height(if (quoteImageUrl != null) QUOTE_THUMBNAIL_SIZE else QUOTE_TEXT_MIN_HEIGHT)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFFAFB9C3)),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(QUOTE_THUMBNAIL_TEXT_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (quoteImageUrl != null && painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(QUOTE_THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = primaryText,
                    color = Color(0xFF8A96A6),
                    style = textStyle,
                    maxLines = if (message.quoteFileName != null || quoteImageUrl != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        color = Color(0xFF8A96A6),
                        style = textStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
