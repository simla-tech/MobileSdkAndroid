package com.simla.android.sdk.notifications.firebase

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.simla.android.sdk.notifications.PushNotifications

interface FirebasePushNotifications : PushNotifications {
    fun handleMessage(context: Context, remoteMessage: RemoteMessage): Boolean

    fun handleRegistered(installationId: String)

    fun handleNewToken(token: String)
}
