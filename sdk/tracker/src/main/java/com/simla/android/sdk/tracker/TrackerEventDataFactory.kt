package com.simla.android.sdk.tracker

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object TrackerEventDataFactory {
    fun pageView(product: TrackerViewedProduct?): JsonObject? = product?.let {
        buildJsonObject {
            putOfferIdentifier(product.offer)
            product.productId?.let { put("product_id", it) }
            product.brand?.takeIf(String::isNotBlank)?.let { put("offer_brand", it) }
        }
    }

    fun click(element: String, leaving: Boolean): JsonObject = buildJsonObject {
        put("el", element.requireNotBlank("Element"))
        put("leaving", leaving)
    }

    fun register(customer: TrackerCustomer): JsonObject = customer(customer)

    fun customerEmail(
        email: String,
        subscriptions: List<TrackerSubscription> = emptyList()
    ): JsonObject =
        buildJsonObject {
            put("customer_email", email.requireNotBlank("Customer email"))
            putSubscriptions(subscriptions)
        }

    fun openCart(customerEmail: String?): JsonObject? = customerEmail?.let {
        buildJsonObject { put("customer_email", it.requireNotBlank("Customer email")) }
    }

    fun contact(channel: TrackerContactChannel): JsonObject = buildJsonObject {
        put("contact_channel", channel.wireValue)
    }

    fun cart(cartId: String, items: List<TrackerCartItem>): JsonObject = buildJsonObject {
        put("cart_id", cartId.requireNotBlank("Cart ID"))
        putItems(items)
    }

    fun order(
        identifier: TrackerOrderIdentifier,
        cartId: String?,
        items: List<TrackerCartItem>,
        deliveryPrice: Double?,
        total: Double?,
    ): JsonObject = buildJsonObject {
        when (identifier) {
            is TrackerOrderIdentifier.SystemId -> put("order_id", identifier.value)
            is TrackerOrderIdentifier.ExternalId -> put("order_external_id", identifier.value)
            is TrackerOrderIdentifier.Number -> put("order_number", identifier.value)
        }
        cartId?.let { put("cart_id", it.requireNotBlank("Cart ID")) }
        putItems(items)
        deliveryPrice?.let {
            require(it.isFinite() && it >= 0) { "Delivery price must be a non-negative number" }
            put("delivery_price", it)
        }
        total?.let {
            require(it.isFinite() && it >= 0) { "Order total must be a non-negative number" }
            put("total", it)
        }
    }

    fun subscription(subscription: TrackerSubscription): JsonObject = buildJsonObject {
        putSubscription(subscription)
    }

    fun form(selector: String): JsonObject = buildJsonObject {
        put("form_selector", selector.requireNotBlank("Form selector"))
    }

    fun fileDownload(fileName: String, element: String?): JsonObject = buildJsonObject {
        put("file_name", fileName.requireNotBlank("File name"))
        element?.let { put("el", it.requireNotBlank("Element")) }
    }

    fun favoriteProduct(offer: TrackerOfferIdentifier, removed: Boolean): JsonObject =
        buildJsonObject {
            putOfferIdentifier(offer)
            put("removed", removed)
        }

    private fun customer(customer: TrackerCustomer): JsonObject = buildJsonObject {
        customer.firstName?.takeIf(String::isNotBlank)?.let { put("customer_first_name", it) }
        customer.lastName?.takeIf(String::isNotBlank)?.let { put("customer_last_name", it) }
        customer.patronymic?.takeIf(String::isNotBlank)?.let {
            put("customer_patronymic", it)
        }
        customer.email?.takeIf(String::isNotBlank)?.let { put("customer_email", it) }
        customer.phone?.takeIf(String::isNotBlank)?.let { put("customer_phone", it) }
        putSubscriptions(customer.subscriptions)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putOfferIdentifier(
        offer: TrackerOfferIdentifier?,
    ) {
        when (offer) {
            is TrackerOfferIdentifier.SystemId -> put("offer_id", offer.value)
            is TrackerOfferIdentifier.ExternalId -> put("offer_external_id", offer.value)
            is TrackerOfferIdentifier.XmlId -> put("offer_xml_id", offer.value)
            null -> Unit
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putItemIdentifier(
        offer: TrackerOfferIdentifier,
    ) {
        when (offer) {
            is TrackerOfferIdentifier.SystemId -> put("id", offer.value)
            is TrackerOfferIdentifier.ExternalId -> put("external_id", offer.value)
            is TrackerOfferIdentifier.XmlId -> put("xml_id", offer.value)
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putItems(items: List<TrackerCartItem>) {
        if (items.isEmpty()) return
        put(
            "items",
            JsonArray(items.map { item ->
                buildJsonObject {
                    putItemIdentifier(item.offer)
                    put("price", item.price)
                    put("quantity", item.quantity)
                }
            }),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putSubscriptions(
        subscriptions: List<TrackerSubscription>,
    ) {
        if (subscriptions.isEmpty()) return
        put(
            "subscriptions",
            JsonArray(subscriptions.map { subscription ->
                buildJsonObject { putSubscription(subscription) }
            }),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putSubscription(
        subscription: TrackerSubscription,
    ) {
        put("channel", subscription.channel.wireValue)
        put("subscription", subscription.address)
        put("active", subscription.active)
    }

    private val TrackerContactChannel.wireValue: String
        get() = name.lowercase()

    private val TrackerSubscriptionChannel.wireValue: String
        get() = name.lowercase()

    private fun String.requireNotBlank(name: String): String =
        trim().also { require(it.isNotEmpty()) { "$name must not be blank" } }
}
