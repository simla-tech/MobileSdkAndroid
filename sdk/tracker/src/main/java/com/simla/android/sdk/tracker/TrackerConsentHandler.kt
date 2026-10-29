package com.simla.android.sdk.tracker

/** Provides the application's tracking consent decision. */
fun interface TrackerConsentHandler {
    /** Returns the saved decision or requests it from the user when necessary. */
    suspend fun getConsent(): TrackerConsentState
}
