package com.simla.android.sdk.recommendations

data class CrmRecommendationResult(
    val executionId: Long,
    val personalized: Boolean,
    val context: CrmRecommendationContext,
    val productIds: List<Long> = emptyList(),
    val products: List<CrmRecommendationProduct> = emptyList(),
)
