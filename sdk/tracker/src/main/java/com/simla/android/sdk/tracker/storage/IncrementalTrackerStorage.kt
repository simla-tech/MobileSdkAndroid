package com.simla.android.sdk.tracker.storage

import com.simla.android.sdk.tracker.model.TrackerEvent

internal interface IncrementalTrackerStorage {
    fun updatePendingEvents(
        upsertedEvents: List<TrackerEvent>,
        removedEventIds: Set<String>,
    )
}
