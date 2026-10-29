package com.simla.android.sdk.notifications.registration

import kotlinx.serialization.Serializable

@Serializable
internal data class PushTokenRequest(
    val customerId: Long? = null,
    val customerExternalId: String? = null,
    val crmSiteCode: String? = null,
    val fid: String? = null,
    val token: String? = null,
    val type: String,
    val deviceType: String,
    val categories: List<String>,
)
