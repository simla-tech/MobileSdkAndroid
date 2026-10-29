# Push notifications with Firebase

The `push-firebase` module adds Simla push notifications through Firebase Cloud Messaging,
including device registration, notification display, and status reporting.

## Contents

- [Granting Simla access to send notifications](#granting-simla-access-to-send-notifications)
- [Installation](#installation)
- [Initialization](#initialization)
- [Device registration](#device-registration)
    - [Expected registration delay](#expected-registration-delay)
- [Custom FirebaseMessagingService](#custom-firebasemessagingservice)
- [Notification navigation and data](#notification-navigation-and-data)
- [Push notification analytics](#push-notification-analytics)

## Granting Simla access to send notifications

To let Simla send notifications through your Firebase project, grant its service account access to
the Firebase Cloud Messaging API. Firebase
[supports](https://firebase.google.com/docs/cloud-messaging/send/v1-api#authorize_a_service_account_from_a_different_project)
sending messages with a service account from another project.

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select the project connected
   to your Android application.

2. Open **IAM & Admin → IAM** and select **Grant access**.

3. Enter the Simla service account in **New principals**:

   ```text
   firebase-adminsdk-fbsvc@push-service-sender.iam.gserviceaccount.com
   ```

4. Assign **Firebase Cloud Messaging API Admin** (`roles/firebasecloudmessaging.admin`).

5. Select **Save**.

This role grants permission to send messages through your project's FCM API. See the
[Firebase role documentation](https://firebase.google.com/docs/projects/iam/roles-predefined-product#cloud-messaging-roles).

## Installation

In the application module's `build.gradle[.kts]`:

```kotlin
dependencies {
    implementation(platform("com.simla.sdk:sdk-bom:<version>"))
    implementation("com.simla.sdk:core-okhttp")
    implementation("com.simla.sdk:push-firebase")
}
```

Configure Firebase in the application: add `google-services.json` and apply the
`com.google.gms.google-services` plugin as described in the
[Firebase setup guide](https://firebase.google.com/docs/android/setup).

## Initialization

Pass the push module settings when [initializing the SDK](sdk.md#initialization):

```kotlin
CrmSdk.init(
    context = this,
    config = CrmSdkConfig(
        siteToken = "SITE_TOKEN",
    ),
    networkEngine = SdkOkHttpNetworkEngine.create(),
) {
    pushNotifications(
        PushNotificationsConfig(
            notificationChannel = NotificationChannelSetting.CreateNew(
                config = NotificationChannelConfig(
                    channelId = "simla_push",
                    channelName = "Notifications",
                    channelDescription = "Simla notifications",
                ),
                // The small notification icon is an application resource.
                smallIconResId = R.drawable.ic_notification,
            ),
        ),
    )
}
```

If the application already creates a notification channel, reuse it:

```kotlin
notificationChannel = NotificationChannelSetting.UseExisting(
    channelId = "existing_channel",
    smallIconResId = R.drawable.ic_notification,
)
```

On Android 13 and later, request the `POST_NOTIFICATIONS` permission. It is already declared in the
SDK manifest; the application requests it according to the
[Android documentation](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Device registration

After the customer signs in, set their identity and register the device:

```kotlin
val sdk = CrmSdk.getInstance()

lifecycleScope.launch {
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(
            siteCustomerId = "customer-42",
            siteCode = "main",
        ),
    )

    // Subscribe to the specified categories.
    sdk.pushNotifications().registerDevice(
        categories = setOf("system", "news"),
    )
}
```

Pass `null` to unsubscribe from all categories:

```kotlin
lifecycleScope.launch {
    sdk.pushNotifications().registerDevice(categories = null)
}
```

The SDK uses either the internal CRM ID or the `siteCustomerId + siteCode` pair configured with
`setCustomerIdentity()`. If both forms are provided, both are included in the request. Registration
fails when no customer identity is available.

The SDK retrieves the Firebase installation ID (FID) automatically and falls back to the FCM token
when the FID is unavailable.

### Expected registration delay

`registerDevice()` stores the data in the Online Consultant backend.

The backend transfers registered tokens and categories to Simla with a background job at the start
of every hour. A subscription or unsubscription therefore normally takes effect after the next
successful synchronization, within approximately 60 minutes after `registerDevice()`. Transfer
errors may increase this delay.

## Custom FirebaseMessagingService

By default, the SDK processes messages with its built-in service. If the application uses its own
`FirebaseMessagingService`, disable the built-in service in `build.gradle[.kts]`:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["crmPushFcmServiceEnabled"] = "false"
    }
}
```

Forward events to the SDK from the custom service:

```kotlin
class MyFirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val handled = CrmSdk.getInstance()
            .firebasePushNotifications()
            .handleMessage(this, message)

        if (!handled) {
            // Handle other application push notifications.
        }
    }

    override fun onRegistered(installationId: String) {
        CrmSdk.getInstance()
            .firebasePushNotifications()
            .handleRegistered(installationId)
    }

    override fun onNewToken(token: String) {
        CrmSdk.getInstance()
            .firebasePushNotifications()
            .handleNewToken(token)
    }
}
```

## Notification navigation and data

The following fields are configured in Simla for a push notification and passed to the SDK in the
message `data`:

| Field          | Purpose                                                         |
|----------------|-----------------------------------------------------------------|
| `click_action` | Navigation URI or an `Intent.action` value                      |
| `ca_<key>`     | Key/value pair passed to the launched Intent extras             |
| `image_url`    | Image URL for the expanded notification                         |
| `buttons`      | JSON array of up to two buttons with `title` and `value` fields |

`click_action` behavior:

- `"myapp://orders/123"` — the SDK opens the URI with `ACTION_VIEW`.
- `"OPEN_ORDER"` — the SDK launches the application's start Activity with
  `intent.action = "OPEN_ORDER"`.
- When absent, the SDK launches the application's start Activity with a regular launch Intent.

Key/value pairs from `ca_*` fields are passed through Intent extras. The `ca_` prefix is removed
from the key and the value is passed unchanged. For example:

```json
{
    "click_action": "OPEN_ORDER",
    "ca_orderId": "123"
}
```

In the Activity opened from the notification:

```kotlin
// The click_action value.
val action = intent.action // "OPEN_ORDER"

// ca_orderId is passed as an extra with the orderId key.
val orderId = intent.getStringExtra("orderId") // "123"
```

## Push notification analytics

The SDK automatically reports notification interaction statuses:

| Event                                 | Status      |
|---------------------------------------|-------------|
| Notification received                 | `delivered` |
| Notification or action button clicked | `clicked`   |
| Notification dismissed                | `swiped`    |
