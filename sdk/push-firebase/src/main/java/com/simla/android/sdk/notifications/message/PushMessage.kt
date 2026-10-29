package com.simla.android.sdk.notifications.message

internal data class PushMessage(
    val pushId: String,
    val category: String?,
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val clickAction: String?,
    val customAttributes: Map<String, String>,
    val buttons: List<PushButton>,
    val utm: PushUtm?,
    val data: Map<String, String>,
)

internal data class PushUtm(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
)
