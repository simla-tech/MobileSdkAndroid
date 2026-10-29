package com.simla.android.sdk.recommendations

data class CrmRecommendationProduct(
    val id: Long,
    val productId: Long,
    val name: String,
    val price: Double,
    val oldPrice: Double = 0.0,
    val currency: String,
    val imageUrl: String = "",
    val url: String = "",
    val available: Boolean = true,
)
