package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.unit.dp

internal val CONTACT_FORM_BUBBLE_PADDING = 10.dp

internal val CONTACT_FORM_ITEM_SPACING = 8.dp

internal val AUTO_MESSAGE_BUTTON_HORIZONTAL_PADDING = 12.dp

internal val CONTACT_FORM_FIELD_WIDTH = 312.dp

internal val CONTACT_FORM_FIELD_SLOT_MIN_HEIGHT = 75.dp

internal val CONTACT_FORM_FIELD_MIN_HEIGHT = 56.dp

internal val CONTACT_FORM_BUTTON_HEIGHT = 36.dp

internal const val CONTACT_FORM_NAME_MAX_LENGTH = 100

internal const val CONTACT_FORM_EMAIL_MAX_LENGTH = 50

internal const val CONTACT_FORM_PHONE_MAX_LENGTH = 50

internal const val CONTACT_FORM_COMMENT_MAX_LENGTH = 1000

internal val CONTACT_FORM_EMAIL_REGEX =
    Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
