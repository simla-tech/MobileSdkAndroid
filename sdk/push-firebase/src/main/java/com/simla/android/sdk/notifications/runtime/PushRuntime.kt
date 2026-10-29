package com.simla.android.sdk.notifications.runtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import com.simla.android.sdk.notifications.PushDiagnosticListener
import com.simla.android.sdk.notifications.config.NotificationChannelConfig
import com.simla.android.sdk.notifications.config.NotificationChannelSetting
import com.simla.android.sdk.notifications.firebase.CrmPushFcmServiceHelper
import com.simla.android.sdk.notifications.firebase.CrmPushFcmServiceHelperImpl
import com.simla.android.sdk.notifications.status.PushStatus
import com.simla.android.sdk.notifications.status.PushStatusConfiguration
import com.simla.android.sdk.notifications.status.PushStatusScheduler
import com.simla.android.sdk.notifications.status.WorkManagerPushStatusScheduler

internal object PushRuntime {

    const val LOG_TAG = "CrmSdk"

    private lateinit var fcmServiceHelper: CrmPushFcmServiceHelper

    private lateinit var channelSetting: NotificationChannelSetting

    private var pushStatusScheduler: PushStatusScheduler? = null

    private var pushStatusConfiguration: PushStatusConfiguration? = null

    internal fun init(context: Context, channelSetting: NotificationChannelSetting) {
        fcmServiceHelper = CrmPushFcmServiceHelperImpl()
        this.channelSetting = channelSetting
        if (channelSetting is NotificationChannelSetting.CreateNew) {
            createNotificationChannelIfNeeded(
                context, channelSetting.config
            )
        }
    }

    internal fun configureStatusReporting(endpoint: String, siteToken: String) {
        pushStatusConfiguration = PushStatusConfiguration(
            endpoint = endpoint,
            siteToken = siteToken,
        )
        pushStatusScheduler = WorkManagerPushStatusScheduler(
            endpoint = endpoint,
            siteToken = siteToken,
        )
        CrmPushLogger.log("Push status reporting configured: endpoint=$endpoint")
    }

    internal fun getStatusConfiguration(): PushStatusConfiguration? = pushStatusConfiguration

    internal fun setDiagnosticListener(listener: PushDiagnosticListener?) {
        CrmPushLogger.setDiagnosticListener(listener)
    }

    internal fun enqueueStatus(
        context: Context,
        pushId: String,
        status: PushStatus,
        changedAtSeconds: Long = System.currentTimeMillis() / 1_000,
        buttonHash: String? = null,
    ) {
        val scheduler = pushStatusScheduler
        if (scheduler == null) {
            CrmPushLogger.log(
                "Push status enqueue skipped: reporting is not configured, " +
                        "pushId=$pushId, status=${status.apiValue}",
            )
            return
        }
        CrmPushLogger.log {
            "Push status enqueue requested: pushId=$pushId, status=${status.apiValue}, " +
                    "changedAt=$changedAtSeconds"
        }
        scheduler.enqueue(context, pushId, status, changedAtSeconds, buttonHash)
    }

    internal fun getFcmServiceHelper(): CrmPushFcmServiceHelper {
        if (::fcmServiceHelper.isInitialized.not()) {
            throw IllegalStateException("PushRuntime is not initialized. Call PushRuntime.init(context) first.")
        }
        return fcmServiceHelper
    }

    internal fun getChannelSetting(): NotificationChannelSetting {
        if (::channelSetting.isInitialized.not()) {
            throw IllegalStateException("PushRuntime is not initialized. Call PushRuntime.init(context) first.")
        }
        return channelSetting
    }

    private fun createNotificationChannelIfNeeded(
        context: Context,
        cfg: NotificationChannelConfig
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            cfg.channelId, cfg.channelName, cfg.importance
        ).apply {
            description = cfg.channelDescription
            setShowBadge(cfg.showBadge)
            cfg.groupId?.let { group = it }
            cfg.sound?.let {
                setSound(
                    it,
                    cfg.soundAudioAttributes ?: AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            enableVibration(cfg.enableVibration)
            cfg.vibrationPattern?.let { vibrationPattern = it }
            enableLights(cfg.enableLights)
            cfg.lightColor?.let { lightColor = it }
            lockscreenVisibility = cfg.lockscreenVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cfg.allowBubbles?.let {
                    setAllowBubbles(it)
                }
            }
        }

        nm.createNotificationChannel(channel)
    }
}
