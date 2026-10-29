package com.simla.android.sdk.notifications.firebase

import android.content.Context
import com.google.firebase.messaging.RemoteMessage

internal interface CrmPushFcmServiceHelper {
    /**
     * @return true если сообщение обработано и дальнейшая обработка не требуется
     */
    fun onMessageReceived(context: Context, remoteMessage: RemoteMessage): Boolean
    fun onRegistered(installationId: String)
    fun onNewToken(newToken: String)
}
