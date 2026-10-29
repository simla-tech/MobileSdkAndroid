package com.simla.android.sdk.notifications.registration

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.error.SdkHttpException
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.notifications.PushDiagnosticListener
import com.simla.android.sdk.notifications.PushRegistrationResult
import com.simla.android.sdk.notifications.firebase.FirebaseProjectNameProvider
import com.simla.android.sdk.notifications.firebase.FirebasePushNotifications
import com.simla.android.sdk.notifications.runtime.PushRuntime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DefaultPushNotificationsClient(
    private val endpoint: String,
    private val siteToken: String,
    private val networkEngine: SdkHttpNetworkEngine,
    private val installationIdProvider: InstallationIdProvider,
    private val registrationTokenProvider: RegistrationTokenProvider,
    private val firebaseProjectNameProvider: FirebaseProjectNameProvider,
    private val customerIdentityProvider: () -> CrmCustomerIdentity?,
) : FirebasePushNotifications {
    override fun handleMessage(context: Context, remoteMessage: RemoteMessage): Boolean =
        PushRuntime.getFcmServiceHelper().onMessageReceived(context, remoteMessage)

    override fun handleRegistered(installationId: String) {
        PushRuntime.getFcmServiceHelper().onRegistered(installationId)
    }

    override fun handleNewToken(token: String) {
        PushRuntime.getFcmServiceHelper().onNewToken(token)
    }

    internal fun setDiagnosticListener(listener: PushDiagnosticListener?) {
        PushRuntime.setDiagnosticListener(listener)
    }

    override suspend fun registerDevice(
        categories: Set<String>?,
    ): PushRegistrationResult {
        val customerIdentity = requireNotNull(customerIdentityProvider()) {
            "Set customer identity with CrmSdk.setCustomerIdentity() before registering push"
        }
        val normalizedSiteCustomerId = customerIdentity.siteCustomerId?.trim()
        val normalizedSiteCode = customerIdentity.siteCode?.trim()
        val fid = installationIdProvider.getId()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        require(fid == null || fid.length <= 255) {
            "Firebase installation ID must not exceed 255 characters"
        }

        val registrationToken = if (fid == null) {
            registrationTokenProvider.getToken()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        } else {
            null
        }
        require(registrationToken == null || registrationToken.length <= 255) {
            "FCM registration token must not exceed 255 characters"
        }
        require(fid != null || registrationToken != null) {
            "Either a registered Firebase installation ID or FCM registration token is required"
        }

        val firebaseProjectName = firebaseProjectNameProvider.getName().trim()
        require(firebaseProjectName.isNotEmpty()) { "Firebase project name must not be blank" }
        require(firebaseProjectName.length <= 255) {
            "Firebase project name must not exceed 255 characters"
        }

        val requestedCategories = categories.orEmpty()
        require(requestedCategories.all { it.isNotBlank() && it.trim().length <= 255 }) {
            "Push categories must be non-blank and must not exceed 255 characters"
        }
        val requestBody = PushTokenRequest(
            customerId = customerIdentity.crmCustomerId,
            customerExternalId = normalizedSiteCustomerId,
            crmSiteCode = normalizedSiteCode,
            fid = fid,
            token = registrationToken,
            type = firebaseProjectName,
            deviceType = "android",
            categories = requestedCategories.map(String::trim).distinct().sorted(),
        )
        val response = networkEngine.postJson(
            SdkHttpRequest(
                url = endpoint,
                headers = mapOf("X-Site-Token" to siteToken),
                body = PushRegistrationJson.encodeToString(requestBody),
            ),
        )
        if (response.code != 204) {
            throw SdkHttpException(
                message = "Push token registration failed: HTTP ${response.code}",
                httpCode = response.code,
                responseBody = response.body,
            )
        }
        return PushRegistrationResult(
            fid = fid,
            token = registrationToken,
        )
    }
}

private val PushRegistrationJson = Json { explicitNulls = false }
