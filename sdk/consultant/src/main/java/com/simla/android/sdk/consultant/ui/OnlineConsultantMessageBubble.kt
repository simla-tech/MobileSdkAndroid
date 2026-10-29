package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Composable
internal fun OnlineConsultantMessageBubble(
    message: OnlineConsultantMessageUiModel,
    maxBubbleWidth: Dp,
    imageDimensionsByUrl: MutableMap<String, Pair<Int, Int>>,
    isFollowFromPrevious: Boolean,
    isFollowToNext: Boolean,
    onLayoutClick: ((String) -> Unit)?,
    onImageClick: (String) -> Unit,
    onFileIconClick: (OnlineConsultantMessageUiModel) -> Unit,
    highlightNonce: Int?,
    onHighlightConsumed: (Int) -> Unit,
    onQuoteClick: (OnlineConsultantMessageUiModel) -> Unit,
    onContactFormSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit,
    onQuickResponseClick: (String) -> Unit,
) {
    ConsultantRecompositionLog("OnlineConsultantMessageBubble") {
        "id=${message.id} status=${message.deliveryStatus} " +
                "followPrev=$isFollowFromPrevious followNext=$isFollowToNext " +
                "highlight=$highlightNonce textHash=${message.text.hashCode()}"
    }
    val visualTheme = LocalCrmVisualTheme.current
    val isVisitor = message.author == OnlineConsultantMessageAuthor.VISITOR
    val bodyPlainText = remember(message.text) { messageHtmlPlainText(message.text) }
    val messageLinkColor = remember(isVisitor, visualTheme) {
        if (isVisitor) bubbleContentColorFor(true, visualTheme) else visualTheme.brandColor
    }
    val bodyAnnotatedText = remember(message.text, messageLinkColor) {
        messageHtmlAnnotatedString(
            source = message.text,
            linkColor = messageLinkColor,
        )
    }
    val authorTextDirection = composeTextDirectionFor(message.authorName)
    val bodyTextDirection = composeTextDirectionFor(bodyPlainText)
    val authorBubbleDirection = bubbleTextDirectionFor(message.authorName)
    val bodyBubbleDirection = bubbleTextDirectionFor(bodyPlainText)
    val authorStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Medium,
        textDirection = authorTextDirection,
    )
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        textDirection = bodyTextDirection,
    )
    val timeStyle = MaterialTheme.typography.labelSmall
    val fileNameStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    val fileSizeStyle = MaterialTheme.typography.bodySmall
    val productTitleStyle = MaterialTheme.typography.bodyLarge
    val orderItemTitleStyle = productTitleStyle
    val orderPrimaryStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val productPriceStyle = orderPrimaryStyle
    val orderSecondaryStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val highlightIndication = LocalIndication.current
    val highlightInteractionSource = remember { MutableInteractionSource() }
    var highlightSize by remember { mutableStateOf(IntSize.Zero) }
    val horizontalPadding = 12.dp
    val verticalPadding = 10.dp
    val statusIcon = statusIconFor(message.deliveryStatus)

    LaunchedEffect(highlightNonce) {
        if (highlightNonce == null) {
            return@LaunchedEffect
        }

        val resolvedHighlightSize = snapshotFlow { highlightSize }
            .first { size -> size.width > 0 && size.height > 0 }
        val press = PressInteraction.Press(
            Offset(
                x = resolvedHighlightSize.width / 2f,
                y = resolvedHighlightSize.height / 2f,
            ),
        )
        highlightInteractionSource.emit(press)
        delay(260)
        highlightInteractionSource.emit(PressInteraction.Release(press))
        onHighlightConsumed(highlightNonce)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { highlightSize = it }
            .indication(highlightInteractionSource, highlightIndication),
    ) {
        val maxBubbleWidthPx = with(density) { maxBubbleWidth.roundToPx() }
        val horizontalPaddingPx = with(density) { horizontalPadding.roundToPx() }
        val verticalPaddingPx = with(density) { verticalPadding.roundToPx() }
        val imageBubblePaddingPx = with(density) { IMAGE_BUBBLE_PADDING.roundToPx() }
        val contentMaxWidthPx = (maxBubbleWidthPx - horizontalPaddingPx * 2).coerceAtLeast(0)
        val imageContentMaxWidthPx = imageBubbleContentMaxWidth(
            maxBubbleWidthPx = maxBubbleWidthPx,
            imageBubblePaddingPx = imageBubblePaddingPx,
        )
        val contentMaxHeightPx = imageContentMaxWidthPx
        val imageUrl = message.imageUrl
        val isFileMessage = message.fileUrl != null || message.fileName != null
        val isQuoteMessage = message.quoteText != null ||
                message.quoteFileName != null ||
                message.quoteImageUrl != null
        val isProductMessage = message.productName != null ||
                message.productImageUrl != null ||
                message.productPriceLabel != null ||
                message.productQuantityLabel != null
        val isOrderMessage = !message.orderNumberLabel.isNullOrBlank() ||
                message.orderItems.isNotEmpty() ||
                message.orderFields.isNotEmpty()
        val contactForm = message.contactForm
        val isContactFormMessage = contactForm != null
        val isAutoMessage = !message.autoMessageTitle.isNullOrBlank() ||
                !message.autoMessageImageUrl.isNullOrBlank() ||
                message.buttons.isNotEmpty() ||
                message.quickResponses.isNotEmpty()
        val isTextMessage = !isContactFormMessage && !isAutoMessage && !isQuoteMessage &&
                !isOrderMessage && !isProductMessage && imageUrl == null && !isFileMessage
        val needsBodyMeasurement = isTextMessage || isAutoMessage || isQuoteMessage
        val authorLayout = textMeasurer.measure(
            text = message.authorName,
            style = authorStyle,
        )
        val authorSingleLineLayout = textMeasurer.measure(
            text = message.authorName,
            style = authorStyle,
            maxLines = 1,
            softWrap = false,
        )

        fun measureBodyText(contentWidthPx: Int): ConsultantMeasuredText {
            return textMeasurer.measure(
                text = bodyPlainText,
                style = bodyStyle,
                constraints = androidx.compose.ui.unit.Constraints(
                    maxWidth = contentWidthPx.coerceAtLeast(0),
                ),
            ).let { layout ->
                toMeasuredText(
                    layout = layout,
                    sampleText = bodyPlainText,
                    direction = bubbleTextDirectionFor(bodyPlainText),
                )
            }
        }

        val singleLineBodyLayout = if (needsBodyMeasurement) {
            textMeasurer.measure(
                text = bodyPlainText,
                style = bodyStyle,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            null
        }
        val timeLayout = textMeasurer.measure(
            text = message.timeLabel,
            style = timeStyle,
            maxLines = 1,
            softWrap = false,
        )
        val statusIconPx = with(density) { STATUS_ICON_SIZE.roundToPx() }
        val metaInnerSpacingPx = with(density) { META_INNER_SPACING.roundToPx() }
        val metaStartSpacingPx = with(density) { META_START_SPACING.roundToPx() }
        val metaContentWidthPx = timeLayout.size.width + if (isVisitor && statusIcon != null) {
            statusIconPx + metaInnerSpacingPx
        } else {
            0
        }
        val metaHeightPx = maxOf(timeLayout.size.height, statusIconPx)
        val imageMetaOverlayWidthPx = imageMetaOverlayRequiredWidth(
            metaContentWidthPx = metaContentWidthPx,
            overlayEdgePaddingPx = with(density) { META_OVERLAY_EDGE_PADDING.roundToPx() },
            overlayHorizontalPaddingPx = with(density) {
                META_OVERLAY_HORIZONTAL_PADDING.roundToPx()
            },
        )
        val imagePainter = imageUrl?.let { rememberAsyncImagePainter(model = it) }
        val autoMessageImagePainter =
            message.autoMessageImageUrl?.let { rememberAsyncImagePainter(model = it) }
        val productImagePainter =
            message.productImageUrl?.let { rememberAsyncImagePainter(model = it) }
        val cachedImageDimensions = imageUrl?.let(imageDimensionsByUrl::get)
        val imageIntrinsicSize = imagePainter?.let(::imageDimensionsFromPainter)
        if (imageUrl != null && cachedImageDimensions == null && imageIntrinsicSize != null) {
            SideEffect {
                imageDimensionsByUrl.putIfAbsent(imageUrl, imageIntrinsicSize)
            }
        }
        // Coil starts a newly composed painter in the loading state even when this list item was
        // shown before. Reuse its first resolved dimensions so the bubble does not briefly return
        // to the square loading size while it crosses the LazyColumn composition boundary.
        val resolvedImageDimensions = cachedImageDimensions ?: imageIntrinsicSize
        val imageSourceWidthPx = imageMessageLayoutSourceSizePx(
            sourceSizePx = message.imageWidthPx ?: resolvedImageDimensions?.first,
            densityScale = density.density,
            fallbackSizePx = imageContentMaxWidthPx,
        )
        val imageSourceHeightPx = imageMessageLayoutSourceSizePx(
            sourceSizePx = message.imageHeightPx ?: resolvedImageDimensions?.second,
            densityScale = density.density,
            fallbackSizePx = imageContentMaxWidthPx,
        )
        val imageLayoutResult = if (imageUrl != null) {
            val imageLayoutInput = ConsultantImageBubbleLayoutInput(
                imageWidthPx = imageSourceWidthPx,
                imageHeightPx = imageSourceHeightPx,
                authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                    authorSingleLineLayout.size.width
                } else {
                    0
                },
                metaWidthPx = imageMetaOverlayWidthPx,
                metaHeightPx = metaHeightPx,
                maxBubbleContentWidthPx = imageContentMaxWidthPx,
                maxBubbleContentHeightPx = contentMaxHeightPx,
            )
            ConsultantImageBubbleLayoutRules.resolve(imageLayoutInput)
        } else {
            null
        }
        val productBubblePaddingPx = with(density) { PRODUCT_BUBBLE_PADDING.roundToPx() }
        val productImageWidthPx = with(density) { PRODUCT_IMAGE_SIZE.roundToPx() }
        val productImageHeightPx = with(density) { PRODUCT_IMAGE_SIZE.roundToPx() }
        val productContentMaxWidthPx =
            (maxBubbleWidthPx - productBubblePaddingPx * 2).coerceAtLeast(0)
        val productTitleLayout = if (isProductMessage) {
            textMeasurer.measure(
                text = message.productName ?: stringResource(R.string.crm_consultant_product),
                style = productTitleStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = productContentMaxWidthPx),
                maxLines = PRODUCT_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            null
        }
        val productPriceLayout = if (isProductMessage) {
            textMeasurer.measure(
                text = productPriceText(message),
                style = productPriceStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = productContentMaxWidthPx),
                maxLines = 1,
                softWrap = false,
            )
        } else {
            null
        }
        val productLayoutResult = if (
            isProductMessage && productTitleLayout != null && productPriceLayout != null
        ) {
            ConsultantProductBubbleLayoutRules.resolve(
                ConsultantProductBubbleLayoutInput(
                    authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                        authorSingleLineLayout.size.width
                    } else {
                        0
                    },
                    productTitleWidthPx = productTitleLayout.size.width,
                    productInfoWidthPx = productPriceLayout.size.width,
                    productImageWidthPx = productImageWidthPx,
                    productImageHeightPx = productImageHeightPx,
                    meta = ConsultantMeasuredMeta(
                        contentWidthPx = metaContentWidthPx,
                        inlineStartSpacingPx = metaStartSpacingPx,
                        heightPx = metaHeightPx,
                        hasStatusIcon = isVisitor && statusIcon != null,
                    ),
                    maxBubbleContentWidthPx = productContentMaxWidthPx,
                ),
            )
        } else {
            null
        }
        val orderBubblePaddingPx = with(density) { ORDER_BUBBLE_PADDING.roundToPx() }
        val orderContentMaxWidthPx =
            (maxBubbleWidthPx - orderBubblePaddingPx * 2).coerceAtLeast(0)
        val orderNumberText = message.orderNumberLabel.orEmpty()
        val orderNumberLayout = if (orderNumberText.isNotBlank()) {
            textMeasurer.measure(
                text = orderNumberText,
                style = orderPrimaryStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = orderContentMaxWidthPx),
                maxLines = 1,
                softWrap = false,
            )
        } else {
            null
        }
        val orderItemFirstLineWidths = if (isOrderMessage) message.orderItems.map { item ->
            textMeasurer.measure(
                text = orderItemTitleText(item),
                style = orderItemTitleStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = orderContentMaxWidthPx),
                maxLines = ORDER_ITEM_NAME_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            ).size.width
        } else emptyList()
        val orderItemDetailLayouts = if (isOrderMessage) message.orderItems.map { item ->
            textMeasurer.measure(
                text = item.details,
                style = orderSecondaryStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = orderContentMaxWidthPx),
                maxLines = 1,
                softWrap = false,
            )
        } else emptyList()
        val orderFieldTitleWidths = if (isOrderMessage) message.orderFields.map { field ->
            textMeasurer.measure(
                text = field.title,
                style = orderPrimaryStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = orderContentMaxWidthPx),
                maxLines = 1,
                softWrap = false,
            ).size.width
        } else emptyList()
        val orderFieldValueLayouts = if (isOrderMessage) message.orderFields.map { field ->
            textMeasurer.measure(
                text = field.value,
                style = orderSecondaryStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = orderContentMaxWidthPx),
            )
        } else emptyList()
        val orderContentWidthPx = (
                listOfNotNull(orderNumberLayout?.size?.width) +
                        orderItemFirstLineWidths +
                        orderItemDetailLayouts.map { it.size.width } +
                        orderFieldTitleWidths +
                        orderFieldValueLayouts.map { it.size.width }
                ).maxOrNull() ?: 0
        val orderLastLineWidthPx = when {
            orderFieldValueLayouts.isNotEmpty() -> {
                toMeasuredText(
                    layout = orderFieldValueLayouts.last(),
                    sampleText = message.orderFields.last().value,
                    direction = ConsultantMessageTextDirection.LTR,
                ).lastLineWidthPx
            }

            orderItemDetailLayouts.isNotEmpty() -> {
                toMeasuredText(
                    layout = orderItemDetailLayouts.last(),
                    sampleText = message.orderItems.last().details,
                    direction = ConsultantMessageTextDirection.LTR,
                ).lastLineWidthPx
            }

            orderNumberLayout != null -> {
                toMeasuredText(
                    layout = orderNumberLayout,
                    sampleText = orderNumberText,
                    direction = ConsultantMessageTextDirection.LTR,
                ).lastLineWidthPx
            }

            else -> 0
        }
        val orderLayoutResult = if (isOrderMessage) {
            ConsultantOrderBubbleLayoutRules.resolve(
                ConsultantOrderBubbleLayoutInput(
                    authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                        authorSingleLineLayout.size.width
                    } else {
                        0
                    },
                    orderContentWidthPx = orderContentWidthPx,
                    lastLineWidthPx = orderLastLineWidthPx,
                    meta = ConsultantMeasuredMeta(
                        contentWidthPx = metaContentWidthPx,
                        inlineStartSpacingPx = metaStartSpacingPx,
                        heightPx = metaHeightPx,
                        hasStatusIcon = isVisitor && statusIcon != null,
                    ),
                    maxBubbleContentWidthPx = orderContentMaxWidthPx,
                ),
            )
        } else {
            null
        }
        val fileBubbleHorizontalPaddingPx =
            with(density) { FILE_BUBBLE_HORIZONTAL_PADDING.roundToPx() }
        val fileBubbleVerticalPaddingPx = with(density) { FILE_BUBBLE_VERTICAL_PADDING.roundToPx() }
        val fileContentHeightPx = with(density) { FILE_CONTENT_HEIGHT.roundToPx() }
        val fileIconWidthPx = with(density) { FILE_ICON_BOX_SIZE.roundToPx() }
        val fileIconTextSpacingPx = with(density) { FILE_ICON_TEXT_SPACING.roundToPx() }
        val fileNameLayout = if (isFileMessage) {
            textMeasurer.measure(
                text = message.fileName ?: stringResource(R.string.crm_consultant_attachment),
                style = fileNameStyle,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            null
        }
        val fileSizeLayout = if (isFileMessage) {
            textMeasurer.measure(
                text = message.fileSizeLabel ?: stringResource(R.string.crm_consultant_file),
                style = fileSizeStyle,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            null
        }
        val fileContentMaxWidthPx =
            (maxBubbleWidthPx - fileBubbleHorizontalPaddingPx * 2).coerceAtLeast(0)
        val fileLayoutResult =
            if (isFileMessage && fileNameLayout != null && fileSizeLayout != null) {
            ConsultantFileBubbleLayoutRules.resolve(
                ConsultantFileBubbleLayoutInput(
                    authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                        authorSingleLineLayout.size.width
                    } else {
                        0
                    },
                    fileNameWidthPx = fileNameLayout.size.width,
                    fileSizeWidthPx = fileSizeLayout.size.width,
                    iconWidthPx = fileIconWidthPx,
                    iconTextSpacingPx = fileIconTextSpacingPx,
                    fileSizeLineHeightPx = fileSizeLayout.size.height,
                    fileSizeLineBaselinePx = fileSizeLayout.getLineBaseline(0).roundToInt(),
                    meta = ConsultantMeasuredMeta(
                        contentWidthPx = metaContentWidthPx,
                        inlineStartSpacingPx = metaStartSpacingPx,
                        heightPx = metaHeightPx,
                        hasStatusIcon = isVisitor && statusIcon != null,
                    ),
                    metaWidthPx = metaContentWidthPx,
                    maxBubbleContentWidthPx = fileContentMaxWidthPx,
                ),
            )
        } else {
            null
        }
        val contactFormBubblePaddingPx = with(density) { CONTACT_FORM_BUBBLE_PADDING.roundToPx() }
        val contactFormContentMaxWidthPx =
            (maxBubbleWidthPx - contactFormBubblePaddingPx * 2).coerceAtLeast(0)
        val contactFormMessageLayout = if (isContactFormMessage) {
            textMeasurer.measure(
                text = message.text,
                style = bodyStyle,
                constraints = androidx.compose.ui.unit.Constraints(
                    maxWidth = contactFormContentMaxWidthPx,
                ),
            )
        } else {
            null
        }
        val contactFormInputWidthPx = with(density) { CONTACT_FORM_FIELD_WIDTH.roundToPx() }
        val contactFormButtonWidthPx = contactFormInputWidthPx
        val contactFormLayoutResult =
            if (isContactFormMessage && contactFormMessageLayout != null) {
                ConsultantContactFormBubbleLayoutRules.resolve(
                    ConsultantContactFormBubbleLayoutInput(
                        messageWidthPx = contactFormMessageLayout.size.width,
                        inputWidthPx = contactFormInputWidthPx,
                        buttonWidthPx = contactFormButtonWidthPx,
                        maxBubbleContentWidthPx = contactFormContentMaxWidthPx,
                    ),
                )
            } else {
                null
            }
        val measuredBody = if (isTextMessage || isAutoMessage) {
            measureBodyText(contentMaxWidthPx).copy(
                preferredSingleLineWidthPx = requireNotNull(singleLineBodyLayout).size.width,
            )
        } else {
            null
        }
        val autoMessageTitleLayout = if (isAutoMessage) {
            textMeasurer.measure(
                text = message.autoMessageTitle.orEmpty(),
                style = authorStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentMaxWidthPx),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            null
        }
        val autoMessageButtonLayouts = if (isAutoMessage) message.buttons.map { button ->
            textMeasurer.measure(
                text = button.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        } else emptyList()
        val autoMessageImageWidthPx = if (!message.autoMessageImageUrl.isNullOrBlank()) {
            contentMaxWidthPx
        } else {
            0
        }
        val autoMessageLayoutResult = if (
            isAutoMessage && autoMessageTitleLayout != null && measuredBody != null
        ) {
            ConsultantAutoMessageBubbleLayoutRules.resolve(
                ConsultantAutoMessageBubbleLayoutInput(
                    authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                        authorSingleLineLayout.size.width
                    } else {
                        0
                    },
                    titleWidthPx = autoMessageTitleLayout.size.width,
                    body = measuredBody,
                    imageWidthPx = autoMessageImageWidthPx,
                    buttonWidthsPx = autoMessageButtonLayouts.map { layout ->
                        layout.size.width + with(density) { AUTO_MESSAGE_BUTTON_HORIZONTAL_PADDING.roundToPx() * 2 }
                    },
                    meta = ConsultantMeasuredMeta(
                        contentWidthPx = metaContentWidthPx,
                        inlineStartSpacingPx = metaStartSpacingPx,
                        heightPx = metaHeightPx,
                        hasStatusIcon = isVisitor && statusIcon != null,
                    ),
                    maxBubbleContentWidthPx = contentMaxWidthPx,
                ),
            )
        } else {
            null
        }
        val quoteBubblePaddingPx = with(density) { QUOTE_BUBBLE_PADDING.roundToPx() }
        val quoteContentMaxWidthPx =
            (maxBubbleWidthPx - quoteBubblePaddingPx * 2).coerceAtLeast(0)
        val quotePrimaryText = quotePrimaryText(message)
        val quoteSecondaryText = quoteSecondaryText(message)
        val quoteHasThumbnail = message.quoteImageUrl != null
        val quoteLeadingWidthPx = with(density) {
            (
                    QUOTE_BAR_WIDTH +
                            QUOTE_BAR_CONTENT_SPACING +
                            if (quoteHasThumbnail) {
                                QUOTE_THUMBNAIL_SIZE + QUOTE_THUMBNAIL_TEXT_SPACING
                            } else {
                                0.dp
                            }
                    ).roundToPx()
        }
        val quoteTextMaxWidthPx = (quoteContentMaxWidthPx - quoteLeadingWidthPx).coerceAtLeast(1)
        val quotePrimaryLayout = if (isQuoteMessage) {
            textMeasurer.measure(
                text = quotePrimaryText,
                style = fileSizeStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = quoteTextMaxWidthPx),
                maxLines = if (message.quoteFileName != null || message.quoteImageUrl != null) 1 else 2,
            )
        } else {
            null
        }
        val quoteSecondaryLayout = if (isQuoteMessage) quoteSecondaryText?.let {
            textMeasurer.measure(
                text = it,
                style = fileSizeStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = quoteTextMaxWidthPx),
                maxLines = 1,
                softWrap = false,
            )
        } else null
        val quoteContentWidthPx = (
                quoteLeadingWidthPx +
                        maxOf(
                            quotePrimaryLayout?.size?.width ?: 0,
                            quoteSecondaryLayout?.size?.width ?: 0
                        )
                ).coerceAtMost(quoteContentMaxWidthPx)
        val quoteHeightPx = maxOf(
            (quotePrimaryLayout?.size?.height ?: 0) + (quoteSecondaryLayout?.size?.height ?: 0),
            with(density) {
                if (quoteHasThumbnail) QUOTE_THUMBNAIL_SIZE.roundToPx() else QUOTE_TEXT_MIN_HEIGHT.roundToPx()
            },
        )
        val quoteMeasuredBody = if (isQuoteMessage) {
            measureBodyText(quoteContentMaxWidthPx).copy(
                preferredSingleLineWidthPx = requireNotNull(singleLineBodyLayout).size.width,
            )
        } else {
            null
        }
        val quoteLayoutResult = if (isQuoteMessage && quoteMeasuredBody != null) {
            ConsultantQuoteBubbleLayoutRules.resolveStable(
                ConsultantQuoteBubbleLayoutInput(
                    authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                        authorSingleLineLayout.size.width
                    } else {
                        0
                    },
                    body = quoteMeasuredBody,
                    quoteContentWidthPx = quoteContentWidthPx,
                    meta = ConsultantMeasuredMeta(
                        contentWidthPx = metaContentWidthPx,
                        inlineStartSpacingPx = metaStartSpacingPx,
                        heightPx = metaHeightPx,
                        hasStatusIcon = isVisitor && statusIcon != null,
                    ),
                    maxBubbleContentWidthPx = quoteContentMaxWidthPx,
                ),
            ) { contentWidthPx ->
                measureBodyText(contentWidthPx).copy(
                    preferredSingleLineWidthPx = requireNotNull(singleLineBodyLayout).size.width,
                )
            }
        } else {
            null
        }
        val needsTextLayoutResult = isTextMessage || (isAutoMessage && onLayoutClick != null)
        val layoutResult = if (needsTextLayoutResult && measuredBody != null) {
            ConsultantMessageBubbleLayoutRules.resolveStable(
            ConsultantMessageBubbleLayoutInput(
                author = ConsultantMeasuredAuthor(
                    sampleText = message.authorName,
                    preferredSingleLineWidthPx = authorSingleLineLayout.size.width,
                    layoutWidthPx = authorLayout.size.width,
                    canWrap = message.authorName.contains(' '),
                ),
                body = measuredBody,
                meta = ConsultantMeasuredMeta(
                    contentWidthPx = metaContentWidthPx,
                    inlineStartSpacingPx = metaStartSpacingPx,
                    heightPx = metaHeightPx,
                    hasStatusIcon = isVisitor && statusIcon != null,
                ),
                maxBubbleContentWidthPx = contentMaxWidthPx,
            ),
            remeasureBody = { contentWidthPx ->
                measureBodyText(contentWidthPx).copy(
                    preferredSingleLineWidthPx = requireNotNull(singleLineBodyLayout).size.width,
                )
            },
            )
        } else {
            null
        }
        val maxBubbleWidthDp = with(density) { maxBubbleWidthPx.toDp() }
        val bubbleWidthDp = layoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + horizontalPaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val autoMessageBubbleWidthDp = autoMessageLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + horizontalPaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val autoMessageContentWidthDp = autoMessageLayoutResult?.let { result ->
            with(density) { result.bubbleContentWidthPx.toDp() }
        }
        val imageBubbleWidthDp = imageLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + IMAGE_BUBBLE_PADDING.roundToPx() * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val imageWidthDp = imageLayoutResult?.let { result ->
            with(density) { result.imageWidthPx.toDp() }
        }
        val imageHeightDp = imageLayoutResult?.let { result ->
            with(density) { result.imageHeightPx.toDp() }
        }
        val productBubbleWidthDp = productLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + productBubblePaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val productContentWidthDp = productLayoutResult?.let { result ->
            with(density) { result.productContentWidthPx.toDp() }
        }
        val orderBubbleWidthDp = orderLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + orderBubblePaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val orderContentWidthDp = orderLayoutResult?.let { result ->
            with(density) { result.orderContentWidthPx.toDp() }
        }
        val productImageWidthDp = productLayoutResult?.let { result ->
            with(density) { result.productImageWidthPx.toDp() }
        }
        val productImageHeightDp = productLayoutResult?.let { result ->
            with(density) { result.productImageHeightPx.toDp() }
        }
        val fileBubbleWidthDp = fileLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + fileBubbleHorizontalPaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val fileContentWidthDp = fileLayoutResult?.let { result ->
            with(density) { result.fileContentWidthPx.toDp() }
        }
        val quoteBubbleWidthDp = quoteLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + quoteBubblePaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val quoteContentWidthDp = quoteLayoutResult?.let { result ->
            with(density) { result.quoteContentWidthPx.toDp() }
        }
        val contactFormBubbleWidthDp = contactFormLayoutResult?.let { result ->
            with(density) {
                (result.bubbleContentWidthPx + contactFormBubblePaddingPx * 2).toDp()
            }.coerceAtMost(maxBubbleWidthDp)
        }
        val contactFormContentWidthDp = contactFormLayoutResult?.let { result ->
            with(density) { result.formContentWidthPx.toDp() }
        }
        val textLayoutDebug = if (
            onLayoutClick != null && measuredBody != null && layoutResult != null &&
            bubbleWidthDp != null
        ) {
            buildTextLayoutDebug(
                message = message,
                maxBubbleWidthPx = maxBubbleWidthPx,
                horizontalPaddingPx = horizontalPaddingPx,
                verticalPaddingPx = verticalPaddingPx,
                contentMaxWidthPx = contentMaxWidthPx,
                authorWidthPx = authorSingleLineLayout.size.width,
                body = measuredBody,
                metaWidthPx = metaContentWidthPx,
                metaHeightPx = metaHeightPx,
                layoutResult = layoutResult,
                bubbleWidthPx = with(density) { bubbleWidthDp.roundToPx() },
            )
        } else {
            null
        }
        val imageLayoutDebug = imageLayoutResult?.takeIf { onLayoutClick != null }?.let { result ->
            buildImageLayoutDebug(
                message = message,
                maxBubbleWidthPx = maxBubbleWidthPx,
                imageBubblePaddingPx = imageBubblePaddingPx,
                imageContentMaxWidthPx = imageContentMaxWidthPx,
                imageContentMaxHeightPx = contentMaxHeightPx,
                imageWidthPx = imageSourceWidthPx,
                imageHeightPx = imageSourceHeightPx,
                authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                    authorSingleLineLayout.size.width
                } else {
                    0
                },
                metaWidthPx = imageMetaOverlayWidthPx,
                metaContentWidthPx = metaContentWidthPx,
                metaHeightPx = metaHeightPx,
                result = result,
                bubbleWidthPx = with(density) { imageBubbleWidthDp?.roundToPx() ?: 0 },
            )
        }
        val productLayoutDebug =
            productLayoutResult?.takeIf { onLayoutClick != null }?.let { result ->
                buildProductLayoutDebug(
                    message = message,
                    maxBubbleWidthPx = maxBubbleWidthPx,
                    horizontalPaddingPx = productBubblePaddingPx,
                    contentMaxWidthPx = productContentMaxWidthPx,
                    productTitleWidthPx = requireNotNull(productTitleLayout).size.width,
                    productInfoWidthPx = requireNotNull(productPriceLayout).size.width,
                    productImageWidthPx = productImageWidthPx,
                    productImageHeightPx = productImageHeightPx,
                    metaWidthPx = metaContentWidthPx,
                    metaHeightPx = metaHeightPx,
                    result = result,
                    bubbleWidthPx = with(density) { productBubbleWidthDp?.roundToPx() ?: 0 },
                )
            }
        val orderLayoutDebug = orderLayoutResult?.takeIf { onLayoutClick != null }?.let { result ->
            buildOrderLayoutDebug(
                message = message,
                maxBubbleWidthPx = maxBubbleWidthPx,
                horizontalPaddingPx = orderBubblePaddingPx,
                contentMaxWidthPx = orderContentMaxWidthPx,
                orderContentWidthPx = orderContentWidthPx,
                orderLastLineWidthPx = orderLastLineWidthPx,
                metaWidthPx = metaContentWidthPx,
                metaHeightPx = metaHeightPx,
                result = result,
                bubbleWidthPx = with(density) { orderBubbleWidthDp?.roundToPx() ?: 0 },
            )
        }
        val fileLayoutDebug = fileLayoutResult?.takeIf { onLayoutClick != null }?.let { result ->
            buildFileLayoutDebug(
                message = message,
                maxBubbleWidthPx = maxBubbleWidthPx,
                horizontalPaddingPx = fileBubbleHorizontalPaddingPx,
                verticalPaddingPx = fileBubbleVerticalPaddingPx,
                contentMaxWidthPx = fileContentMaxWidthPx,
                fileNameWidthPx = requireNotNull(fileNameLayout).size.width,
                fileSizeWidthPx = requireNotNull(fileSizeLayout).size.width,
                iconWidthPx = fileIconWidthPx,
                iconTextSpacingPx = fileIconTextSpacingPx,
                fileContentHeightPx = fileContentHeightPx,
                authorWidthPx = if (shouldShowAuthor(message, isFollowFromPrevious)) {
                    authorSingleLineLayout.size.width
                } else {
                    0
                },
                metaWidthPx = metaContentWidthPx,
                metaHeightPx = metaHeightPx,
                result = result,
                bubbleWidthPx = with(density) { fileBubbleWidthDp?.roundToPx() ?: 0 },
            )
        }
        val quoteLayoutDebug = quoteLayoutResult?.takeIf { onLayoutClick != null }?.let { result ->
            buildQuoteLayoutDebug(
                message = message,
                maxBubbleWidthPx = maxBubbleWidthPx,
                horizontalPaddingPx = quoteBubblePaddingPx,
                contentMaxWidthPx = quoteContentMaxWidthPx,
                body = requireNotNull(quoteMeasuredBody),
                quotePrimaryWidthPx = requireNotNull(quotePrimaryLayout).size.width,
                quoteSecondaryWidthPx = quoteSecondaryLayout?.size?.width ?: 0,
                quoteContentWidthPx = quoteContentWidthPx,
                quoteHeightPx = quoteHeightPx,
                metaWidthPx = metaContentWidthPx,
                metaHeightPx = metaHeightPx,
                result = result,
                bubbleWidthPx = with(density) { quoteBubbleWidthDp?.roundToPx() ?: 0 },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isVisitor) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isContactFormMessage && shouldShowAvatar(message, isFollowToNext)) {
                ConsultantAvatar(message = message)
                Spacer(modifier = Modifier.width(8.dp))
            } else if (!isContactFormMessage && shouldReserveAvatarSlot(message, isFollowToNext)) {
                Spacer(modifier = Modifier.width(AVATAR_SLOT_WIDTH))
            }

            if (contactForm != null && contactFormLayoutResult != null &&
                contactFormBubbleWidthDp != null && contactFormContentWidthDp != null
            ) {
                ContactFormMessageBubble(
                    message = message,
                    contactForm = contactForm,
                    bubbleWidth = contactFormBubbleWidthDp,
                    formContentWidth = contactFormContentWidthDp,
                    bodyStyle = bodyStyle,
                    onSubmit = onContactFormSubmit,
                )
            } else if (isAutoMessage && autoMessageLayoutResult != null &&
                autoMessageBubbleWidthDp != null && autoMessageContentWidthDp != null
            ) {
                AutoMessageBubble(
                    message = message,
                    imagePainter = autoMessageImagePainter,
                    statusIcon = statusIcon,
                    layoutResult = autoMessageLayoutResult,
                    bubbleWidth = autoMessageBubbleWidthDp,
                    contentWidth = autoMessageContentWidthDp,
                    isVisitor = isVisitor,
                    bodyStyle = bodyStyle,
                    authorStyle = authorStyle,
                    titleStyle = authorStyle,
                    bodyBubbleDirection = bodyBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    onQuickResponseClick = onQuickResponseClick,
                    layoutDebug = textLayoutDebug.orEmpty(),
                )
            } else if (isQuoteMessage && quoteLayoutResult != null && quoteBubbleWidthDp != null &&
                quoteContentWidthDp != null
            ) {
                QuoteMessageBubble(
                    message = message,
                    statusIcon = statusIcon,
                    quoteLayoutResult = quoteLayoutResult,
                    bubbleWidth = quoteBubbleWidthDp,
                    quoteContentWidth = quoteContentWidthDp,
                    isVisitor = isVisitor,
                    bodyStyle = bodyStyle,
                    fileSizeStyle = fileSizeStyle,
                    authorStyle = authorStyle,
                    authorBubbleDirection = authorBubbleDirection,
                    bodyBubbleDirection = bodyBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    onQuoteClick = onQuoteClick,
                    layoutDebug = quoteLayoutDebug.orEmpty(),
                )
            } else if (isOrderMessage && orderLayoutResult != null &&
                orderBubbleWidthDp != null && orderContentWidthDp != null
            ) {
                OrderMessageBubble(
                    message = message,
                    statusIcon = statusIcon,
                    orderLayoutResult = orderLayoutResult,
                    bubbleWidth = orderBubbleWidthDp,
                    orderContentWidth = orderContentWidthDp,
                    isVisitor = isVisitor,
                    orderItemTitleStyle = orderItemTitleStyle,
                    orderPrimaryStyle = orderPrimaryStyle,
                    orderSecondaryStyle = orderSecondaryStyle,
                    authorStyle = authorStyle,
                    authorBubbleDirection = authorBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    layoutDebug = orderLayoutDebug.orEmpty(),
                )
            } else if (isProductMessage && productLayoutResult != null &&
                productBubbleWidthDp != null && productContentWidthDp != null &&
                productImageWidthDp != null && productImageHeightDp != null
            ) {
                ProductMessageBubble(
                    message = message,
                    statusIcon = statusIcon,
                    productImagePainter = productImagePainter,
                    productLayoutResult = productLayoutResult,
                    bubbleWidth = productBubbleWidthDp,
                    productContentWidth = productContentWidthDp,
                    productImageWidth = productImageWidthDp,
                    productImageHeight = productImageHeightDp,
                    isVisitor = isVisitor,
                    productTitleStyle = productTitleStyle,
                    productPriceStyle = productPriceStyle,
                    authorStyle = authorStyle,
                    authorBubbleDirection = authorBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    onImageClick = onImageClick,
                    layoutDebug = productLayoutDebug.orEmpty(),
                )
            } else if (imageUrl != null && imagePainter != null && imageLayoutResult != null && imageBubbleWidthDp != null &&
                imageWidthDp != null && imageHeightDp != null
            ) {
                ImageMessageBubble(
                    message = message,
                    imagePainter = imagePainter,
                    statusIcon = statusIcon,
                    imageLayoutResult = imageLayoutResult,
                    bubbleWidth = imageBubbleWidthDp,
                    imageWidth = imageWidthDp,
                    imageHeight = imageHeightDp,
                    isVisitor = isVisitor,
                    authorStyle = authorStyle,
                    authorBubbleDirection = authorBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    onImageClick = onImageClick,
                    layoutDebug = imageLayoutDebug.orEmpty(),
                )
            } else if (isFileMessage && fileLayoutResult != null && fileBubbleWidthDp != null &&
                fileContentWidthDp != null
            ) {
                FileMessageBubble(
                    message = message,
                    statusIcon = statusIcon,
                    fileLayoutResult = fileLayoutResult,
                    bubbleWidth = fileBubbleWidthDp,
                    fileContentWidth = fileContentWidthDp,
                    isVisitor = isVisitor,
                    fileNameStyle = fileNameStyle,
                    fileSizeStyle = fileSizeStyle,
                    authorStyle = authorStyle,
                    authorBubbleDirection = authorBubbleDirection,
                    shouldShowAuthor = shouldShowAuthor(message, isFollowFromPrevious),
                    onLayoutClick = onLayoutClick,
                    onFileIconClick = onFileIconClick,
                    layoutDebug = fileLayoutDebug.orEmpty(),
                )
            } else {
                val resolvedBubbleWidth = requireNotNull(bubbleWidthDp)
                val resolvedLayoutResult = requireNotNull(layoutResult)
                val shape = bubbleShapeFor(isVisitor, visualTheme.roundedElements)
                Surface(
                    modifier = Modifier
                        .width(resolvedBubbleWidth)
                        .clip(shape)
                        .then(
                            if (onLayoutClick != null) {
                                Modifier.clickable { onLayoutClick(textLayoutDebug.orEmpty()) }
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
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (shouldShowAuthor(message, isFollowFromPrevious)) {
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
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AnnotatedLinkText(
                                text = bodyAnnotatedText,
                                color = bubbleContentColorFor(isVisitor, visualTheme),
                                style = bodyStyle,
                                textAlign = bubbleTextAlignFor(bodyBubbleDirection),
                                linkHighlightColor = messageLinkColor,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (resolvedLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.INLINE_OVERLAY) {
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
                                                y = resolvedLayoutResult.metaOverlayOffsetYPx,
                                            )
                                        },
                                )
                            }
                        }

                        if (resolvedLayoutResult.metaPlacement == ConsultantMessageMetaPlacement.BELOW_TEXT) {
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

        }
    }
}
