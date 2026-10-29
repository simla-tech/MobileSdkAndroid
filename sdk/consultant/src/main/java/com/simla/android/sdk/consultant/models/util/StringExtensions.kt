package com.simla.android.sdk.consultant.models.util

internal fun String.hasScheme(): Boolean {
    return contains("://") || startsWith("//")
}
