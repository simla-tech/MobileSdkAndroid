package com.simla.android.sdk.tracker

/** Receives queue, sending, delivery, retry, rejection and drop updates from the tracker. */
fun interface TrackerEventUpdateListener {
    fun onEventUpdate(update: TrackerEventUpdate)
}
