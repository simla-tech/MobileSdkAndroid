package com.simla.android.sdk.tracker

import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.storage.IncrementalTrackerStorage
import com.simla.android.sdk.tracker.storage.TrackerStorage

internal class RecordingIncrementalTrackerStorage :
    TrackerStorage,
    IncrementalTrackerStorage {
    val upsertedEvents = mutableListOf<List<TrackerEvent>>()
    val upsertedEventIds: List<List<String>>
        get() = upsertedEvents.map { events -> events.mapNotNull(TrackerEvent::id) }
    var fullSnapshotWrites = 0

    override fun activateScope(siteToken: String, userToken: String) = true
    override fun getPendingEvents(): String? = null
    override fun setPendingEvents(value: String?) {
        fullSnapshotWrites += 1
    }

    override fun updatePendingEvents(
        upsertedEvents: List<TrackerEvent>,
        removedEventIds: Set<String>,
    ) {
        this.upsertedEvents += upsertedEvents
    }

    override fun getTimeSyncState(): String? = null
    override fun setTimeSyncState(value: String?) = Unit
    override fun clear() = Unit
}
