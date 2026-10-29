# Simla Android SDK

The SDK adds an online consultant, customer activity tracking, product recommendations, and push
notifications to an Android application.

## Modules

- `sdk:core` — shared runtime and the `CrmSdk` entry point;
- `sdk:core-okhttp` — the standard OkHttp transport;
- `sdk:consultant` — online consultant;
- `sdk:tracker` — customer activity tracking;
- `sdk:recommendations` — product recommendations;
- `sdk:push-firebase` — Firebase push notifications;
- `sdk:bom` — compatible versions of the SDK artifacts.

## Requirements

- To use the online consultant, the Online Consultant module must be enabled in Simla.
- Android 7.0 (API 24) or later.
- Android `compileSdk 36`.
- The SDK is compiled for JVM 17 with Kotlin 2.0.21.

## Installation

Add Maven Central to `settings.gradle[.kts]`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the required dependencies to the application module's `build.gradle[.kts]`. A dependency makes
the module API available to the application; modules are started separately in `CrmSdk.init(...)`:

```kotlin
dependencies {
    implementation(platform("com.simla.sdk:sdk-bom:<version>"))

    // Keep only the modules your application needs.
    // The shared core module is included automatically.
    implementation("com.simla.sdk:core-okhttp")     // Default OkHttp transport
    implementation("com.simla.sdk:consultant")      // Online consultant
    implementation("com.simla.sdk:tracker")         // Tracking
    implementation("com.simla.sdk:recommendations") // Recommendations
    implementation("com.simla.sdk:push-firebase")   // Push notifications
}
```

## Initialization

Initialize the SDK in `Application.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = CrmSdkConfig(
            // Required site token from the Online Consultant
            // or tracker settings in Simla.
            siteToken = "SITE_TOKEN",

            // The default server is https://c.simla.com.
            // To use another server:
            // baseUrl = "https://your-server.example",
        )

        CrmSdk.init(
            context = this,
            config = config,

            // Standard OkHttp transport.
            networkEngine = SdkOkHttpNetworkEngine.create(),

            // To use a custom transport, depend on core without core-okhttp
            // and pass your SdkNetworkEngine implementation instead of the line above:
            // networkEngine = CustomSdkNetworkEngine(),

            // Optional URI and title of the initial screen.
            pageContext = SessionPageContext(
                url = "app://home",
                title = "Home",
            ),
        ) {
            // Install and start the required modules with the shared SDK session.
            onlineConsultant()
            tracker()
            recommendations()

            pushNotifications(
                PushNotificationsConfig(
                    notificationChannel = NotificationChannelSetting.UseExisting(
                        channelId = "app_notifications",
                        smallIconResId = R.drawable.ic_notification,
                    ),
                ),
            )
        }
    }
}
```

## Customer identity

After sign-in, provide the available customer identifiers. Three forms are supported:

```kotlin
val sdk = CrmSdk.getInstance()

lifecycleScope.launch {
    // Internal customer ID in Simla.
    sdk.setCustomerIdentity(CrmCustomerIdentity(crmCustomerId = 37))

    // Customer ID on the website and the Simla site code.
    // siteCustomerId and siteCode must be provided together.
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(siteCustomerId = "customer-42", siteCode = "main"),
    )

    // Both forms of identification.
    // If a module accepts either identifier,
    // the SDK gives priority to crmCustomerId.
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(
            crmCustomerId = 37,
            siteCustomerId = "customer-42",
            siteCode = "main",
        ),
    )
}
```

Pass `null` when the customer signs out:

```kotlin
lifecycleScope.launch {
    // Switches the session to an anonymous customer.
    sdk.setCustomerIdentity(null)
}
```

## Using modules

After initialization, obtain the SDK instance with:

```kotlin
val sdk = CrmSdk.getInstance()
```

Accessor methods return module APIs. The first call to consultant, tracker, or recommendations also
installs that module lazily if it was not selected in `init()`. The push module requires
configuration and must be installed in `init()`:

```kotlin
sdk.onlineConsultant() // Requires the consultant module.
sdk.tracker()          // Requires the tracker module.
sdk.recommendations()  // Requires the recommendations module.

// Requires the push-firebase module and PushNotificationsConfig during initialization.
sdk.pushNotifications()
```

Module documentation:

- [Online consultant](./consultant.md) — displaying the chat.
- [Tracker](./tracker.md) — sending customer events.
- [Product recommendations](./recommendations.md) — retrieving products and tracking interactions.
- [Push notifications](./push-firebase.md) — configuring Firebase and subscription categories.

## Verification

```shell
./gradlew sdkTest apiCheckAll
./gradlew :sdk:core:assembleSimlaRelease :sdk:consultant:assembleSimlaRelease \
  :sdk:tracker:assembleSimlaRelease :sdk:recommendations:assembleSimlaRelease \
  :sdk:push-firebase:assembleSimlaRelease :sdk:bom:build
```

`apiCheckAll` compares the modules' current public API against the stored `*.api` baseline files and
detects unintended changes to the public contract. If the public API changes intentionally,
`apiDumpAll` updates the baseline files. Review the resulting changes before including them in a
pull request.

## Contributing

Before opening a pull request:

1. Describe the problem or proposed change in an issue.
2. Add tests for changed behavior.
3. Run `./gradlew sdkTest apiCheckAll`.
4. If the public API changed intentionally, run `./gradlew apiDumpAll`, review the updated API
   baselines, and include them in the pull request.

Do not commit tokens, keys, `local.properties`, `google-services.json`, or other secrets.

## Security

Do not disclose vulnerability details in public issues.

Use GitHub Private Vulnerability Reporting under **Security → Report a vulnerability**. If the form
is unavailable, contact the project maintainers through a private channel first.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](../../LICENSE).
