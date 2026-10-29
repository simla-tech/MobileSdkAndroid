package com.simla.android.sdk.tracker

import com.simla.android.sdk.tracker.storage.TrackerStorage

internal class ScopedMemoryTrackerStorage : TrackerStorage {
    private data class State(var events: String? = null, var time: String? = null)

    private val states = mutableMapOf<String, State>()
    private var activeScope: String? = null

    override fun activateScope(siteToken: String, userToken: String): Boolean {
        val scope = "$siteToken:$userToken"
        if (activeScope == scope) return false
        activeScope = scope
        return true
    }

    override fun getPendingEvents() = activeState().events
    override fun setPendingEvents(value: String?) {
        activeState().events = value
    }

    override fun getTimeSyncState() = activeState().time
    override fun setTimeSyncState(value: String?) {
        activeState().time = value
    }

    override fun clear() {
        activeScope?.let(states::remove)
    }

    private fun activeState(): State = states.getOrPut(checkNotNull(activeScope)) { State() }
}
