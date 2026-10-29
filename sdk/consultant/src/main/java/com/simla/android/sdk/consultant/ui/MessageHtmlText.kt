package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
internal fun AnnotatedLinkText(
    text: AnnotatedString,
    color: Color,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    linkTag: String = MESSAGE_HTML_LINK_TAG,
    linkHighlightColor: Color,
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var pressedLinkRange by remember { mutableStateOf<TextRange?>(null) }
    val highlightColor = remember(linkHighlightColor) { linkHighlightColor.copy(alpha = 0.14f) }

    Text(
        text = text,
        color = color,
        style = style,
        textAlign = textAlign,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .drawBehind {
                val layout = layoutResult ?: return@drawBehind
                val range = pressedLinkRange ?: return@drawBehind
                val horizontalPadding = 4.dp.toPx()
                val verticalPadding = 2.dp.toPx()
                val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                layout.linkHighlightRects(range).forEach { rect ->
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(
                            x = rect.left - horizontalPadding,
                            y = rect.top - verticalPadding,
                        ),
                        size = Size(
                            width = rect.width + horizontalPadding * 2,
                            height = rect.height + verticalPadding * 2,
                        ),
                        cornerRadius = cornerRadius,
                    )
                }
            }
            .pointerInput(text, linkTag, uriHandler) {
                detectTapGestures(
                    onPress = { position ->
                        val layout = layoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(position)
                        val annotation = text.getStringAnnotations(
                            tag = linkTag,
                            start = offset,
                            end = offset,
                        ).firstOrNull() ?: return@detectTapGestures

                        pressedLinkRange = TextRange(annotation.start, annotation.end)
                        val released = tryAwaitRelease()
                        pressedLinkRange = null

                        if (released) {
                            runCatching {
                                uriHandler.openUri(annotation.item)
                            }
                        }
                    },
                )
            },
    )
}

internal fun messageHtmlPlainText(source: String): String {
    return parseMessageHtml(source).plainText
}

internal fun messageHtmlAnnotatedString(
    source: String,
    linkColor: Color,
): AnnotatedString {
    val parsed = parseMessageHtml(source)
    return buildAnnotatedString {
        append(parsed.plainText)
        parsed.links.forEach { link ->
            addStringAnnotation(
                tag = MESSAGE_HTML_LINK_TAG,
                annotation = link.url,
                start = link.start,
                end = link.end,
            )
            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
                start = link.start,
                end = link.end,
            )
        }
    }
}
