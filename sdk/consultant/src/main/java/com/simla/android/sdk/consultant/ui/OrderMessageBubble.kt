package com.simla.android.sdk.consultant.ui

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderFieldUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantOrderItemUiModel

@Composable
internal fun OrderMessageBubble(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    orderLayoutResult: ConsultantOrderBubbleLayoutResult,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    orderContentWidth: androidx.compose.ui.unit.Dp,
    isVisitor: Boolean,
    orderItemTitleStyle: androidx.compose.ui.text.TextStyle,
    orderPrimaryStyle: androidx.compose.ui.text.TextStyle,
    orderSecondaryStyle: androidx.compose.ui.text.TextStyle,
    authorStyle: androidx.compose.ui.text.TextStyle,
    authorBubbleDirection: ConsultantMessageTextDirection,
    shouldShowAuthor: Boolean,
    onLayoutClick: ((String) -> Unit)?,
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
            modifier = Modifier.padding(ORDER_BUBBLE_PADDING),
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

            OrderMessageContent(
                message = message,
                statusIcon = statusIcon,
                orderLayoutResult = orderLayoutResult,
                orderItemTitleStyle = orderItemTitleStyle,
                orderPrimaryStyle = orderPrimaryStyle,
                orderSecondaryStyle = orderSecondaryStyle,
                modifier = Modifier.width(orderContentWidth),
            )

            if (orderLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
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
internal fun OrderMessageContent(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    orderLayoutResult: ConsultantOrderBubbleLayoutResult,
    orderItemTitleStyle: androidx.compose.ui.text.TextStyle,
    orderPrimaryStyle: androidx.compose.ui.text.TextStyle,
    orderSecondaryStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        message.orderNumberLabel?.takeIf { it.isNotBlank() }?.let { orderNumber ->
            OrderValueLine(
                text = orderNumber,
                statusIcon = statusIcon,
                message = message,
                style = orderPrimaryStyle,
                color = bubbleContentColorFor(message, LocalCrmVisualTheme.current),
                showInlineMeta = message.orderItems.isEmpty() &&
                        message.orderFields.isEmpty() &&
                        orderLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY,
            )
        }

        message.orderItems.forEachIndexed { index, item ->
            OrderItemBlock(
                item = item,
                statusIcon = statusIcon,
                message = message,
                orderItemTitleStyle = orderItemTitleStyle,
                orderSecondaryStyle = orderSecondaryStyle,
                showInlineMeta = message.orderFields.isEmpty() &&
                        index == message.orderItems.lastIndex &&
                        orderLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY,
            )
        }

        message.orderFields.forEachIndexed { index, field ->
            OrderFieldBlock(
                field = field,
                statusIcon = statusIcon,
                message = message,
                orderPrimaryStyle = orderPrimaryStyle,
                orderSecondaryStyle = orderSecondaryStyle,
                showInlineMeta = index == message.orderFields.lastIndex &&
                        orderLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY,
            )
        }
    }
}

@Composable
internal fun OrderItemBlock(
    item: OnlineConsultantOrderItemUiModel,
    statusIcon: StatusIconSpec?,
    message: OnlineConsultantMessageUiModel,
    orderItemTitleStyle: androidx.compose.ui.text.TextStyle,
    orderSecondaryStyle: androidx.compose.ui.text.TextStyle,
    showInlineMeta: Boolean,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val context = LocalContext.current
    val itemUrl = item.url?.takeIf { it.isNotBlank() }
    var itemTitleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isNamePressed by remember { mutableStateOf(false) }
    val nameColor by animateColorAsState(
        targetValue = when {
            message.author == OnlineConsultantMessageAuthor.VISITOR -> visualTheme.mainTextColor
            itemUrl == null -> visualTheme.primaryText
            isNamePressed -> visualTheme.brandPressedColor
            else -> visualTheme.brandColor
        },
        label = "orderItemNameColor",
    )
    val nameBackgroundColor by animateColorAsState(
        targetValue = if (
            message.author != OnlineConsultantMessageAuthor.VISITOR &&
            isNamePressed &&
            itemUrl != null
        ) {
            visualTheme.brandAlphaColor
        } else {
            Color.Transparent
        },
        label = "orderItemNameBackgroundColor",
    )
    val itemTitleText = orderItemTitleAnnotatedString(
        item = item,
        nameColor = nameColor,
        isNameClickable = itemUrl != null,
    )
    val currentItemTitleText by rememberUpdatedState(itemTitleText)
    val currentItemTitleLayout by rememberUpdatedState(itemTitleLayout)
    val itemNameStartOffset = orderItemNameStartOffset(item)
    val itemNameEndOffset = itemNameStartOffset + item.name.length
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = itemTitleText,
            color = nameColor,
            style = orderItemTitleStyle,
            maxLines = ORDER_ITEM_NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { itemTitleLayout = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawTextRangePressedBackground(
                        layout = currentItemTitleLayout,
                        start = itemNameStartOffset,
                        end = itemNameEndOffset,
                        color = nameBackgroundColor,
                        horizontalPaddingPx = ORDER_ITEM_NAME_HIGHLIGHT_HORIZONTAL_PADDING.toPx(),
                        cornerRadiusPx = ORDER_ITEM_NAME_HIGHLIGHT_CORNER_RADIUS.toPx(),
                    )
                }
                .pointerInput(itemUrl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val titleLayout = currentItemTitleLayout
                        val titleText = currentItemTitleText
                        val isNameDown = titleLayout != null &&
                                titleText.hasOrderItemNameLinkAt(titleLayout, down.position)
                        if (itemUrl == null || !isNameDown) {
                            return@awaitEachGesture
                        }

                        down.consume()
                        isNamePressed = true
                        val up = waitForUpOrCancellation()
                        isNamePressed = false
                        if (up != null) {
                            val isNameUp = currentItemTitleLayout?.let { layout ->
                                currentItemTitleText.hasOrderItemNameLinkAt(layout, up.position)
                            } == true
                            up.consume()
                            if (isNameUp) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, itemUrl.toUri()),
                                    )
                                }
                            }
                        }
                    }
                },
        )
        OrderValueLine(
            text = item.details,
            statusIcon = statusIcon,
            message = message,
            style = orderSecondaryStyle,
            color = LocalCrmVisualTheme.current.mainGrayColor,
            showInlineMeta = showInlineMeta,
        )
    }
}

@Composable
internal fun OrderFieldBlock(
    field: OnlineConsultantOrderFieldUiModel,
    statusIcon: StatusIconSpec?,
    message: OnlineConsultantMessageUiModel,
    orderPrimaryStyle: androidx.compose.ui.text.TextStyle,
    orderSecondaryStyle: androidx.compose.ui.text.TextStyle,
    showInlineMeta: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = field.title,
            color = bubbleContentColorFor(message, LocalCrmVisualTheme.current),
            style = orderPrimaryStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        OrderValueLine(
            text = field.value,
            statusIcon = statusIcon,
            message = message,
            style = orderSecondaryStyle,
            color = LocalCrmVisualTheme.current.mainGrayColor,
            showInlineMeta = showInlineMeta,
        )
    }
}

@Composable
internal fun OrderValueLine(
    text: String,
    statusIcon: StatusIconSpec?,
    message: OnlineConsultantMessageUiModel,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    showInlineMeta: Boolean,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = color,
            style = style,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (showInlineMeta) {
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
