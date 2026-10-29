package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun isFollowMessage(
    previous: OnlineConsultantMessageUiModel?,
    current: OnlineConsultantMessageUiModel,
): Boolean {
    if (previous == null || previous.author != current.author) {
        return false
    }

    val previousTime = parseFollowMessageTime(previous.sentAt) ?: return false
    val currentTime = parseFollowMessageTime(current.sentAt) ?: return false
    if (kotlin.math.abs(currentTime - previousTime) >= FOLLOW_MESSAGE_WINDOW_MS) {
        return false
    }

    if (current.author != OnlineConsultantMessageAuthor.VISITOR &&
        (previous.authorName != current.authorName || previous.authorAvatarUrl != current.authorAvatarUrl)
    ) {
        return false
    }

    return true
}

internal fun shouldShowAvatar(
    message: OnlineConsultantMessageUiModel,
    isFollowToNext: Boolean,
): Boolean {
    if (message.author == OnlineConsultantMessageAuthor.VISITOR || isFollowToNext) {
        return false
    }

    return message.author == OnlineConsultantMessageAuthor.CONSULTANT ||
            message.author == OnlineConsultantMessageAuthor.BOT ||
            message.authorAvatarUrl != null
}

internal fun shouldReserveAvatarSlot(
    message: OnlineConsultantMessageUiModel,
    isFollowToNext: Boolean,
): Boolean {
    return isFollowToNext && message.author != OnlineConsultantMessageAuthor.VISITOR
}

internal fun shouldShowAuthor(
    message: OnlineConsultantMessageUiModel,
    isFollowFromPrevious: Boolean,
): Boolean {
    return !isFollowFromPrevious
}

internal fun parseFollowMessageTime(value: String?): Long? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        parseIsoUtcDate(value)?.time
    }.getOrNull()
}
