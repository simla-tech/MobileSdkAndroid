package com.simla.android.sdk.consultant.ui

internal fun List<ConsultantChatListItem>.debugListItemKeys(limit: Int = 8): String {
    if (isEmpty()) {
        return "[]"
    }

    val head = take(limit).joinToString(prefix = "[", postfix = "]") { it.key }
    return if (size > limit) {
        "$head ... last=${last().key}"
    } else {
        head
    }
}
