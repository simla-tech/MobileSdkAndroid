package com.simla.android.sdk.notifications.firebase

import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.simla.android.sdk.notifications.registration.InstallationIdProvider
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import kotlinx.coroutines.CancellationException

internal object FirebaseInstallationIdProvider : InstallationIdProvider {
    override suspend fun getId(): String? {
        return try {
            FirebaseMessaging.getInstance().register().awaitResult()
            FirebaseInstallations.getInstance().id.awaitResult()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            CrmPushLogger.log {
                "Firebase installation ID is unavailable: ${error.message}"
            }
            null
        }
    }
}
