package com.simla.android.sdk.tracker.storage

interface TrackerStorage {
    fun activateScope(siteToken: String, userToken: String): Boolean

    fun getPendingEvents(): String?

    fun setPendingEvents(value: String?)

    fun getTimeSyncState(): String?

    fun setTimeSyncState(value: String?)

    fun clear()
}
