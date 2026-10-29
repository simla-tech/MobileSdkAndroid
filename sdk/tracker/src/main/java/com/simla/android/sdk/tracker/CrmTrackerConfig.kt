package com.simla.android.sdk.tracker

internal data class CrmTrackerConfig(
    val siteToken: String,
    val baseUrl: String,
    val debug: Boolean = false,
    override val trackerBatchSize: Int = 50,
    override val trackerFlushIntervalMs: Long = 15_000,
    override val trackerTimeSyncIntervalMs: Long = 60 * 60 * 1000,
    override val trackerMaxQueueSize: Int = 1_000,
) : TrackerRuntimeConfig {
    val clientApiBaseUrl: String
        get() {
            val trimmed = baseUrl.trim().trimEnd('/')
            return if (trimmed.endsWith("/api/client/v1")) trimmed else "$trimmed/api/client/v1"
        }
}
