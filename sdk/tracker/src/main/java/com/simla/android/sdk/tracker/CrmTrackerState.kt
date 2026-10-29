package com.simla.android.sdk.tracker

internal data class CrmTrackerState(
    val connectionStatus: TrackerConnectionStatus = TrackerConnectionStatus.DISCONNECTED,
    val serverAllowsTracking: Boolean = false,
    val consentRequired: Boolean = false,
    val consentState: TrackerConsentState = TrackerConsentState.UNKNOWN,
    val pendingEventCount: Int = 0,
    val lastSuccessfulFlushAtEpochMs: Long? = null,
    val lastError: String? = null,
    val rateLimitedUntilEpochMs: Long? = null,
)
