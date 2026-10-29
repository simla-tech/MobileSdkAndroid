package com.simla.android.sdk.notifications.runtime

import com.simla.android.sdk.CrmFeature
import com.simla.android.sdk.CrmFeatureContext
import com.simla.android.sdk.notifications.PushNotifications
import com.simla.android.sdk.notifications.PushNotificationsConfig
import com.simla.android.sdk.notifications.PushNotificationsFeature
import com.simla.android.sdk.notifications.firebase.DefaultFirebaseProjectNameProvider
import com.simla.android.sdk.notifications.firebase.FirebaseInstallationIdProvider
import com.simla.android.sdk.notifications.firebase.FirebaseRegistrationTokenProvider
import com.simla.android.sdk.notifications.registration.DefaultPushNotificationsClient

internal class PushNotificationsModule(
    private val config: PushNotificationsConfig,
) : CrmFeature<PushNotifications> {
    override val key = PushNotificationsFeature.Key

    override fun install(context: CrmFeatureContext): PushNotifications {
        PushRuntime.init(context.applicationContext, config.notificationChannel)
        PushRuntime.configureStatusReporting(
            endpoint = "${context.config.clientApiBaseUrl}/push-tokens/set-status",
            siteToken = context.config.siteToken,
        )
        return DefaultPushNotificationsClient(
            endpoint = "${context.config.clientApiBaseUrl}/push-tokens",
            siteToken = context.config.siteToken,
            networkEngine = context.networkEngine,
            installationIdProvider = FirebaseInstallationIdProvider,
            registrationTokenProvider = FirebaseRegistrationTokenProvider,
            firebaseProjectNameProvider = DefaultFirebaseProjectNameProvider,
            customerIdentityProvider = context.session.currentCustomerIdentity,
        )
    }
}
