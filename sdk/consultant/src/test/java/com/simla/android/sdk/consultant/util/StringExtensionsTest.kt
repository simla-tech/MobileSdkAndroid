package com.simla.android.sdk.consultant.util

import com.simla.android.sdk.util.toIso8601EpochMillis
import com.simla.android.sdk.util.toIso8601Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringExtensionsTest {
    @Test
    fun `ISO timestamp parser accounts for offset`() {
        assertEquals(
            "2026-04-15T10:20:30Z".toIso8601EpochMillis(),
            "2026-04-15T12:20:30+02:00".toIso8601EpochMillis(),
        )
    }

    @Test
    fun `ISO timestamp parser truncates sub-millisecond precision`() {
        assertEquals(
            "2026-04-15T10:20:30.123Z".toIso8601EpochMillis(),
            "2026-04-15T10:20:30.123456789Z".toIso8601EpochMillis(),
        )
    }

    @Test
    fun `ISO timestamp parser rejects invalid timestamp`() {
        assertNull("not-a-timestamp".toIso8601EpochMillis())
    }

    @Test
    fun `ISO timestamp formatter produces UTC milliseconds`() {
        val epochMillis = "2026-04-15T10:20:30.123Z".toIso8601EpochMillis()

        assertEquals("2026-04-15T10:20:30.123Z", epochMillis?.toIso8601Timestamp())
    }
}
