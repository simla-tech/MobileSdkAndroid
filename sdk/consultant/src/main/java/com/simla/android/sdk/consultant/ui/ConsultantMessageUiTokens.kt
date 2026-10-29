package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.unit.dp

internal const val MAX_BUBBLE_WIDTH_FRACTION = 0.8f

internal val STATUS_ICON_SIZE = 14.dp

internal val META_INNER_SPACING = 4.dp

internal val META_START_SPACING = 12.dp

internal val IMAGE_BUBBLE_PADDING = 4.dp

internal val FILE_BUBBLE_HORIZONTAL_PADDING = 10.dp

internal val FILE_BUBBLE_VERTICAL_PADDING = 10.dp

internal val FILE_CONTENT_HEIGHT = 50.dp

internal val FILE_ICON_BOX_SIZE = 42.dp

internal val FILE_ICON_TEXT_SPACING = 8.dp

internal val QUOTE_BUBBLE_PADDING = 10.dp

internal val QUOTE_BAR_WIDTH = 2.dp

internal val QUOTE_BAR_CONTENT_SPACING = 10.dp

internal val QUOTE_THUMBNAIL_SIZE = 40.dp

internal val QUOTE_THUMBNAIL_TEXT_SPACING = 8.dp

internal val QUOTE_TEXT_MIN_HEIGHT = 40.dp

internal val ORDER_BUBBLE_PADDING = 10.dp

internal val PRODUCT_BUBBLE_PADDING = 10.dp

internal val PRODUCT_IMAGE_SIZE = 80.dp

internal const val PRODUCT_TITLE_MAX_LINES = 2

internal const val PRODUCT_TITLE_LINK_TAG = "product_title_link"

internal const val ORDER_ITEM_NAME_MAX_LINES = 2

internal const val ORDER_ITEM_NAME_LINK_TAG = "order_item_name_link"

internal const val PERSONAL_DATA_LINK_TAG = "personal_data_link"

internal const val MESSAGE_HTML_LINK_TAG = "message_html_link"

internal val PRODUCT_TITLE_HIGHLIGHT_HORIZONTAL_PADDING = 2.dp

internal val PRODUCT_TITLE_HIGHLIGHT_CORNER_RADIUS = 4.dp

internal val ORDER_ITEM_NAME_HIGHLIGHT_HORIZONTAL_PADDING = 2.dp

internal val ORDER_ITEM_NAME_HIGHLIGHT_CORNER_RADIUS = 4.dp

internal val META_OVERLAY_EDGE_PADDING = 4.dp

internal val META_OVERLAY_HORIZONTAL_PADDING = 2.dp

internal val AVATAR_SLOT_WIDTH = 40.dp

internal const val FOLLOW_MESSAGE_WINDOW_MS = 5 * 60 * 1000L

internal const val SPINNER_MINIMUM_VISIBLE_MS = 400L

internal const val DEFAULT_IMAGE_MESSAGE_WIDTH_PX = 271

internal const val DEFAULT_IMAGE_MESSAGE_HEIGHT_PX = 162

internal val MESSAGE_HTML_ANCHOR_REGEX = Regex(
    pattern = """<a\b[^>]*href\s*=\s*(['"])(.*?)\1[^>]*>(.*?)</a>""",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

internal val MESSAGE_HTML_BREAK_REGEX = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

internal val MESSAGE_HTML_BLOCK_CLOSE_REGEX =
    Regex("""</(p|div|li|ul|ol|h[1-6])\s*>""", RegexOption.IGNORE_CASE)

internal val MESSAGE_HTML_TAG_REGEX =
    Regex("""<[^>]+>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

internal val MESSAGE_HTML_ENTITY_REGEX = Regex("""&(#x?[0-9A-Fa-f]+|[A-Za-z]+);""")

internal val MESSAGE_PLAIN_LINK_REGEX = Regex(
    """(?i)\b((?:https?://|www\.)[^\s<]+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?:/[^\s<]*)?)"""
)

internal val MESSAGE_URL_SCHEME_REGEX = Regex("""(?i)^https?://""")
