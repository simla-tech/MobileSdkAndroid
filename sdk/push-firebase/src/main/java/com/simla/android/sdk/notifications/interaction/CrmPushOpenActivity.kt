package com.simla.android.sdk.notifications.interaction

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import com.simla.android.sdk.notifications.status.PushStatus

class CrmPushOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pushId = PushInteractionPendingIntents.pushId(intent)
        if (pushId != null) {
            PushInteractionPendingIntents.saveAttribution(intent)
            PushInteractionPendingIntents.reportStatus(this, intent, pushId, PushStatus.CLICKED)
            dismissNotification(pushId)
            openDestination(
                pushId = pushId,
                clickAction = PushInteractionPendingIntents.clickAction(intent),
                targetUri = PushInteractionPendingIntents.targetUri(intent),
                customAttributes = PushInteractionPendingIntents.customAttributes(intent),
            )
        }
        finish()
    }

    private fun dismissNotification(pushId: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PushInteractionPendingIntents.notificationId(pushId))
    }

    private fun openDestination(
        pushId: String,
        clickAction: String?,
        targetUri: String?,
        customAttributes: Map<String, String>,
    ) {
        val destinationUri = targetUri ?: clickAction?.takeIf(::isAbsoluteUri)
        val destinationIntent = if (destinationUri != null) {
            createDeepLinkIntent(destinationUri)
        } else {
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                clickAction?.let { action = it }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            } ?: return
        }

        customAttributes.forEach { (key, value) -> destinationIntent.putExtra(key, value) }

        CrmPushLogger.log {
            "Push destination opening: pushId=$pushId, action=${destinationIntent.action}, " +
                    "uri=${destinationIntent.data}, customAttributeKeys=${customAttributes.keys}"
        }
        runCatching { startActivity(destinationIntent) }
            .onFailure { error ->
                CrmPushLogger.logError(
                    "Push destination opening failed: pushId=$pushId",
                    error,
                )
            }
    }

    private fun isAbsoluteUri(value: String): Boolean =
        Uri.parse(value).let { it.isAbsolute && !it.host.isNullOrBlank() }

    private fun createDeepLinkIntent(value: String): Intent {
        val implicitIntent = Intent(Intent.ACTION_VIEW, Uri.parse(value))
        val applicationIntent = Intent(implicitIntent).setPackage(packageName)
        return applicationIntent.takeIf { it.resolveActivity(packageManager) != null }
            ?: implicitIntent
    }
}
