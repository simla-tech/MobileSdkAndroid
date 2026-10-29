package com.simla.android.sdk.notifications.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PushInteractionPendingIntentsTest {
    @Test
    fun `hashes exact button value with sha256`() {
        assertEquals(
            "43a7f8908ce2f884e300d9ae570ac276af71ad2cc97c48e470cf37eebb0dd975",
            PushInteractionPendingIntents.buttonHash("sample-shop://orders/123"),
        )
    }

    @Test
    fun `does not normalize button value before hashing`() {
        assertNotEquals(
            PushInteractionPendingIntents.buttonHash("sample-shop://orders/123"),
            PushInteractionPendingIntents.buttonHash(" sample-shop://orders/123 "),
        )
    }
}
