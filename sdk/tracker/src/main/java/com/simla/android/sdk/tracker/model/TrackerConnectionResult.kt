package com.simla.android.sdk.tracker.model

sealed interface TrackerConnectionResult {
    val response: TrackerConnectResponse

    data class Available(override val response: TrackerConnectResponse) : TrackerConnectionResult

    data class DisabledByServer(override val response: TrackerConnectResponse) :
        TrackerConnectionResult
}
