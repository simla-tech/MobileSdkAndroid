package com.simla.android.sdk.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultTrackerEventUpdateListenerTest {
    private val event = TrackerEventReference("event-42", TrackerEventKind.CLICK)

    @Test
    fun formats_every_update_with_event_metadata_and_reason() {
        val updates = listOf(
            TrackerEventUpdate.Queued(listOf(event)),
            TrackerEventUpdate.Delivered(listOf(event)),
            TrackerEventUpdate.RetryScheduled(listOf(event), "HTTP 429"),
            TrackerEventUpdate.Rejected(event, "invalid event"),
            TrackerEventUpdate.Dropped(listOf(event), "queue overflow"),
        )
        assertEquals(
            listOf(
                "queued event={id=event-42, type=CLICK}",
                "delivered event={id=event-42, type=CLICK}",
                "retry_scheduled event={id=event-42, type=CLICK} reason=HTTP 429",
                "rejected event={id=event-42, type=CLICK} reason=invalid event",
                "dropped event={id=event-42, type=CLICK} reason=queue overflow",
            ),
            updates.flatMap { it.toLogMessages() },
        )
    }

    @Test
    fun large_batches_log_each_event_on_a_separate_line() {
        val events = (1..1_000).map { TrackerEventReference("event-$it", TrackerEventKind.CART) }
        val messages = TrackerEventUpdate.Delivered(events).toLogMessages()

        assertEquals(1_000, messages.size)
        events.forEachIndexed { index, event ->
            assertEquals("delivered event={id=${event.id}, type=CART}", messages[index])
        }
    }

    @Test
    fun sending_logs_exact_json_payload() {
        val payload = "[{\"data\":{\"email\":\"customer@example.com\"},\"platform\":\"android\"}]"
        assertEquals(
            listOf(
                "sending event={id=event-42, type=CLICK}",
                "sending payload[1/1] firstEventId=event-42: $payload",
            ),
            TrackerEventUpdate.Sending(listOf(event), payload).toLogMessages(),
        )
    }

    @Test
    fun payload_chunks_preserve_full_json_and_surrogate_pairs() {
        val payload = "а".repeat(999) + "\uD83D\uDE00" + "б".repeat(2_000)
        val parts = TrackerEventUpdate.Sending(listOf(event), payload).toLogMessages().drop(1)

        assertEquals(payload, parts.joinToString("") { it.substringAfter(": ") })
        assertEquals(4, parts.size)
        parts.forEachIndexed { index, part ->
            assertEquals(
                true,
                part.startsWith("sending payload[${index + 1}/4] firstEventId=event-42: ")
            )
            assertEquals(true, part.toByteArray(Charsets.UTF_8).size < 4_000)
            assertEquals(false, part.last().isHighSurrogate())
        }
    }

    @Test
    fun empty_update_is_still_logged() {
        assertEquals(
            listOf("queued events=[]"),
            TrackerEventUpdate.Queued(emptyList()).toLogMessages()
        )
    }
}
