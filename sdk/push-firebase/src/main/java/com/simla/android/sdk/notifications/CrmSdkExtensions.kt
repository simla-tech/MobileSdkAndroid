package com.simla.android.sdk.notifications

import android.content.Context
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.CrmSdkModulesBuilder
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.notifications.firebase.FirebasePushNotifications
import com.simla.android.sdk.notifications.runtime.PushNotificationsModule
import com.simla.android.sdk.session.SessionPageContext

fun CrmSdk.Companion.init(
    context: Context,
    config: com.simla.android.sdk.CrmSdkConfig,
    networkEngine: SdkNetworkEngine,
    pushNotifications: PushNotificationsConfig,
    pageContext: SessionPageContext = SessionPageContext(),
): Unit = initWithModules(
    context = context,
    config = config,
    pageContext = pageContext,
    modules = listOf(PushNotificationsModule(pushNotifications)),
    networkEngine = networkEngine,
)

/** Installs Firebase push notifications while [CrmSdk.init] configures the SDK. */
fun CrmSdkModulesBuilder.pushNotifications(config: PushNotificationsConfig) {
    register(PushNotificationsModule(config))
}

fun CrmSdk.firebasePushNotifications(): FirebasePushNotifications =
    requireModule(PushNotificationsFeature.Key) as? FirebasePushNotifications
        ?: error("The installed push notifications module is not Firebase-based")
