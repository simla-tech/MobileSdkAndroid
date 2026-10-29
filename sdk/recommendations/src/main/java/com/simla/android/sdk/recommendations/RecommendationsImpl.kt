package com.simla.android.sdk.recommendations

import com.simla.android.sdk.CrmCustomerIdentity
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpNetworkEngine
import com.simla.android.sdk.network.SdkHttpRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RecommendationsImpl(
    private val networkEngine: SdkHttpNetworkEngine,
    private val authorizedHeadersProvider: suspend () -> Map<String, String>,
    private val customerIdentityProvider: () -> CrmCustomerIdentity?,
    private val endpointResolver: (String) -> String,
    private val pageViewTracker: suspend (String, String?, JsonObject) -> Unit,
    private val eventTracker: suspend (String, JsonObject) -> Unit,
    private val logger: SdkLogger? = null,
) : Recommendations {
    override suspend fun getProducts(
        request: CrmRecommendationRequest,
    ): CrmRecommendationResult {
        val customerIdentity = customerIdentityProvider()
        val normalizedRequest = request.copy(
            blockCode = request.blockCode.trim(),
            productExternalId = request.productExternalId.normalizedOrNull(),
            offerExternalId = request.offerExternalId.normalizedOrNull(),
        )
        normalizedRequest.validate()
        val httpRequest = SdkHttpRequest(
            url = endpointResolver(RECOMMENDATIONS_ENDPOINT),
            headers = authorizedHeadersProvider(),
            body = SdkJson.encodeToString(normalizedRequest.toDto(customerIdentity)),
        )
        logPayload("Recommendations request POST ${httpRequest.url}", httpRequest.body)
        val response = networkEngine.postJson(httpRequest)
        logPayload("Recommendations response HTTP ${response.code}", response.body)
        if (!response.isSuccessful) {
            val errors = runCatching {
                SdkJson.decodeFromString<RecommendationErrorResponse>(response.body).errors
            }.getOrDefault(emptyList())
            throw CrmRecommendationsHttpException(
                httpCode = response.code,
                responseBody = response.body,
                errors = errors,
            )
        }

        return SdkJson.decodeFromString<CrmRecommendationResultDto>(response.body).toDomain()
    }

    override suspend fun trackImpression(
        recommendation: CrmRecommendationResult,
        screenUri: String,
        title: String?,
    ) {
        pageViewTracker(screenUri, title, recommendationData(recommendation))
    }

    override suspend fun trackClick(
        recommendation: CrmRecommendationResult,
        productId: Long,
        position: Int?,
        element: String?,
        leaving: Boolean,
    ) {
        require(productId > 0) { "Recommendation product ID must be greater than zero" }
        require(position == null || position >= 0) { "Recommendation position must not be negative" }
        val data = recommendationData(
            recommendation = recommendation,
            productId = productId,
            position = position,
            element = element,
            leaving = leaving,
        )
        eventTracker("click", data)
    }

    private fun recommendationData(
        recommendation: CrmRecommendationResult,
        productId: Long? = null,
        position: Int? = null,
        element: String? = null,
        leaving: Boolean? = null,
    ) = buildJsonObject {
        put("recommendation_execution_id", recommendation.executionId)
        productId?.let { put("product_id", it) }
        position?.let { put("recommendation_position", it) }
        element?.takeIf { it.isNotBlank() }?.let { put("el", it) }
        leaving?.let { put("leaving", it) }
    }

    private fun CrmRecommendationRequest.validate() {
        require(mechanicCode.isNotEmpty()) { "Recommendation mechanic code must not be empty" }
        require(blockCode.isNotBlank()) { "Recommendation block code must not be blank" }
        require(productId == null || productId > 0) {
            "Recommendation product ID must be greater than zero"
        }
        require(offerId == null || offerId > 0) {
            "Recommendation offer ID must be greater than zero"
        }
        require(productId == null || productExternalId.isNullOrBlank()) {
            "Recommendation product requires either ID or external ID"
        }
        require(offerId == null || offerExternalId.isNullOrBlank()) {
            "Recommendation offer requires either ID or external ID"
        }
        require(
            (productId == null && productExternalId.isNullOrBlank()) ||
                    (offerId == null && offerExternalId.isNullOrBlank()),
        ) { "Recommendation requires either product context or offer context" }
    }

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private fun logPayload(label: String, body: String) {
        val logger = logger ?: return
        logger.d(label)
        // Leave room for the prefix and multibyte characters within Logcat's entry limit.
        body.chunked(800).forEachIndexed { index, chunk ->
            logger.d("$label [$index]: $chunk")
        }
    }

    @Serializable
    private data class RecommendationErrorResponse(
        val errors: List<String> = emptyList(),
    )

    private companion object {
        private const val RECOMMENDATIONS_ENDPOINT = "recommendations/products"
    }
}
