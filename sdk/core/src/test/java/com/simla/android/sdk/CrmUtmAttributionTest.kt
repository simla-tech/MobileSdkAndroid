package com.simla.android.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrmUtmAttributionTest {
    @Test
    fun attribution_is_kept_in_process_memory_until_replaced() {
        val first = CrmUtmAttribution(source = "push", campaign = "first")
        val second = CrmUtmAttribution(source = "push", campaign = "second")

        try {
            CrmUtmAttributionStorage.save(first)
            assertEquals(first, CrmUtmAttributionStorage.load())

            CrmUtmAttributionStorage.save(second)
            assertEquals(second, CrmUtmAttributionStorage.load())
        } finally {
            CrmUtmAttributionStorage.clear()
        }
    }

    @Test
    fun empty_attribution_clears_process_memory() {
        try {
            CrmUtmAttributionStorage.save(CrmUtmAttribution(source = "push"))
            CrmUtmAttributionStorage.save(CrmUtmAttribution())

            assertNull(CrmUtmAttributionStorage.load())
        } finally {
            CrmUtmAttributionStorage.clear()
        }
    }
}
