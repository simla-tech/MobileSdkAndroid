package com.simla.android.sdk.tracker

import com.simla.android.sdk.tracker.model.TrackerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TrackerOutgoingEventAdapterTest {
    @Test
    fun batch_respects_utf8_body_size() {
        val first = TrackerEvent(type = "click", name = "ascii")
        val second = TrackerEvent(type = "click", name = "кириллица")
        val firstBody = TrackerOutgoingEventAdapter.serialize(listOf(first))
        val bothBody = TrackerOutgoingEventAdapter.serialize(listOf(first, second))
        val limit = bothBody.toByteArray(Charsets.UTF_8).size - 1

        val batch = TrackerOutgoingEventAdapter.prepareBatch(
            events = listOf(first, second),
            maxEventCount = 50,
            maxBodyBytes = limit,
        )

        assertEquals(listOf(first), batch.events)
        assertEquals(firstBody, batch.body)
        assertNull(batch.oversizedEvent)
    }

    @Test
    fun batch_reports_an_event_that_does_not_fit_alone() {
        val event = TrackerEvent(type = "click", name = "слишком большое событие")
        val bodySize = TrackerOutgoingEventAdapter.serialize(listOf(event))
            .toByteArray(Charsets.UTF_8)
            .size

        val batch = TrackerOutgoingEventAdapter.prepareBatch(
            events = listOf(event),
            maxEventCount = 50,
            maxBodyBytes = bodySize - 1,
        )

        assertEquals(emptyList<TrackerEvent>(), batch.events)
        assertEquals("[]", batch.body)
        assertSame(event, batch.oversizedEvent)
    }
}
