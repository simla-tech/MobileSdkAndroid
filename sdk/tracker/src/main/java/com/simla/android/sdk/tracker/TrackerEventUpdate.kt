package com.simla.android.sdk.tracker

import com.simla.android.sdk.tracker.model.TrackerEvent

enum class TrackerEventKind {
    PAGE_VIEW,
    CLICK,
    REGISTER,
    LOGIN,
    CONSULT,
    OPEN_CART,
    CONTACT,
    CART,
    ORDER,
    SUBSCRIPTION,
    FORM_START,
    FORM_SUBMIT,
    FILE_DOWNLOAD,
    FAVORITE_PRODUCT,
    UNKNOWN,
}

data class TrackerEventReference(
    val id: String,
    val kind: TrackerEventKind,
)

/** Event lifecycle updates. Sending exposes the outgoing JSON for explicit diagnostics. */
sealed interface TrackerEventUpdate {
    val events: List<TrackerEventReference>

    data class Queued(
        override val events: List<TrackerEventReference>,
    ) : TrackerEventUpdate

    /** Exact batch body before a network attempt; may contain personal data. No auth headers. */
    data class Sending(
        override val events: List<TrackerEventReference>,
        val payload: String,
    ) : TrackerEventUpdate

    /** Backend accepted these events; does not confirm downstream CRM processing. */
    data class Delivered(
        override val events: List<TrackerEventReference>,
    ) : TrackerEventUpdate

    data class RetryScheduled(
        override val events: List<TrackerEventReference>,
        val reason: String,
    ) : TrackerEventUpdate

    data class Rejected(
        val event: TrackerEventReference,
        val reason: String,
    ) : TrackerEventUpdate {
        override val events: List<TrackerEventReference> = listOf(event)
    }

    data class Dropped(
        override val events: List<TrackerEventReference>,
        val reason: String,
    ) : TrackerEventUpdate
}

internal fun TrackerEvent.toReference(): TrackerEventReference = TrackerEventReference(
    id = requireNotNull(id) { "Tracker event must be normalized before it is observed" },
    kind = when (type) {
        "page_view" -> TrackerEventKind.PAGE_VIEW
        "click" -> TrackerEventKind.CLICK
        "register" -> TrackerEventKind.REGISTER
        "login" -> TrackerEventKind.LOGIN
        "consult" -> TrackerEventKind.CONSULT
        "open_cart" -> TrackerEventKind.OPEN_CART
        "contact" -> TrackerEventKind.CONTACT
        "cart" -> TrackerEventKind.CART
        "order" -> TrackerEventKind.ORDER
        "subscription" -> TrackerEventKind.SUBSCRIPTION
        "form_start" -> TrackerEventKind.FORM_START
        "form_submit" -> TrackerEventKind.FORM_SUBMIT
        "file_download" -> TrackerEventKind.FILE_DOWNLOAD
        "favorite_product" -> TrackerEventKind.FAVORITE_PRODUCT
        else -> TrackerEventKind.UNKNOWN
    },
)

internal fun List<TrackerEvent>.toReferences(): List<TrackerEventReference> =
    map(TrackerEvent::toReference)
