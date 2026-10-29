package com.simla.android.sdk.notifications.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.simla.android.sdk.notifications.registration.RegistrationTokenProvider
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import kotlinx.coroutines.CancellationException

internal object FirebaseRegistrationTokenProvider : RegistrationTokenProvider {
    override suspend fun getToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.awaitResult()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            CrmPushLogger.log {
                "FCM registration token is unavailable: ${error.message}"
            }
            null
        }
    }
}
