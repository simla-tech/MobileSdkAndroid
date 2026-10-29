package com.simla.android.sdk.notifications.interaction

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.simla.android.sdk.CrmUtmAttribution
import com.simla.android.sdk.CrmUtmAttributionStorage
import com.simla.android.sdk.notifications.message.PushButton
import com.simla.android.sdk.notifications.message.PushUtm
import com.simla.android.sdk.notifications.runtime.CrmPushLogger
import com.simla.android.sdk.notifications.runtime.PushRuntime
import com.simla.android.sdk.notifications.status.PushStatus
import com.simla.android.sdk.notifications.status.WorkManagerPushStatusScheduler
import java.security.MessageDigest

internal object PushInteractionPendingIntents {
    private const val EXTRA_PUSH_ID = "com.simla.android.sdk.notifications.extra.PUSH_ID"
    private const val EXTRA_STATUS_ENDPOINT =
        "com.simla.android.sdk.notifications.extra.STATUS_ENDPOINT"
    private const val EXTRA_SITE_TOKEN = "com.simla.android.sdk.notifications.extra.SITE_TOKEN"
    private const val EXTRA_CLICK_ACTION = "com.simla.android.sdk.notifications.extra.CLICK_ACTION"
    private const val EXTRA_TARGET_URI = "com.simla.android.sdk.notifications.extra.TARGET_URI"
    private const val EXTRA_BUTTON_HASH = "com.simla.android.sdk.notifications.extra.BUTTON_HASH"
    private const val EXTRA_CUSTOM_ATTRIBUTE_KEYS =
        "com.simla.android.sdk.notifications.extra.CUSTOM_ATTRIBUTE_KEYS"
    private const val EXTRA_UTM_SOURCE = "com.simla.android.sdk.notifications.extra.UTM_SOURCE"
    private const val EXTRA_UTM_MEDIUM = "com.simla.android.sdk.notifications.extra.UTM_MEDIUM"
    private const val EXTRA_UTM_CAMPAIGN = "com.simla.android.sdk.notifications.extra.UTM_CAMPAIGN"
    private const val EXTRA_UTM_TERM = "com.simla.android.sdk.notifications.extra.UTM_TERM"
    private const val EXTRA_UTM_CONTENT = "com.simla.android.sdk.notifications.extra.UTM_CONTENT"
    private val reservedExtraKeys = setOf(
        EXTRA_PUSH_ID,
        EXTRA_STATUS_ENDPOINT,
        EXTRA_SITE_TOKEN,
        EXTRA_CLICK_ACTION,
        EXTRA_TARGET_URI,
        EXTRA_BUTTON_HASH,
        EXTRA_CUSTOM_ATTRIBUTE_KEYS,
        EXTRA_UTM_SOURCE,
        EXTRA_UTM_MEDIUM,
        EXTRA_UTM_CAMPAIGN,
        EXTRA_UTM_TERM,
        EXTRA_UTM_CONTENT,
    )

    fun open(
        context: Context,
        pushId: String,
        clickAction: String?,
        customAttributes: Map<String, String>,
        utm: PushUtm?,
    ): PendingIntent {
        val intent = addAttribution(
            addCustomAttributes(
                addStatusConfiguration(
                    Intent(context, CrmPushOpenActivity::class.java)
                        .setData(interactionUri("open", pushId))
                        .putExtra(EXTRA_PUSH_ID, pushId)
                        .putExtra(EXTRA_CLICK_ACTION, clickAction),
                ),
                customAttributes,
            ),
            utm,
        )
        return PendingIntent.getActivity(
            context,
            pushId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun button(
        context: Context,
        pushId: String,
        buttonIndex: Int,
        button: PushButton,
        customAttributes: Map<String, String>,
        utm: PushUtm?,
    ): PendingIntent {
        val intent = addAttribution(
            addCustomAttributes(
                addStatusConfiguration(
                    Intent(context, CrmPushOpenActivity::class.java)
                        .setData(interactionUri("button-$buttonIndex", pushId))
                        .putExtra(EXTRA_PUSH_ID, pushId)
                        .putExtra(EXTRA_TARGET_URI, button.value)
                        .putExtra(EXTRA_BUTTON_HASH, buttonHash(button.value)),
                ),
                customAttributes,
            ),
            utm,
        )
        return PendingIntent.getActivity(
            context,
            31 * pushId.hashCode() + buttonIndex + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun dismiss(context: Context, pushId: String): PendingIntent {
        val intent = addStatusConfiguration(
            Intent(context, CrmPushDismissReceiver::class.java)
                .setData(interactionUri("dismiss", pushId))
                .putExtra(EXTRA_PUSH_ID, pushId),
        )
        return PendingIntent.getBroadcast(
            context,
            pushId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun pushId(intent: Intent): String? = intent.getStringExtra(EXTRA_PUSH_ID)

    fun clickAction(intent: Intent): String? = intent.getStringExtra(EXTRA_CLICK_ACTION)

    fun targetUri(intent: Intent): String? = intent.getStringExtra(EXTRA_TARGET_URI)

    fun buttonHash(intent: Intent): String? = intent.getStringExtra(EXTRA_BUTTON_HASH)

    fun customAttributes(intent: Intent): Map<String, String> =
        intent.getStringArrayListExtra(EXTRA_CUSTOM_ATTRIBUTE_KEYS)
            .orEmpty()
            .mapNotNull { key -> intent.getStringExtra(key)?.let { key to it } }
            .toMap()

    fun reportStatus(context: Context, intent: Intent, pushId: String, status: PushStatus) {
        val endpoint = intent.getStringExtra(EXTRA_STATUS_ENDPOINT)
        val siteToken = intent.getStringExtra(EXTRA_SITE_TOKEN)
        val buttonHash = buttonHash(intent)
        if (endpoint != null && siteToken != null) {
            CrmPushLogger.log(
                "Push interaction status received: pushId=$pushId, status=${status.apiValue}, " +
                        "configuration=intent",
            )
            WorkManagerPushStatusScheduler(endpoint, siteToken).enqueue(
                context = context,
                pushId = pushId,
                status = status,
                changedAtSeconds = System.currentTimeMillis() / 1_000,
                buttonHash = buttonHash,
            )
        } else {
            CrmPushLogger.log(
                "Push interaction status received: pushId=$pushId, status=${status.apiValue}, " +
                        "configuration=runtime",
            )
            PushRuntime.enqueueStatus(context, pushId, status, buttonHash = buttonHash)
        }
    }

    fun saveAttribution(intent: Intent) {
        val attribution = CrmUtmAttribution(
            source = intent.getStringExtra(EXTRA_UTM_SOURCE),
            medium = intent.getStringExtra(EXTRA_UTM_MEDIUM),
            campaign = intent.getStringExtra(EXTRA_UTM_CAMPAIGN),
            term = intent.getStringExtra(EXTRA_UTM_TERM),
            content = intent.getStringExtra(EXTRA_UTM_CONTENT),
        )
        if (attribution.isEmpty()) return
        CrmUtmAttributionStorage.save(attribution)
    }

    private fun addStatusConfiguration(intent: Intent): Intent {
        val configuration = PushRuntime.getStatusConfiguration() ?: return intent
        return intent
            .putExtra(EXTRA_STATUS_ENDPOINT, configuration.endpoint)
            .putExtra(EXTRA_SITE_TOKEN, configuration.siteToken)
    }

    private fun addAttribution(intent: Intent, utm: PushUtm?): Intent {
        if (utm == null) return intent
        utm.source?.let { intent.putExtra(EXTRA_UTM_SOURCE, it) }
        utm.medium?.let { intent.putExtra(EXTRA_UTM_MEDIUM, it) }
        utm.campaign?.let { intent.putExtra(EXTRA_UTM_CAMPAIGN, it) }
        utm.term?.let { intent.putExtra(EXTRA_UTM_TERM, it) }
        utm.content?.let { intent.putExtra(EXTRA_UTM_CONTENT, it) }
        return intent
    }

    fun notificationId(pushId: String): Int = pushId.hashCode()

    internal fun buttonHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun addCustomAttributes(
        intent: Intent,
        customAttributes: Map<String, String>,
    ): Intent {
        val safeCustomAttributes = customAttributes.filterKeys { it !in reservedExtraKeys }
        intent.putStringArrayListExtra(
            EXTRA_CUSTOM_ATTRIBUTE_KEYS,
            ArrayList(safeCustomAttributes.keys),
        )
        safeCustomAttributes.forEach { (key, value) -> intent.putExtra(key, value) }
        return intent
    }

    private fun interactionUri(action: String, pushId: String): Uri =
        "crm-push://notification/$action/${Uri.encode(pushId)}".toUri()
}
