package com.simla.android.sdk.notifications.runtime

import android.util.Log
import com.simla.android.sdk.notifications.PushDiagnosticEvent
import com.simla.android.sdk.notifications.PushDiagnosticLevel
import com.simla.android.sdk.notifications.PushDiagnosticListener

internal object CrmPushLogger {

    @Volatile
    private var diagnosticListener: PushDiagnosticListener? = null

    fun setDiagnosticListener(listener: PushDiagnosticListener?) {
        diagnosticListener = listener
    }

    fun log(message: String) {
        Log.i(PushRuntime.LOG_TAG, message)
        notifyListener(PushDiagnosticEvent(PushDiagnosticLevel.INFO, message))
    }

    fun log(messageProducer: () -> String) {
        log(messageProducer())
    }

    fun logError(message: String, error: Throwable) {
        Log.e(PushRuntime.LOG_TAG, message, error)
        notifyListener(PushDiagnosticEvent(PushDiagnosticLevel.ERROR, message, error))
    }

    private fun notifyListener(event: PushDiagnosticEvent) {
        runCatching { diagnosticListener?.onDiagnosticEvent(event) }
            .onFailure { error ->
                Log.e(PushRuntime.LOG_TAG, "Push diagnostic listener failed", error)
            }
    }

}
