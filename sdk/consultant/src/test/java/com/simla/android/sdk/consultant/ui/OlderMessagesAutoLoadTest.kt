package com.simla.android.sdk.consultant.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OlderMessagesAutoLoadTest {
    @Test
    fun `loads once for a new oldest visible message`() {
        assertTrue(
            shouldAutoLoadOlderMessages(
                isAtOlderMessagesEdge = true,
                oldestVisibleMessageId = "message-42",
                lastAutoLoadAnchorMessageId = "message-43",
                canLoadOlderMessages = true,
                isLoadingOlderMessages = false,
                isInitialLoading = false,
            ),
        )
    }

    @Test
    fun `does not reload for the same oldest visible message`() {
        assertFalse(
            shouldAutoLoadOlderMessages(
                isAtOlderMessagesEdge = true,
                oldestVisibleMessageId = "message-42",
                lastAutoLoadAnchorMessageId = "message-42",
                canLoadOlderMessages = true,
                isLoadingOlderMessages = false,
                isInitialLoading = false,
            ),
        )
    }

    @Test
    fun `does not load while history state blocks pagination`() {
        assertFalse(
            shouldAutoLoadOlderMessages(
                isAtOlderMessagesEdge = true,
                oldestVisibleMessageId = "message-42",
                lastAutoLoadAnchorMessageId = null,
                canLoadOlderMessages = true,
                isLoadingOlderMessages = true,
                isInitialLoading = false,
            ),
        )
        assertFalse(
            shouldAutoLoadOlderMessages(
                isAtOlderMessagesEdge = true,
                oldestVisibleMessageId = "message-42",
                lastAutoLoadAnchorMessageId = null,
                canLoadOlderMessages = false,
                isLoadingOlderMessages = false,
                isInitialLoading = false,
            ),
        )
    }
}
