package com.simla.android.sdk.recommendations

interface Recommendations {
    suspend fun getProducts(request: CrmRecommendationRequest): CrmRecommendationResult

    suspend fun trackImpression(
        recommendation: CrmRecommendationResult,
        screenUri: String,
        title: String? = null,
    )

    suspend fun trackClick(
        recommendation: CrmRecommendationResult,
        productId: Long,
        position: Int? = null,
        element: String? = null,
        leaving: Boolean = false,
    )
}
