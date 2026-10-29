package com.simla.android.sdk.notifications

interface PushNotifications {
    suspend fun registerDevice(
        categories: Set<String>? = null,
    ): PushRegistrationResult

}
