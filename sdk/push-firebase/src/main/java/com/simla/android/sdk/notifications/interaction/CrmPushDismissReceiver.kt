package com.simla.android.sdk.notifications.interaction

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simla.android.sdk.notifications.status.PushStatus

class CrmPushDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pushId = PushInteractionPendingIntents.pushId(intent) ?: return
        PushInteractionPendingIntents.reportStatus(context, intent, pushId, PushStatus.SWIPED)
    }
}
