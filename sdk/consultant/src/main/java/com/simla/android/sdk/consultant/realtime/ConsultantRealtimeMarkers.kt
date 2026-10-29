package com.simla.android.sdk.consultant.realtime

import com.simla.android.sdk.util.toRealtimeMarkerEpochMillis

internal fun latestRealtimeMarker(
    first: String?,
    second: String?,
): String? {
    return listOfNotNull(
        first?.takeIf(String::isNotBlank),
        second?.takeIf(String::isNotBlank),
    ).maxWithOrNull(realtimeMarkerComparator)
}

internal val realtimeMarkerComparator = Comparator<String> { left, right ->
    val leftEpochMillis = left.toRealtimeMarkerEpochMillis()
    val rightEpochMillis = right.toRealtimeMarkerEpochMillis()
    when {
        leftEpochMillis != null && rightEpochMillis != null ->
            leftEpochMillis.compareTo(rightEpochMillis)

        leftEpochMillis != null -> 1
        rightEpochMillis != null -> -1
        else -> left.compareTo(right)
    }
}
