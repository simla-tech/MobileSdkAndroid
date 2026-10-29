package com.simla.android.sdk.notifications.registration

internal fun interface InstallationIdProvider {
    suspend fun getId(): String?
}
