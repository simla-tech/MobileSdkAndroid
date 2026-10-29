package com.simla.android.sdk.recommendations

class CrmRecommendationsHttpException(
    val httpCode: Int,
    val responseBody: String,
    val errors: List<String> = emptyList(),
) : Exception(
    buildString {
        append("Recommendations request failed: HTTP ")
        append(httpCode)
        if (errors.isNotEmpty()) {
            append(": ")
            append(errors.joinToString())
        }
    },
)
