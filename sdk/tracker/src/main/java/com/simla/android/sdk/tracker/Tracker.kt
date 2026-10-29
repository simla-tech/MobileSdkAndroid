package com.simla.android.sdk.tracker

interface Tracker {
    /**
     * Replaces the listener for future event updates. Pass null to remove it.
     * Callbacks run on the thread producing the update, not necessarily the main thread.
     * Keep callbacks non-blocking; do not synchronously call back into the tracker.
     * Listener exceptions are isolated from event processing. Past updates are not replayed.
     */
    fun setEventUpdateListener(listener: TrackerEventUpdateListener?)

    /**
     * Registers the application-owned consent handler.
     *
     * The handler is consulted only when the backend enables tracking and requires consent.
     */
    fun setConsentHandler(handler: TrackerConsentHandler?)

    suspend fun trackPageView(
        screenUri: String,
        title: String? = null,
        product: TrackerViewedProduct? = null,
        code: String? = null,
    )

    suspend fun trackClick(
        element: String,
        leaving: Boolean = false,
        code: String? = null,
    )

    suspend fun trackRegister(customer: TrackerCustomer, code: String? = null)

    suspend fun trackLogin(
        customerEmail: String,
        subscriptions: List<TrackerSubscription> = emptyList(),
        code: String? = null,
    )

    suspend fun trackConsult(
        customerEmail: String,
        subscriptions: List<TrackerSubscription> = emptyList(),
        code: String? = null,
    )

    suspend fun trackOpenCart(customerEmail: String? = null, code: String? = null)

    suspend fun trackContact(channel: TrackerContactChannel, code: String? = null)

    suspend fun trackCart(
        cartId: String,
        items: List<TrackerCartItem> = emptyList(),
        code: String? = null,
    )

    suspend fun trackOrder(
        identifier: TrackerOrderIdentifier,
        cartId: String? = null,
        items: List<TrackerCartItem> = emptyList(),
        deliveryPrice: Double? = null,
        total: Double? = null,
        code: String? = null,
    )

    suspend fun trackSubscription(subscription: TrackerSubscription, code: String? = null)

    suspend fun trackFormStart(formSelector: String, code: String? = null)

    suspend fun trackFormSubmit(formSelector: String, code: String? = null)

    suspend fun trackFileDownload(
        fileName: String,
        element: String? = null,
        code: String? = null,
    )

    suspend fun trackFavoriteProduct(
        offer: TrackerOfferIdentifier,
        removed: Boolean = false,
        code: String? = null,
    )

    suspend fun flush()
}
