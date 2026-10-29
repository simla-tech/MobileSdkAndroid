package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun ImageMessageBubble(
    message: OnlineConsultantMessageUiModel,
    imagePainter: AsyncImagePainter,
    statusIcon: StatusIconSpec?,
    imageLayoutResult: ConsultantImageBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    imageWidth: androidx.compose.ui.unit.Dp,
    imageHeight: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    authorStyle: androidx.compose.ui.text.TextStyle,
    authorBubbleDirection: ConsultantMessageTextDirection,
    shouldShowAuthor: Boolean,
    onLayoutClick: ((String) -> Unit)?,
    onImageClick: (String) -> Unit,
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
                } else if (message.imageUrl != null) {
                    Modifier.clickable { onImageClick(requireNotNull(message.imageUrl)) }
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
            modifier = Modifier.padding(IMAGE_BUBBLE_PADDING),
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

            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .height(imageHeight)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                Image(
                    painter = imagePainter,
                    contentDescription = null,
                    contentScale = imageLayoutResult.contentScale,
                    modifier = Modifier.fillMaxSize(),
                )

                if (imageLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY) {
                    MessageMetaOverlay(
                        message = message,
                        statusIcon = statusIcon,
                    )
                }
            }

            if (imageLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
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
