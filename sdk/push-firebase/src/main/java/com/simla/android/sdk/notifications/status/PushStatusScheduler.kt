package com.simla.android.sdk.notifications.status

import android.content.Context

internal interface PushStatusScheduler {
    fun enqueue(
        context: Context,
        pushId: String,
        status: PushStatus,
        changedAtSeconds: Long,
        buttonHash: String? = null,
    )
}
