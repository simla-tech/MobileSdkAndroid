package com.simla.android.sdk.consultant.ui

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImagePainter
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.icons.Image as ImageIcon

@Composable
internal fun ProductMessageBubble(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    productImagePainter: AsyncImagePainter?,
    productLayoutResult: ConsultantProductBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    productContentWidth: androidx.compose.ui.unit.Dp,
    productImageWidth: androidx.compose.ui.unit.Dp,
    productImageHeight: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    productTitleStyle: androidx.compose.ui.text.TextStyle,
    productPriceStyle: androidx.compose.ui.text.TextStyle,
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
            .clip(shape),
        color = bubbleColorFor(isVisitor, visualTheme),
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = if (isVisitor) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(PRODUCT_BUBBLE_PADDING),
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

            ProductMessageContent(
                message = message,
                statusIcon = statusIcon,
                productImagePainter = productImagePainter,
                productLayoutResult = productLayoutResult,
                productTitleStyle = productTitleStyle,
                productPriceStyle = productPriceStyle,
                productImageWidth = productImageWidth,
                productImageHeight = productImageHeight,
                onLayoutClick = onLayoutClick,
                onImageClick = onImageClick,
                layoutDebug = layoutDebug,
                modifier = Modifier.width(productContentWidth),
            )

            if (productLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
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
internal fun ProductMessageContent(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    productImagePainter: AsyncImagePainter?,
    productLayoutResult: ConsultantProductBubbleLayoutResult,
    productTitleStyle: androidx.compose.ui.text.TextStyle,
    productPriceStyle: androidx.compose.ui.text.TextStyle,
    productImageWidth: androidx.compose.ui.unit.Dp,
    productImageHeight: androidx.compose.ui.unit.Dp,
    onLayoutClick: ((String) -> Unit)?,
    onImageClick: (String) -> Unit,
    layoutDebug: String,
    modifier: Modifier = Modifier,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val context = LocalContext.current
    val productUrl = message.productUrl?.takeIf { it.isNotBlank() }
    val productTitle = message.productName ?: stringResource(R.string.crm_consultant_product)
    var productTitleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isTitlePressed by remember { mutableStateOf(false) }
    var productImageBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var productTitleBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val titleColor by animateColorAsState(
        targetValue = when {
            message.author == OnlineConsultantMessageAuthor.VISITOR -> visualTheme.mainTextColor
            productUrl == null -> visualTheme.primaryText
            isTitlePressed -> visualTheme.brandPressedColor
            else -> visualTheme.brandColor
        },
        label = "product-title-color",
    )
    val titleBackgroundColor by animateColorAsState(
        targetValue = if (
            message.author != OnlineConsultantMessageAuthor.VISITOR &&
            isTitlePressed &&
            productUrl != null
        ) {
            visualTheme.brandAlphaColor
        } else {
            Color.Transparent
        },
        label = "product-title-background",
    )
    val productTitleText = productTitleAnnotatedString(
        title = productTitle,
        titleColor = titleColor,
        isTitleClickable = productUrl != null,
        url = productUrl.orEmpty(),
    )
    val currentProductTitleText by rememberUpdatedState(productTitleText)
    val currentProductTitleLayout by rememberUpdatedState(productTitleLayout)

    Column(
        modifier = modifier.then(
            if (onLayoutClick != null) {
                Modifier.pointerInput(layoutDebug, productImageBounds, productTitleBounds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isInteractiveTap =
                            productImageBounds?.contains(down.position) == true ||
                                    productTitleBounds?.contains(down.position) == true

                        if (isInteractiveTap) {
                            waitForUpOrCancellation(pass = PointerEventPass.Final)
                            return@awaitEachGesture
                        }

                        down.consume()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            onLayoutClick(layoutDebug)
                        }
                    }
                }
            } else {
                Modifier
            },
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(productImageWidth)
                .height(productImageHeight)
                .onGloballyPositioned { coordinates ->
                    productImageBounds = coordinates.boundsInParent()
                }
                .clip(RoundedCornerShape(8.dp))
                .background(ConsultantProductPlaceholderColor),
            contentAlignment = Alignment.Center,
        ) {
            if (productImagePainter != null) {
                Image(
                    painter = productImagePainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            message.productImageUrl?.let { imageUrl ->
                                Modifier.clickable { onImageClick(imageUrl) }
                            } ?: Modifier,
                        ),
                )
            } else {
                Icon(
                    imageVector = ConsultantIcons.ImageIcon,
                    contentDescription = null,
                    tint = ConsultantProductPlaceholderIconColor,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Text(
            text = productTitleText,
            color = titleColor,
            style = productTitleStyle,
            maxLines = PRODUCT_TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { productTitleLayout = it },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    productTitleBounds = if (productUrl != null) {
                        coordinates.boundsInParent()
                    } else {
                        null
                    }
                }
                .drawBehind {
                    drawTextRangePressedBackground(
                        layout = currentProductTitleLayout,
                        start = 0,
                        end = productTitle.length,
                        color = titleBackgroundColor,
                        horizontalPaddingPx = PRODUCT_TITLE_HIGHLIGHT_HORIZONTAL_PADDING.toPx(),
                        cornerRadiusPx = PRODUCT_TITLE_HIGHLIGHT_CORNER_RADIUS.toPx(),
                    )
                }
                .pointerInput(productUrl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val titleLayout = currentProductTitleLayout
                        val titleText = currentProductTitleText
                        val isTitleDown = titleLayout != null &&
                                titleText.hasProductTitleLinkAt(titleLayout, down.position)
                        if (productUrl == null || !isTitleDown) {
                            return@awaitEachGesture
                        }

                        down.consume()
                        isTitlePressed = true
                        val up = waitForUpOrCancellation()
                        isTitlePressed = false
                        if (up != null) {
                            val isTitleUp = currentProductTitleLayout?.let { layout ->
                                currentProductTitleText.hasProductTitleLinkAt(layout, up.position)
                            } == true
                            up.consume()
                            if (isTitleUp) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, productUrl.toUri()),
                                    )
                                }
                            }
                        }
                    }
                },
        )

        val price = productPriceText(message)
        if (price.isNotBlank() ||
            productLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (price.isNotBlank()) {
                    Text(
                        text = price,
                        color = bubbleContentColorFor(message, visualTheme),
                        style = productPriceStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }

                if (productLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY) {
                    MessageMetaRow(
                        message = message,
                        statusIcon = statusIcon,
                        metaDirection = ConsultantMessageTextDirection.LTR,
                        inlineStartSpacing = META_START_SPACING,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }
    }
}
