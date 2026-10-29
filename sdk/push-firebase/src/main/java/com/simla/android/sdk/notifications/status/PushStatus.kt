package com.simla.android.sdk.notifications.status

internal enum class PushStatus(val apiValue: String) {
    DELIVERED("delivered"),
    CLICKED("clicked"),
    SWIPED("swiped"),
}
