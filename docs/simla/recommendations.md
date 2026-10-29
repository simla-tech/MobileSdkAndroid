# Product recommendations

The `recommendations` module retrieves product recommendations from Simla and sends block
impression and product click events. The application is responsible for displaying products and
navigating to them.

## Contents

- [Installation](#installation)
- [Retrieving recommendations](#retrieving-recommendations)
- [Request result](#request-result)
- [Tracking interactions](#tracking-interactions)
- [Error handling](#error-handling)

## Installation

Add the module to the application dependencies:

```kotlin
dependencies {
    implementation(platform("com.simla.sdk:sdk-bom:<version>"))
    implementation("com.simla.sdk:recommendations")
}
```

[Initialize the SDK](./sdk.md#initialization), then obtain an object implementing the
recommendations API:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.recommendations.recommendations

val recommendations = CrmSdk.getInstance().recommendations()
```

## Retrieving recommendations

An active recommendation mechanic must be configured in Simla. Its settings determine the required
customer and product data.

The SDK automatically uses the customer identity provided through `setCustomerIdentity()`. See
[Customer identity](./sdk.md#customer-identity) for details.

```kotlin
import com.simla.android.sdk.recommendations.CrmRecommendationRequest

lifecycleScope.launch {
    val result = recommendations.getProducts(
        request = CrmRecommendationRequest(
            // Required symbolic code of an active recommendation mechanic.
            mechanicCode = "mobile-home",

            // Required non-empty code of the recommendation block in the application.
            blockCode = "product_similar",

            // Optional positive internal product ID in Simla.
            // Alternatively, pass productExternalId = "product-101".
            // Do not pass both identifiers.
            productId = 101,

            // For a mechanic that works with an offer, pass one of these instead:
            // offerId = 1001 — positive internal offer ID;
            // offerExternalId = "offer-1001" — external offer ID.
            // Product and offer contexts are mutually exclusive.
        ),
    )

    // The application displays result.products.
}
```

## Request result

`getProducts()` returns `CrmRecommendationResult`:

```kotlin
// Mechanic execution ID used to associate interaction events.
val executionId = result.executionId

// Whether the result is personalized.
val personalized = result.personalized

// Mechanic type and the data it requires.
val mechanicType = result.context.mechanicType
val required = result.context.required

// Recommended product identifiers.
val productIds = result.productIds

// Recommended offer data. The list may be empty.
val products = result.products

products.forEach { product ->
    // Offer ID.
    val offerId = product.id

    // Product ID passed to trackClick().
    val productId = product.productId

    val name = product.name
    val price = product.price

    // Defaults to 0.0.
    val oldPrice = product.oldPrice

    val currency = product.currency

    // Defaults to an empty string.
    val imageUrl = product.imageUrl
    val url = product.url

    // Availability flag; defaults to true.
    val available = product.available
}
```

## Tracking interactions

The application reports impressions and clicks explicitly. Calling `getProducts()` does not send
an impression event.

### Block impression

Call `trackImpression()` after displaying the block:

```kotlin
lifecycleScope.launch {
    recommendations.trackImpression(
        // The request result whose products are displayed.
        recommendation = result,

        // Required absolute screen URI with a scheme.
        // Examples: app://home or myapp://products/101.
        // A relative path such as /home is not valid.
        screenUri = "app://products/101",

        // Optional screen title.
        title = "Product details",
    )
}
```

The method sends a page-view event with the mechanic execution ID. The tracker also uses this
screen's URI and title for subsequent events.

### Product click

Call `trackClick()` when a recommended product is selected:

```kotlin
lifecycleScope.launch {
    recommendations.trackClick(
        // The result from which the product was selected.
        recommendation = result,

        // Required positive product ID.
        // Use product.productId from result.products.
        productId = product.productId,

        // Optional position in the displayed list, indexed from zero.
        position = index,

        // Optional application element identifier.
        element = "recommendations#product_similar",

        // Optional flag indicating navigation away. Defaults to false.
        leaving = true,
    )

    // The application navigates to the selected product.
}
```

The method sends a click event with the mechanic execution ID, product ID, and optional position.

Recommendation events use tracker settings,
including [tracking consent](./tracker.md#tracking-consent)
and the [queue and delivery](./tracker.md#queue-and-delivery) rules. Completion of
`trackImpression()` or `trackClick()` does not confirm server delivery.

A listener registered with `tracker.setEventUpdateListener()` receives recommendation event
updates as well: queued, sending, delivered, retry scheduled, rejected, and dropped. A block
impression has the `PAGE_VIEW` kind and a product click has the `CLICK` kind. `Delivered` confirms
that the server accepted the event, not that Simla finished processing it. See
[Diagnostics](./tracker.md#diagnostics).

## Error handling

For invalid parameters, the SDK throws `IllegalArgumentException` before sending a request. For an
unsuccessful HTTP response, it throws `CrmRecommendationsHttpException`.

```kotlin
import com.simla.android.sdk.recommendations.CrmRecommendationsHttpException
import kotlinx.coroutines.CancellationException

lifecycleScope.launch {
    try {
        val result = recommendations.getProducts(
            request = CrmRecommendationRequest(
                mechanicCode = "mobile-home",
                blockCode = "home",
            ),
        )

        // Display result.products.
    } catch (error: CancellationException) {
        throw error
    } catch (error: CrmRecommendationsHttpException) {
        // Server HTTP response code.
        val httpCode = error.httpCode

        // Messages from the response errors field; may be empty.
        val errors = error.errors

        // Raw server response body.
        val responseBody = error.responseBody

        // Handle the error in the application.
    } catch (error: IllegalArgumentException) {
        // Correct the request parameters.
    } catch (error: Exception) {
        // Handle a network or response parsing error.
    }
}
```
