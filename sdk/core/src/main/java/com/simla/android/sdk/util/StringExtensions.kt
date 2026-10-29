package com.simla.android.sdk.util

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ISO_8601_TIMESTAMP_REGEX =
    Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:\d{2})$""")

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String?.normalizedContactFormValue(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() }
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.fileNameFromUrl(): String? {
    return substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.toRealtimeMarkerEpochMillis(): Long? {
    return toIso8601EpochMillis()
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.toIso8601EpochMillis(): Long? {
    val match = ISO_8601_TIMESTAMP_REGEX.matchEntire(this) ?: return null
    val seconds = match.groupValues[1]
    val milliseconds = match.groupValues[2].padEnd(3, '0').take(3)
    val offset = match.groupValues[3]
    val normalizedTimestamp = "$seconds.$milliseconds$offset"
    val parsePosition = ParsePosition(0)
    val date = iso8601Formatter().parse(normalizedTimestamp, parsePosition)
    return date
        ?.takeIf { parsePosition.index == normalizedTimestamp.length }
        ?.time
}

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Long.toIso8601Timestamp(): String {
    return iso8601Formatter().apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(this))
}

private fun iso8601Formatter(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        isLenient = false
    }
}
