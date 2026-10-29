package com.simla.android.sdk.consultant.ui.models

internal fun List<OnlineConsultantMessageUiModel>.lastIncomingReadMessageId(): Long {
    return mapNotNull { message ->
        if (
            message.author != OnlineConsultantMessageAuthor.VISITOR &&
            message.deliveryStatus == OnlineConsultantMessageDeliveryStatus.READ
        ) {
            message.id.toLongOrNull()
        } else {
            null
        }
    }.maxOrNull() ?: 0L
}

internal fun List<OnlineConsultantMessageUiModel>.sortedForDisplay(): List<OnlineConsultantMessageUiModel> {
    return sortedWith(
        compareBy<OnlineConsultantMessageUiModel> { it.sortKey == null }
            .thenBy { it.sortKey.orEmpty() }
            .thenBy { it.id },
    )
}

internal fun List<OnlineConsultantMessageUiModel>.debugMessageIds(limit: Int = 6): String {
    if (isEmpty()) {
        return "[]"
    }

    val head = take(limit).joinToString(prefix = "[", postfix = "]") { it.id }
    return if (size > limit) {
        "$head ... last=${last().id}"
    } else {
        head
    }
}
