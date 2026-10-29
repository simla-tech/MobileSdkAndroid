package com.simla.android.sdk.tracker

interface TrackerRuntimeConfig {
    val trackerBatchSize: Int
    val trackerFlushIntervalMs: Long
    val trackerTimeSyncIntervalMs: Long
    val trackerMaxQueueSize: Int
}
