package com.simla.android.sdk.consultant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsultantGenerationTest {
    @Test
    fun `snapshot becomes stale after generation advances`() {
        val generation = ConsultantGeneration()
        val firstSession = generation.snapshot()

        generation.advance()

        assertFalse(generation.isCurrent(firstSession))
        assertTrue(generation.isCurrent(generation.snapshot()))
    }

    @Test
    fun `each advance invalidates the previous connection`() {
        val generation = ConsultantGeneration()
        val firstConnection = generation.advance()
        val secondConnection = generation.advance()

        assertFalse(generation.isCurrent(firstConnection))
        assertTrue(generation.isCurrent(secondConnection))
    }
}
