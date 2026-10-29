# Tracker

The `tracker` module sends Android application events to Simla, including screen and product views,
clicks, customer registration and login, cart changes, and order placement.

## Contents

- [Installation](#installation)
- [Customer identity](#customer-identity)
- [Tracking consent](#tracking-consent)
- [Sending events](#sending-events)
    - [Event types](#event-types)
        - [Screen and product views](#screen-and-product-views)
        - [Registration](#registration)
        - [Login](#login)
        - [Consultation request](#consultation-request)
        - [Opening the cart](#opening-the-cart)
        - [Cart contents](#cart-contents)
        - [Order placement](#order-placement)
        - [Subscriptions](#subscriptions)
        - [Element click](#element-click)
        - [Contact navigation](#contact-navigation)
        - [Form start](#form-start)
        - [Form submission](#form-submission)
        - [File download](#file-download)
        - [Favorites change](#favorites-change)
    - [Configured events](#configured-events)
    - [Queue and delivery](#queue-and-delivery)
    - [Diagnostics](#diagnostics)

## Installation

Add the module to the application dependencies:

```kotlin
dependencies {
    implementation(platform("com.simla.sdk:sdk-bom:<version>"))
    implementation("com.simla.sdk:tracker")
}
```

[Initialize the SDK](./sdk.md#initialization), then obtain the tracker:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.tracker.tracker

val tracker = CrmSdk.getInstance().tracker()
```

## Customer identity

Notify the SDK when a customer signs in, changes accounts, or signs out by calling
`sdk.setCustomerIdentity(...)`. See [Customer identity](./sdk.md#customer-identity) for details.

The tracker maps `crmCustomerId` and `siteCustomerId` configured in the SDK to
`system_customer_id` and `system_customer_external_id`. Calls to `trackLogin()` and
`trackRegister()` send events and do not replace `setCustomerIdentity()`.

Event queues are stored separately for every combination of site token and SDK user token.

## Tracking consent

If the CRM settings require tracking consent, provide a handler:

```kotlin
val tracker = CrmSdk.getInstance().tracker(
    consentHandler = TrackerConsentHandler {
        // myConsentRepository is an application component.
        // It returns a saved decision or asks the customer for one.
        // The result is TrackerConsentState:
        // GRANTED — consent was granted;
        // DENIED — consent was denied;
        // UNKNOWN — no decision has been made.
        myConsentRepository.getConsent()
    },
)
```

The SDK invokes the handler when the server enables tracking and requires consent. The application
implements the consent interface and stores the decision.

When consent is required, events are sent only after `GRANTED` is returned. Events recorded before
the connection or consent decision completes are buffered in memory and moved to the persistent
queue after `GRANTED`.

The handler can be installed or replaced after obtaining the tracker:

```kotlin
tracker.setConsentHandler(
    TrackerConsentHandler {
        myConsentRepository.getConsent()
    },
)
```

When consent is `DENIED`, the current queue and temporary buffer are cleared.

## Sending events

The application calls tracker methods in response to the corresponding customer actions. Tracking
must be enabled in the Simla site settings. If the server disables tracking, events are not added to
the queue.

### Event types

#### Screen and product views

Call `trackPageView()` when a screen opens:

```kotlin
tracker.trackPageView(
    // Absolute URI with a scheme.
    // Examples: app://catalog or myapp://products/42.
    // A relative path such as /catalog is not valid.
    screenUri = "app://products/42",

    // Optional screen title.
    title = "Sneakers",

    // Optional data for the viewed product.
    // When product is provided, set at least one field:
    // offer, productId, or a non-empty brand.
    product = TrackerViewedProduct(
        // Offer identifier:
        // SystemId(42) — internal ID in Simla;
        // ExternalId("offer-42") — external ID;
        // XmlId("offer-42") — XML ID.
        offer = TrackerOfferIdentifier.ExternalId("offer-42"),

        // Optional internal product ID in Simla.
        productId = 42,

        // Optional brand.
        brand = "Brand",
    ),
)
```

After this call, the tracker uses the screen URI and title for subsequent events. The previous
screen URI is sent as the navigation source (`referrer_url`).

#### Registration

```kotlin
tracker.trackRegister(
    // Provide at least one non-empty customer attribute or subscription.
    customer = TrackerCustomer(
        // Optional customer data.
        firstName = "John",
        lastName = "Smith",
        patronymic = "Michael",
        email = "john@example.com",
        phone = "+12025550123",

        // Optional subscription list; empty by default.
        subscriptions = listOf(
            TrackerSubscription(
                // Available channels: EMAIL, SMS, WABA.
                channel = TrackerSubscriptionChannel.EMAIL,
                // Non-empty subscription address: email or phone number.
                address = "john@example.com",
                // true enables the subscription; false opts out.
                active = true,
            ),
        ),
    ),
)
```

#### Login

```kotlin
tracker.trackLogin(
    // Required non-empty customer email.
    customerEmail = "john@example.com",

    // Optional subscription list; empty by default.
    subscriptions = listOf(
        TrackerSubscription(
            channel = TrackerSubscriptionChannel.EMAIL,
            address = "john@example.com",
            active = true,
        ),
    ),
)
```

#### Consultation request

```kotlin
tracker.trackConsult(
    // Required non-empty customer email.
    customerEmail = "john@example.com",

    // Optional subscription list; empty by default.
    subscriptions = listOf(
        TrackerSubscription(
            channel = TrackerSubscriptionChannel.EMAIL,
            address = "john@example.com",
            active = true,
        ),
    ),
)
```

#### Opening the cart

```kotlin
tracker.trackOpenCart(
    // Optional non-empty customer email.
    customerEmail = "john@example.com",
)
```

#### Cart contents

```kotlin
val items = listOf(
    TrackerCartItem(
        // Offer identifier.
        offer = TrackerOfferIdentifier.ExternalId("offer-42"),

        // Unit price: a finite non-negative number.
        price = 49.90,

        // Quantity: a positive integer.
        quantity = 2,
    ),
)

tracker.trackCart(
    // Required non-empty cart identifier.
    cartId = "cart-123",

    // Optional item list; empty by default.
    items = items,
)
```

#### Order placement

```kotlin
tracker.trackOrder(
    // Prefer the internal CRM order ID (1..2147483647).
    // To use an external ID or an order number instead:
    // TrackerOrderIdentifier.ExternalId("order-123")
    // TrackerOrderIdentifier.Number("2026-123").
    identifier = TrackerOrderIdentifier.SystemId(123),

    // Optional non-empty cart identifier.
    cartId = "cart-123",

    // Optional item list; empty by default.
    items = items,

    // Optional delivery price: a finite non-negative number.
    deliveryPrice = 3.00,

    // Optional total: a finite non-negative number.
    total = 102.80,
)
```

`SystemId` is sent as `order_id` and is used to attribute the source to the order. If the internal
ID
is unavailable, pass `ExternalId`; `Number` records the event but does not identify an order when
sources are uploaded to the CRM.

#### Subscriptions

Call `trackSubscription()` when a subscription changes:

```kotlin
tracker.trackSubscription(
    subscription = TrackerSubscription(
        // Available channels: EMAIL, SMS, WABA.
        channel = TrackerSubscriptionChannel.EMAIL,

        // Non-empty subscription address: email or phone number.
        address = "john@example.com",

        // true enables the subscription; false opts out.
        active = true,
    ),
)
```

#### Element click

```kotlin
tracker.trackClick(
    // Non-empty application element identifier.
    element = "checkout_button",

    // Optional flag indicating navigation away. Defaults to false.
    leaving = false,
)
```

#### Contact navigation

```kotlin
tracker.trackContact(
    // Available channels: TELEGRAM, WHATSAPP.
    channel = TrackerContactChannel.TELEGRAM,
)
```

#### Form start

```kotlin
tracker.trackFormStart(
    // Non-empty application form identifier.
    formSelector = "feedback_form",
)
```

#### Form submission

```kotlin
tracker.trackFormSubmit(
    // Non-empty application form identifier.
    formSelector = "feedback_form",
)
```

#### File download

```kotlin
tracker.trackFileDownload(
    // Required non-empty file name.
    fileName = "catalog.pdf",

    // Optional non-empty application element identifier.
    element = "download_catalog_button",
)
```

#### Favorites change

```kotlin
tracker.trackFavoriteProduct(
    // Offer identifier.
    offer = TrackerOfferIdentifier.ExternalId("offer-42"),

    // false adds to favorites; true removes from favorites.
    // Defaults to false.
    removed = false,
)
```

### Configured events

Every `track*()` method accepts an optional `code` parameter. Its value comes from the event
settings in the Simla administration interface.

```kotlin
tracker.trackClick(
    element = "checkout_button",

    // Optional non-empty code from the Simla event settings.
    code = "checkout_click",
)
```

### Queue and delivery

The tracker stores accepted events in a local SQLite database and sends them automatically:

- every 15 seconds while tracking is active;
- whenever 50 events have accumulated.

One request contains up to 50 events. The queue stores up to 1,000 events and removes the oldest
events when it overflows.

After a network error, events remain queued for another delivery attempt. For an HTTP `429`
response, the tracker observes the server-provided delay. Events permanently rejected by the
server are removed from the queue.

To attempt immediate delivery of one batch, call:

```kotlin
tracker.flush()
```

`flush()` does not guarantee delivery of the entire queue. No request is made when tracking is
disabled or rate limiting is active.

Events emitted before tracker activation are not added to the queue. Completion of a `track*()`
call does not itself confirm server delivery.

### Diagnostics

Register a listener to observe event processing:

```kotlin
tracker.setEventUpdateListener(
    TrackerEventUpdateListener { update ->
        when (update) {
            is TrackerEventUpdate.RetryScheduled ->
                Log.w("Tracker", update.reason)

            is TrackerEventUpdate.Rejected ->
                Log.w("Tracker", update.reason)

            is TrackerEventUpdate.Dropped ->
                Log.w("Tracker", update.reason)

            else -> Unit
        }
    },
)
```

Available updates:

| Update           | Meaning                                                                   |
|------------------|---------------------------------------------------------------------------|
| `Queued`         | Events were added to the queue                                            |
| `Sending`        | A batch delivery attempt is starting; `payload` contains the request JSON |
| `Delivered`      | The server accepted the events                                            |
| `RetryScheduled` | Events remain queued for another delivery attempt                         |
| `Rejected`       | The server permanently rejected an event                                  |
| `Dropped`        | Events were removed during queue cleanup or overflow                      |

Each update contains references to events with their IDs and kinds. `Delivered` confirms server
acceptance, not completion of subsequent processing in Simla.

The listener receives only new updates. Its callback may run off the main thread, must return
quickly, and must not call the tracker synchronously. `Sending.payload` may contain personal data.

Remove the listener with:

```kotlin
tracker.setEventUpdateListener(null)
```
