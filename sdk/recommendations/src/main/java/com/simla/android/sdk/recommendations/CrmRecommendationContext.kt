package com.simla.android.sdk.recommendations

data class CrmRecommendationContext(
    val mechanicType: String,
    val required: List<String> = emptyList(),
)
