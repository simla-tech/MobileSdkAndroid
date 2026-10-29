package com.simla.android.sdk.notifications.message

import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.json.Json

internal object PushMessageParser {
    private const val SDK_NOTIFICATION_FINGERPRINT = "r_group_push"
    private const val CATEGORY = "r_group_category"
    private const val PUSH_ID = "push_id"
    private const val IMAGE_URL = "image_url"
    private const val CLICK_ACTION = "click_action"
    private const val BUTTONS = "buttons"
    private const val CUSTOM_ATTRIBUTE_PREFIX = "ca_"
    private const val UTM_SOURCE = "utm_source"
    private const val UTM_MEDIUM = "utm_medium"
    private const val UTM_CAMPAIGN = "utm_campaign"
    private const val UTM_TERM = "utm_term"
    private const val UTM_CONTENT = "utm_content"
    private val pushIdPattern = Regex("^[^_\\s]+_[^\\s]+$")
    private val buttonsJson = Json { ignoreUnknownKeys = true }

    fun fromRemoteMessage(remoteMessage: RemoteMessage): PushMessage? = parse(
        data = remoteMessage.data,
        notificationTitle = remoteMessage.notification?.title,
        notificationBody = remoteMessage.notification?.body,
        notificationImageUrl = remoteMessage.notification?.imageUrl?.toString(),
    )

    fun parse(
        data: Map<String, String>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        notificationImageUrl: String? = null,
    ): PushMessage? {
        if (!isValidFingerprint(data[SDK_NOTIFICATION_FINGERPRINT])) return null
        val pushId = data[PUSH_ID]?.trim()?.takeIf(pushIdPattern::matches) ?: return null
        return PushMessage(
            pushId = pushId,
            category = data[CATEGORY]?.trim()?.takeIf(String::isNotEmpty),
            title = notificationTitle ?: data.getNotificationParamCompat("title"),
            body = notificationBody ?: data.getNotificationParamCompat("body"),
            imageUrl = notificationImageUrl ?: data[IMAGE_URL]?.trim()?.takeIf(String::isNotEmpty),
            clickAction = data[CLICK_ACTION]?.trim()?.takeIf(String::isNotEmpty),
            customAttributes = data.customAttributes(),
            buttons = data[BUTTONS].parseButtons(),
            utm = data.utm(),
            data = data,
        )
    }

    private fun isValidFingerprint(fingerprint: String?): Boolean =
        fingerprint.toBoolean() || fingerprint == "1"

    private fun Map<String, String>.getNotificationParamCompat(key: String): String? =
        this["gcm.n.$key"]
            ?: this["gcm.notification.$key"]
            ?: this[key]

    private fun Map<String, String>.customAttributes(): Map<String, String> =
        entries.mapNotNull { (key, value) ->
            if (!key.startsWith(CUSTOM_ATTRIBUTE_PREFIX)) return@mapNotNull null
            val attributeKey = key.removePrefix(CUSTOM_ATTRIBUTE_PREFIX)
                .takeIf(String::isNotEmpty) ?: return@mapNotNull null
            attributeKey to value
        }.toMap()

    private fun Map<String, String>.utm(): PushUtm? {
        val utm = PushUtm(
            source = nonBlank(UTM_SOURCE),
            medium = nonBlank(UTM_MEDIUM),
            campaign = nonBlank(UTM_CAMPAIGN),
            term = nonBlank(UTM_TERM),
            content = nonBlank(UTM_CONTENT),
        )
        return utm.takeUnless {
            it.source == null && it.medium == null && it.campaign == null &&
                    it.term == null && it.content == null
        }
    }

    private fun Map<String, String>.nonBlank(key: String): String? =
        this[key]?.trim()?.takeIf(String::isNotEmpty)

    private fun String?.parseButtons(): List<PushButton> {
        if (this.isNullOrBlank()) return emptyList()
        return runCatching { buttonsJson.decodeFromString<List<PushButton>>(this) }
            .getOrDefault(emptyList())
            .filter { it.title.isNotBlank() && it.value.isNotBlank() }
            .take(MAX_BUTTONS)
    }

    private const val MAX_BUTTONS = 2
}
