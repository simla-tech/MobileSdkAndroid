package com.simla.android.sdk.consultant.upload

internal fun Long.formatFileSizeLabel(): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble().coerceAtLeast(0.0)
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }

    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "%.1f %s".format(java.util.Locale.US, value, units[unitIndex])
    }
}
