package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.message.ConsultantMessage
import com.simla.android.sdk.consultant.realtime.realtimeMarkerComparator

internal fun List<ConsultantMessage>.latestRealtimeMarker(): String? {
    return mapNotNull { it.realtimeMarker?.takeIf(String::isNotBlank) }
        .maxWithOrNull(realtimeMarkerComparator)
}
