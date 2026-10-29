package com.simla.android.sdk.notifications.config

import androidx.annotation.DrawableRes

sealed class NotificationChannelSetting {
    abstract val channelId: String

    @get:DrawableRes
    abstract val smallIconResId: Int?

    data class UseExisting(
        override val channelId: String,
        override val smallIconResId: Int? = null
    ) : NotificationChannelSetting()

    data class CreateNew(
        val config: NotificationChannelConfig,
        override val smallIconResId: Int? = null
    ) : NotificationChannelSetting() {
        override val channelId: String
            get() = config.channelId
    }

}
