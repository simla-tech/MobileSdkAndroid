package com.simla.android.sdk.recommendations

import com.simla.android.sdk.CrmCustomerIdentity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CrmRecommendationRequestDto(
    @SerialName("mechanic_code") val mechanicCode: String,
    @SerialName("block_code") val blockCode: String,
    @SerialName("system_customer_id") val systemCustomerId: Long? = null,
    @SerialName("system_customer_external_id") val systemCustomerExternalId: String? = null,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_external_id") val productExternalId: String? = null,
    @SerialName("offer_id") val offerId: Long? = null,
    @SerialName("offer_external_id") val offerExternalId: String? = null,
)

@Serializable
internal data class CrmRecommendationResultDto(
    val executionId: Long,
    val personalized: Boolean,
    val context: CrmRecommendationContextDto,
    val productIds: List<Long> = emptyList(),
    val products: List<CrmRecommendationProductDto> = emptyList(),
)

@Serializable
internal data class CrmRecommendationContextDto(
    val mechanicType: String,
    val required: List<String> = emptyList(),
)

@Serializable
internal data class CrmRecommendationProductDto(
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

internal fun CrmRecommendationRequest.toDto(
    customerIdentity: CrmCustomerIdentity?,
) = CrmRecommendationRequestDto(
    mechanicCode = mechanicCode,
    blockCode = blockCode,
    systemCustomerId = customerIdentity?.crmCustomerId,
    systemCustomerExternalId = customerIdentity
        ?.takeIf { it.crmCustomerId == null }
        ?.siteCustomerId
        ?.trim()
        ?.takeIf(String::isNotEmpty),
    productId = productId,
    productExternalId = productExternalId,
    offerId = offerId,
    offerExternalId = offerExternalId,
)

internal fun CrmRecommendationResultDto.toDomain() = CrmRecommendationResult(
    executionId = executionId,
    personalized = personalized,
    context = CrmRecommendationContext(
        mechanicType = context.mechanicType,
        required = context.required,
    ),
    productIds = productIds,
    products = products.map { product ->
        CrmRecommendationProduct(
            id = product.id,
            productId = product.productId,
            name = product.name,
            price = product.price,
            oldPrice = product.oldPrice,
            currency = product.currency,
            imageUrl = product.imageUrl,
            url = product.url,
            available = product.available,
        )
    },
)
