package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import java.util.Calendar

internal fun buildMessageListItems(
    messages: List<OnlineConsultantMessageUiModel>,
    todayLabel: String,
    yesterdayLabel: String,
): List<ConsultantChatListItem> {
    val today = Calendar.getInstance()
    val todayKey = calendarDayKey(today)
    val yesterdayKey = (today.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.let(::calendarDayKey)
    var previousDayKey: String? = null

    return buildList {
        messages.forEachIndexed { index, message ->
            val messageDate = localMessageDate(message.sentAt) ?: (today.clone() as Calendar)
            val messageDayKey = calendarDayKey(messageDate)
            if (messageDayKey != previousDayKey) {
                add(
                    ConsultantChatListItem.DateSeparator(
                        dayKey = messageDayKey,
                        label = formatMessageDateLabel(
                            messageDate,
                            todayKey,
                            yesterdayKey,
                            todayLabel,
                            yesterdayLabel,
                        ),
                    ),
                )
                previousDayKey = messageDayKey
            }

            add(
                ConsultantChatListItem.Message(
                    message = message,
                    previousMessage = messages.getOrNull(index - 1),
                    nextMessage = messages.getOrNull(index + 1),
                ),
            )
        }
    }
}

internal fun MutableList<ConsultantChatListItem>.reconcileByKey(
    incoming: List<ConsultantChatListItem>,
) {
    val incomingMessageIds = incoming.mapNotNullTo(hashSetOf()) { item ->
        (item as? ConsultantChatListItem.Message)?.message?.id
    }
    val normalizedIncoming = incoming.mapIndexed { targetIndex, item ->
        if (item !is ConsultantChatListItem.Message) {
            return@mapIndexed item
        }

        val currentWithSameMessageId = firstOrNull { current ->
            current is ConsultantChatListItem.Message &&
                    current.message.id == item.message.id
        } as? ConsultantChatListItem.Message
        if (currentWithSameMessageId != null) {
            return@mapIndexed item.copy(key = currentWithSameMessageId.key)
        }

        val currentAtTarget = getOrNull(targetIndex) as? ConsultantChatListItem.Message
        if (currentAtTarget?.isPendingReplacementBy(item, incomingMessageIds) == true) {
            item.copy(key = currentAtTarget.key)
        } else {
            item
        }
    }
    val incomingKeys = normalizedIncoming.mapTo(hashSetOf()) { it.key }
    var index = 0
    while (index < size) {
        if (this[index].key !in incomingKeys) {
            removeAt(index)
        } else {
            index += 1
        }
    }

    normalizedIncoming.forEachIndexed { targetIndex, item ->
        val current = getOrNull(targetIndex)
        if (current?.key == item.key) {
            if (current != item) {
                this[targetIndex] = item
            }
            return@forEachIndexed
        }

        val existingIndex = indexOfFirst { it.key == item.key }
        if (existingIndex >= 0) {
            removeAt(existingIndex)
        }
        add(targetIndex, item)
    }

    while (size > normalizedIncoming.size) {
        removeAt(lastIndex)
    }
}

private fun ConsultantChatListItem.Message.isPendingReplacementBy(
    incoming: ConsultantChatListItem.Message,
    incomingMessageIds: Set<String>,
): Boolean {
    return message.id.startsWith(LOCAL_MESSAGE_ID_PREFIX) &&
            message.id !in incomingMessageIds &&
            !incoming.message.id.startsWith(LOCAL_MESSAGE_ID_PREFIX) &&
            message.author == incoming.message.author &&
            message.text == incoming.message.text &&
            message.fileName == incoming.message.fileName
}

internal fun List<ConsultantChatListItem>.hasNewerMessageThan(
    current: List<ConsultantChatListItem>,
): Boolean {
    val currentNewestMessageId = current.firstNotNullOfOrNull { item ->
        (item as? ConsultantChatListItem.Message)?.message?.id
    } ?: return false
    val incomingNewestMessageId = firstNotNullOfOrNull { item ->
        (item as? ConsultantChatListItem.Message)?.message?.id
    } ?: return false

    return incomingNewestMessageId != currentNewestMessageId &&
            any { item ->
                item is ConsultantChatListItem.Message &&
                        item.message.id == currentNewestMessageId
            }
}

private const val LOCAL_MESSAGE_ID_PREFIX = "local-"

internal fun findQuotedMessageListItemIndex(
    listItems: List<ConsultantChatListItem>,
    pending: ConsultantPendingQuoteScroll,
): Int? {
    pending.targetMessageId
        ?.takeIf { it.isNotBlank() }
        ?.let { targetMessageId ->
            return listItems.indexOfFirst { item ->
                item is ConsultantChatListItem.Message && item.message.id == targetMessageId
            }.takeIf { it >= 0 }
        }

    val normalizedQuoteText = pending.quoteText.normalizedQuoteText() ?: return null
    val sourceIndex = listItems.indexOfFirst { item ->
        item is ConsultantChatListItem.Message && item.message.id == pending.sourceMessageId
    }
    val candidateRange = if (sourceIndex >= 0) {
        sourceIndex - 1 downTo 0
    } else {
        listItems.lastIndex downTo 0
    }

    return candidateRange.firstOrNull { index ->
        val item = listItems[index] as? ConsultantChatListItem.Message ?: return@firstOrNull false
        item.message.text.normalizedQuoteText() == normalizedQuoteText
    }
}
