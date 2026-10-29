package com.simla.android.sdk.recommendations

data class CrmRecommendationRequest(
    val mechanicCode: String,
    val blockCode: String,
    val productId: Long? = null,
    val productExternalId: String? = null,
    val offerId: Long? = null,
    val offerExternalId: String? = null,
)
