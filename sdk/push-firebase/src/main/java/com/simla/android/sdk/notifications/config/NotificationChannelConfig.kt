package com.simla.android.sdk.notifications.config

import android.app.Notification
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.annotation.ColorInt

data class NotificationChannelConfig(
    val channelName: String,
    val channelDescription: String? = null,
    val channelId: String = DEFAULT_CHANNEL_ID,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,

    // Значок на иконке приложения
    val showBadge: Boolean = true,

    // Группа каналов (id группы, если приложение их использует)
    val groupId: String? = null,

    // Звук
    // null = не трогаем (оставляем системный / существующий),
    // Uri.EMPTY = явно сделать канал без звука
    val sound: Uri? = null,
    val soundAudioAttributes: AudioAttributes? = null,

    // Вибрация
    val enableVibration: Boolean = false,
    val vibrationPattern: LongArray? = null,

    // Индикатор уведомлений (LED)
    val enableLights: Boolean = true,
    @field:ColorInt
    val lightColor: Int? = null,

    // Видимость на экране блокировки
    val lockscreenVisibility: Int = Notification.VISIBILITY_PRIVATE,

    // Разрешить "пузырьки" (bubbles), API 29+
    val allowBubbles: Boolean? = null
) {
    companion object {
        private const val DEFAULT_CHANNEL_ID = "crm_push_sdk_default_channel"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationChannelConfig

        if (importance != other.importance) return false
        if (showBadge != other.showBadge) return false
        if (enableVibration != other.enableVibration) return false
        if (enableLights != other.enableLights) return false
        if (lightColor != other.lightColor) return false
        if (lockscreenVisibility != other.lockscreenVisibility) return false
        if (allowBubbles != other.allowBubbles) return false
        if (channelName != other.channelName) return false
        if (channelDescription != other.channelDescription) return false
        if (channelId != other.channelId) return false
        if (groupId != other.groupId) return false
        if (sound != other.sound) return false
        if (soundAudioAttributes != other.soundAudioAttributes) return false
        if (!vibrationPattern.contentEquals(other.vibrationPattern)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = importance
        result = 31 * result + showBadge.hashCode()
        result = 31 * result + enableVibration.hashCode()
        result = 31 * result + enableLights.hashCode()
        result = 31 * result + (lightColor ?: 0)
        result = 31 * result + lockscreenVisibility
        result = 31 * result + (allowBubbles?.hashCode() ?: 0)
        result = 31 * result + channelName.hashCode()
        result = 31 * result + (channelDescription?.hashCode() ?: 0)
        result = 31 * result + channelId.hashCode()
        result = 31 * result + (groupId?.hashCode() ?: 0)
        result = 31 * result + (sound?.hashCode() ?: 0)
        result = 31 * result + (soundAudioAttributes?.hashCode() ?: 0)
        result = 31 * result + (vibrationPattern?.contentHashCode() ?: 0)
        return result
    }
}
