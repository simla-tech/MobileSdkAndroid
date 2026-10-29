package com.simla.android.sdk.notifications.registration

internal fun interface RegistrationTokenProvider {
    suspend fun getToken(): String?
}
