package com.simla.android.sdk.tracker

internal interface ConsultantTrackerObserver {
    fun onQueueSizeChanged(size: Int)
    fun onEventUpdate(update: TrackerEventUpdate)
    fun onFlushSucceeded(atEpochMs: Long)
    fun onError(message: String?)
    fun onRateLimited(untilEpochMs: Long?)
}
