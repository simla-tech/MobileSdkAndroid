package com.simla.android.sdk.notifications

import com.simla.android.sdk.CrmFeatureKey
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.notifications.registration.DefaultPushNotificationsClient

fun CrmSdk.pushNotifications(): PushNotifications =
    requireModule(PushNotificationsFeature.Key)

fun PushNotifications.setDiagnosticListener(listener: PushDiagnosticListener?) {
    require(this is DefaultPushNotificationsClient) {
        "The installed push notifications module does not support diagnostics"
    }
    setDiagnosticListener(listener)
}

internal object PushNotificationsFeature {
    val Key = CrmFeatureKey<PushNotifications>("push-notifications")
}
