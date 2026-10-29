package com.simla.android.sdk.notifications.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.simla.android.sdk.notifications.runtime.PushRuntime

class CrmPushFcmService : FirebaseMessagingService() {

    /**
     * Поведение FCM при доставке сообщений в зависимости от состояния приложения и типа сообщения:
     *
     * -------------------------------------------------------------------------------------------
     * App state      | Notification        | Data                | Both
     * -------------------------------------------------------------------------------------------
     * Foreground     | onMessageReceived   | onMessageReceived   | onMessageReceived
     * Background     | System tray         | onMessageReceived   | Notification: system tray
     *                |                     |                     | Data: in extras of the intent
     * -------------------------------------------------------------------------------------------
     *
     * Источник: Firebase Cloud Messaging — Receive messages in Android
     * https://firebase.google.com/docs/cloud-messaging/android/receive
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        PushRuntime.getFcmServiceHelper().onMessageReceived(this, remoteMessage)
    }

    override fun onRegistered(installationId: String) {
        PushRuntime.getFcmServiceHelper().onRegistered(installationId)
    }

    override fun onNewToken(newToken: String) {
        PushRuntime.getFcmServiceHelper().onNewToken(newToken)
    }
}
