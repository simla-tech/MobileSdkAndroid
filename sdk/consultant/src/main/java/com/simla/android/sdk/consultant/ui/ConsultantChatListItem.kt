package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal sealed interface ConsultantChatListItem {
    val key: String

    data class DateSeparator(
        val dayKey: String,
        val label: String,
    ) : ConsultantChatListItem {
        override val key: String = "date-$dayKey"
    }

    data class Message(
        val message: OnlineConsultantMessageUiModel,
        val previousMessage: OnlineConsultantMessageUiModel?,
        val nextMessage: OnlineConsultantMessageUiModel?,
        override val key: String = "message-${message.id}",
    ) : ConsultantChatListItem
}
