package com.simla.android.sdk.consultant.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun localMessageDate(value: String?): Calendar? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        parseIsoUtcDate(value)?.let { parsedDate ->
            Calendar.getInstance().apply {
                time = parsedDate
            }
        }
    }.getOrNull()
}

internal fun formatMessageDateLabel(
    date: Calendar,
    todayKey: String,
    yesterdayKey: String,
    todayLabel: String,
    yesterdayLabel: String,
): String {
    return when (calendarDayKey(date)) {
        todayKey -> todayLabel
        yesterdayKey -> yesterdayLabel
        else -> crmDateFormatter().format(date.time)
    }
}

internal fun calendarDayKey(calendar: Calendar): String {
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
}

internal fun parseIsoUtcDate(value: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
    )

    patterns.forEach { pattern ->
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        runCatching { formatter.parse(value) }.getOrNull()?.let { return it }
    }

    return null
}

internal fun crmDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat(
        "d MMMM yyyy",
        Locale.getDefault(),
    )
}
