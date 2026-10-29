package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineConsultantFollowMessageTest {
    @Test
    fun consultant_message_within_five_minutes_and_same_author_is_follow() {
        val previous = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )
        val current = message(
            id = "2",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:04:59Z",
            avatarUrl = null,
        )

        assertTrue(isFollowMessage(previous, current))
    }

    @Test
    fun consultant_message_after_five_minutes_is_not_follow() {
        val previous = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )
        val current = message(
            id = "2",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:05:00Z",
            avatarUrl = null,
        )

        assertFalse(isFollowMessage(previous, current))
    }

    @Test
    fun consultant_message_with_different_author_name_is_not_follow() {
        val previous = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )
        val current = message(
            id = "2",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Maria",
            sentAt = "2026-04-22T10:04:00Z",
            avatarUrl = null,
        )

        assertFalse(isFollowMessage(previous, current))
    }

    @Test
    fun visitor_message_is_follow_only_by_author_and_time() {
        val previous = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )
        val current = message(
            id = "2",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:01:00Z",
            avatarUrl = null,
        )

        assertTrue(isFollowMessage(previous, current))
    }

    @Test
    fun avatar_is_hidden_when_message_has_followup_after_it() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertFalse(shouldShowAvatar(message, isFollowToNext = true))
    }

    @Test
    fun avatar_is_shown_for_last_message_in_incoming_chain() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldShowAvatar(message, isFollowToNext = false))
    }

    @Test
    fun author_is_shown_for_first_outgoing_message_in_chain() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldShowAuthor(message, isFollowFromPrevious = false))
    }

    @Test
    fun author_is_shown_for_first_incoming_message_in_chain() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldShowAuthor(message, isFollowFromPrevious = false))
    }

    @Test
    fun author_is_hidden_for_follow_messages() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertFalse(shouldShowAuthor(message, isFollowFromPrevious = true))
    }

    @Test
    fun avatar_is_hidden_for_visitor_messages() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertFalse(shouldShowAvatar(message, isFollowToNext = false))
    }

    @Test
    fun consultant_avatar_is_shown_even_without_image_url() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldShowAvatar(message, isFollowToNext = false))
    }

    @Test
    fun bot_avatar_is_shown_even_without_explicit_image_url() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.BOT,
            authorName = "Bot",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldShowAvatar(message, isFollowToNext = false))
    }

    @Test
    fun followup_incoming_message_reserves_avatar_slot() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertTrue(shouldReserveAvatarSlot(message, isFollowToNext = true))
    }

    @Test
    fun first_incoming_message_does_not_reserve_avatar_slot_when_avatar_is_visible() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.CONSULTANT,
            authorName = "Anna",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertFalse(shouldReserveAvatarSlot(message, isFollowToNext = false))
    }

    @Test
    fun outgoing_message_does_not_reserve_avatar_slot() {
        val message = message(
            id = "1",
            author = OnlineConsultantMessageAuthor.VISITOR,
            authorName = "You",
            sentAt = "2026-04-22T10:00:00Z",
            avatarUrl = null,
        )

        assertFalse(shouldReserveAvatarSlot(message, isFollowToNext = true))
    }

    private fun message(
        id: String,
        author: OnlineConsultantMessageAuthor,
        authorName: String,
        sentAt: String,
        avatarUrl: String?,
    ): OnlineConsultantMessageUiModel {
        return OnlineConsultantMessageUiModel(
            id = id,
            author = author,
            authorName = authorName,
            text = "text",
            timeLabel = "10:00",
            sentAt = sentAt,
            authorAvatarUrl = avatarUrl,
            deliveryStatus = OnlineConsultantMessageDeliveryStatus.NONE,
        )
    }
}
