package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ConsultantMessageListReconciliationTest {
    @Test
    fun `unchanged list items retain their instances`() {
        val initial = buildMessageListItems(
            messages = listOf(
                message("1"),
                message("2"),
                message("3"),
                message("4"),
            ),
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
        ).asReversed()
        val current = initial.toMutableList()

        val incoming = buildMessageListItems(
            messages = listOf(
                message("1"),
                message("2"),
                message("3"),
                message("4", OnlineConsultantMessageDeliveryStatus.READ),
            ),
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
        ).asReversed()

        current.reconcileByKey(incoming)

        assertEquals(incoming, current)
        assertSame(initial[3], current[3])
        assertSame(initial[4], current[4])
        assertNotSame(initial[0], current[0])
    }

    @Test
    fun `server response replaces pending message under the same lazy list key`() {
        val current = buildMessageListItems(
            messages = listOf(
                message("local-42", OnlineConsultantMessageDeliveryStatus.SENDING)
                    .copy(text = "Same message"),
            ),
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
        ).asReversed().toMutableList()
        val pendingKey = (current.first() as ConsultantChatListItem.Message).key

        val incoming = buildMessageListItems(
            messages = listOf(
                message("42", OnlineConsultantMessageDeliveryStatus.DELIVERED)
                    .copy(text = "Same message"),
            ),
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
        ).asReversed()

        current.reconcileByKey(incoming)

        assertEquals(incoming.size, current.size)
        val replaced = current.first() as ConsultantChatListItem.Message
        assertEquals("42", replaced.message.id)
        assertEquals(pendingKey, replaced.key)
    }

    @Test
    fun `only insertion at newest edge starts placement animation`() {
        val current = reversedItems("1", "2")

        assertEquals(true, reversedItems("1", "2", "3").hasNewerMessageThan(current))
        assertEquals(false, reversedItems("0", "1", "2").hasNewerMessageThan(current))
        assertEquals(
            false,
            reversedItems("1", "42").hasNewerMessageThan(reversedItems("1", "local-42")),
        )
    }

    private fun reversedItems(vararg ids: String): List<ConsultantChatListItem> =
        buildMessageListItems(
            messages = ids.map(::message),
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
        ).asReversed()

    private fun message(
        id: String,
        status: OnlineConsultantMessageDeliveryStatus =
            OnlineConsultantMessageDeliveryStatus.DELIVERED,
    ) = OnlineConsultantMessageUiModel(
        id = id,
        author = OnlineConsultantMessageAuthor.VISITOR,
        authorName = "You",
        text = "Message $id",
        timeLabel = "12:00",
        sentAt = "2026-08-21T12:00:0${id}Z",
        deliveryStatus = status,
    )
}
