package com.simla.android.sdk.tracker

enum class TrackerContactChannel {
    TELEGRAM,
    WHATSAPP,
}

enum class TrackerSubscriptionChannel {
    EMAIL,
    SMS,
    WABA,
}

data class TrackerSubscription(
    val channel: TrackerSubscriptionChannel,
    val address: String,
    val active: Boolean,
) {
    init {
        require(address.isNotBlank()) { "Subscription address must not be blank" }
    }
}

data class TrackerCustomer(
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val subscriptions: List<TrackerSubscription> = emptyList(),
) {
    init {
        require(
            listOf(firstName, lastName, patronymic, email, phone).any { !it.isNullOrBlank() } ||
                    subscriptions.isNotEmpty(),
        ) { "At least one customer attribute must be provided" }
    }
}

sealed interface TrackerOfferIdentifier {
    @JvmInline
    value class SystemId(val value: Long) : TrackerOfferIdentifier {
        init {
            require(value in 1..UInt.MAX_VALUE.toLong()) {
                "Offer system ID must fit an unsigned 32-bit integer"
            }
        }
    }

    @JvmInline
    value class ExternalId(val value: String) : TrackerOfferIdentifier {
        init {
            require(value.isNotBlank()) { "Offer external ID must not be blank" }
        }
    }

    @JvmInline
    value class XmlId(val value: String) : TrackerOfferIdentifier {
        init {
            require(value.isNotBlank()) { "Offer XML ID must not be blank" }
        }
    }
}

data class TrackerViewedProduct(
    val offer: TrackerOfferIdentifier? = null,
    val productId: Long? = null,
    val brand: String? = null,
) {
    init {
        require(productId == null || productId in 1..UInt.MAX_VALUE.toLong()) {
            "Product ID must fit an unsigned 32-bit integer"
        }
        require(offer != null || productId != null || !brand.isNullOrBlank()) {
            "At least one product attribute must be provided"
        }
    }
}

data class TrackerCartItem(
    val offer: TrackerOfferIdentifier,
    val price: Double,
    val quantity: Int,
) {
    init {
        require(price.isFinite() && price >= 0) { "Item price must be a non-negative number" }
        require(quantity > 0) { "Item quantity must be greater than zero" }
    }
}

sealed interface TrackerOrderIdentifier {
    @JvmInline
    value class SystemId(val value: Long) : TrackerOrderIdentifier {
        init {
            require(value in 1..Int.MAX_VALUE.toLong()) {
                "Order system ID must fit a positive signed 32-bit integer"
            }
        }
    }

    @JvmInline
    value class ExternalId(val value: String) : TrackerOrderIdentifier {
        init {
            require(value.isNotBlank()) { "Order external ID must not be blank" }
        }
    }

    @JvmInline
    value class Number(val value: String) : TrackerOrderIdentifier {
        init {
            require(value.isNotBlank()) { "Order number must not be blank" }
        }
    }
}
