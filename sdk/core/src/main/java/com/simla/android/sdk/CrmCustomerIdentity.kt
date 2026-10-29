package com.simla.android.sdk

/** Identifies a customer by the RetailCRM ID, the site customer ID, or both. */
data class CrmCustomerIdentity(
    val siteCustomerId: String?,
    val siteCode: String?,
    val crmCustomerId: Long?,
) {
    constructor(crmCustomerId: Long) : this(null, null, crmCustomerId)

    constructor(siteCustomerId: String, siteCode: String) : this(siteCustomerId, siteCode, null)

    constructor(crmCustomerId: Long, siteCustomerId: String, siteCode: String) :
            this(siteCustomerId, siteCode, crmCustomerId)

    init {
        require(crmCustomerId == null || crmCustomerId > 0) {
            "CRM customer ID must be greater than zero"
        }
        require((siteCustomerId == null) == (siteCode == null)) {
            "Site customer ID and site code must be provided together"
        }
        require(crmCustomerId != null || siteCustomerId != null) {
            "CRM customer ID or site customer ID must be provided"
        }
        siteCustomerId?.let {
            require(it.isNotBlank()) { "Site customer ID must not be blank" }
            require(it.trim().length <= 255) {
                "Site customer ID must not exceed 255 characters"
            }
        }
        siteCode?.let {
            require(it.isNotBlank()) { "Site code must not be blank" }
            require(it.trim().length <= 255) { "Site code must not exceed 255 characters" }
        }
    }
}
