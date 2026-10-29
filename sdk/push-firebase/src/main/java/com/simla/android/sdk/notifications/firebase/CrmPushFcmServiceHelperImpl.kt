package com.simla.android.sdk.notifications.firebase

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.simla.android.sdk.notifications.interaction.PushInteractionPendingIntents
import com.simla.android.sdk.notifications.message.PushMessage
import com.simla.android.sdk.notifications.message.PushMessageParser
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import com.simla.android.sdk.notifications.runtime.PushRuntime
import com.simla.android.sdk.notifications.status.PushStatus

internal class CrmPushFcmServiceHelperImpl : CrmPushFcmServiceHelper {

    override fun onMessageReceived(context: Context, remoteMessage: RemoteMessage): Boolean {
        val sdkMessage = PushMessageParser.fromRemoteMessage(remoteMessage)
        if (sdkMessage == null) {
            CrmPushLogger.log("Push message ignored")
            return false
        }

        CrmPushLogger.log {
            "Push message received: pushId=${sdkMessage.pushId}, " +
                    "image=${sdkMessage.imageUrl != null}, buttons=${sdkMessage.buttons.size}, " +
                    "customAttributeKeys=${sdkMessage.customAttributes.keys}"
        }
        PushRuntime.enqueueStatus(context, sdkMessage.pushId, PushStatus.DELIVERED)
        if (sdkMessage.title != null && sdkMessage.body != null) {
            showNotification(context, sdkMessage)
            sdkMessage.imageUrl?.let { imageUrl ->
                PushNotificationImageWorker.enqueue(
                    context = context,
                    pushId = sdkMessage.pushId,
                    imageUrl = imageUrl,
                )
            }
        }
        return true
    }

    override fun onRegistered(installationId: String) {
        CrmPushLogger.log("Firebase installation ID registered")
    }

    override fun onNewToken(newToken: String) {
        CrmPushLogger.log("Firebase registration token updated")
    }

    private fun showNotification(context: Context, notification: PushMessage) {
        val channelSetting = PushRuntime.getChannelSetting()
        val smallIconResId = channelSetting.smallIconResId
            ?: context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info
        val builder = NotificationCompat.Builder(context, channelSetting.channelId)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(
                PushInteractionPendingIntents.open(
                    context = context,
                    pushId = notification.pushId,
                    clickAction = notification.clickAction,
                    customAttributes = notification.customAttributes,
                    utm = notification.utm,
                ),
            )
            .setDeleteIntent(PushInteractionPendingIntents.dismiss(context, notification.pushId))
            .setSmallIcon(smallIconResId)

        notification.buttons.forEachIndexed { index, button ->
            builder.addAction(
                smallIconResId,
                button.title,
                PushInteractionPendingIntents.button(
                    context = context,
                    pushId = notification.pushId,
                    buttonIndex = index,
                    button = button,
                    customAttributes = notification.customAttributes,
                    utm = notification.utm,
                ),
            )
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            PushInteractionPendingIntents.notificationId(notification.pushId),
            builder.build(),
        )
    }
}
