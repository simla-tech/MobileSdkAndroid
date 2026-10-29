package com.simla.android.sdk.tracker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class TrackerEvent(
    val type: String,
    val code: String? = null,
    val data: JsonObject? = null,
    val name: String? = null,
    val id: String? = null,
    @SerialName("page_url")
    val pageUrl: String? = null,
    @SerialName("page_title")
    val pageTitle: String? = null,
    @SerialName("referrer_url")
    val referrerUrl: String? = null,
    val timestamp: String? = null,
    @SerialName("create_customer")
    val createCustomer: Boolean = false,
    @SerialName("create_customer_by_email")
    val createCustomerByEmail: Boolean = false,
    @SerialName("system_customer_id")
    val systemCustomerId: Long? = null,
    @SerialName("system_customer_external_id")
    val systemCustomerExternalId: String? = null,
    @SerialName("ga_client_id")
    val gaClientId: String? = null,
    val utm: TrackerUtm? = null,
)
