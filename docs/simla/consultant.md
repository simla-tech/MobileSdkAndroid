# Online consultant

The `consultant` module provides a ready-to-use chat screen for communication between a customer
and managers in the CRM connected to Simla. The screen is available for Jetpack Compose and
Android View.

## Contents

- [Installation](#installation)
- [Jetpack Compose](#jetpack-compose)
- [Android View](#android-view)
    - [Fragment](#fragment)
    - [Activity](#activity)
- [Customer identity](#customer-identity)
- [Real-time events](#real-time-events)
- [Unread messages](#unread-messages)
- [Localization](#localization)
- [Chat capabilities](#chat-capabilities)

## Installation

Add the module to the application dependencies:

```kotlin
dependencies {
    implementation(platform("com.simla.sdk:sdk-bom:<version>"))
    implementation("com.simla.sdk:consultant")
}
```

[Initialize the SDK](./sdk.md#initialization) before displaying the chat.

## Jetpack Compose

Place `OnlineConsultantChatScreen` on an application screen:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.ui.OnlineConsultantChatScreen

@Composable
fun ConsultantScreen() {
    OnlineConsultantChatScreen(
        // Initialized SDK instance.
        sdk = CrmSdk.getInstance(),

        // Optional Modifier for screen size and placement.
        modifier = Modifier.fillMaxSize(),

        // Optional handler for connection, message sending,
        // and history loading errors. Empty by default.
        onError = { error ->
            Log.e("Consultant", "Chat error", error)
        },
    )
}
```

## Android View

Add `OnlineConsultantView` to the XML layout:

```xml
<com.simla.android.sdk.consultant.ui.OnlineConsultantView
    android:id="@+id/consultantView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Fragment

Bind the SDK in the overridden `onViewCreated()` method:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.ui.OnlineConsultantView

class ConsultantFragment : Fragment(R.layout.fragment_consultant) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val consultantView =
            view.findViewById<OnlineConsultantView>(R.id.consultantView)

        consultantView.bind(
            sdk = CrmSdk.getInstance(),

            // The chat state subscription is cancelled when
            // the Fragment View is destroyed.
            scope = viewLifecycleOwner.lifecycleScope,

            // Activity registry used by the system file picker.
            activityResultRegistry =
                requireActivity().activityResultRegistry,

            // File selection is bound to the View lifecycle.
            // bind() must be called before the STARTED state.
            lifecycleOwner = viewLifecycleOwner,

            // Non-empty file-picker registration key.
            // It must survive screen recreation and be unique
            // within the Activity registry.
            activityResultKey = "consultant_fragment_files",

            // Optional handler for chat and file-picker errors.
            onError = { error ->
                Log.e("Consultant", "Chat error", error)
            },
        )
    }
}
```

### Activity

Bind the SDK in `onCreate()` after setting the layout:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.ui.OnlineConsultantView

class ConsultantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultant)

        val consultantView =
            findViewById<OnlineConsultantView>(R.id.consultantView)

        consultantView.bind(
            sdk = CrmSdk.getInstance(),

            // The chat state subscription is cancelled when
            // the Activity is destroyed.
            scope = lifecycleScope,

            // Activity registry used by the system file picker.
            activityResultRegistry = this.activityResultRegistry,

            // File selection is bound to the Activity lifecycle.
            // bind() must be called before the STARTED state.
            lifecycleOwner = this,

            // Non-empty key unique within the Activity registry.
            // Reuse the same key after Activity recreation.
            activityResultKey = "consultant_activity_files",

            // Optional handler for chat and file-picker errors.
            onError = { error ->
                Log.e("Consultant", "Chat error", error)
            },
        )
    }
}
```

## Customer identity

The application must notify the SDK when a customer signs in, changes accounts, or signs out by
calling `sdk.setCustomerIdentity(...)`. The SDK does not observe application authentication on its
own. See [Customer identity](./sdk.md#customer-identity) for details.

When a new identity is provided, the SDK disconnects the current session and automatically connects
the specified customer's session. The chat does not need to be reconnected separately.

Call `sdk.setCustomerIdentity(null)` when the customer signs out. This informs the SDK about the
sign-out and switches the session to an anonymous customer.

The SDK stores a separate consultant user token for each identity. Only `siteCustomerId` is sent as
`X-Site-Customer-Id` in `POST /connect`; the internal `crmCustomerId` does not replace this header.
When only the CRM ID is provided, conversation history is restored using the SDK token stored on
the current installation.

## Real-time events

When the online consultant is initialized, the SDK automatically subscribes to events over
WebSocket. New messages and chat state changes therefore appear without additional application
actions.

The subscription follows the application lifecycle. The SDK disconnects WebSocket in the
background, then reconnects and resubscribes when the application returns to the foreground.

## Unread messages

The unread message count is available as `StateFlow<Int>`:

```kotlin
val consultant = CrmSdk.getInstance().onlineConsultant()
val unreadCount = consultant.unreadCount
```

For example, in Compose:

```kotlin
val unreadCount by CrmSdk.getInstance()
    .onlineConsultant()
    .unreadCount
    .collectAsState()

if (unreadCount > 0) {
    Badge {
        Text(unreadCount.toString())
    }
}
```

The ready-to-use chat screen automatically marks visible incoming messages as read. If the unread
counter is disabled in the Simla Online Consultant settings, `unreadCount` returns `0`.

## Localization

The SDK contains English strings by default, as well as Russian and Spanish translations. The
application resource configuration determines the language.

To change text, override a public `crm_consultant_*` string resource in the application:

```xml
<!-- res/values/strings.xml -->
<resources>
    <string name="crm_consultant_message">Type a message</string>
    <string name="crm_consultant_send_message">Send</string>
</resources>
```

When overriding strings with parameters, preserve their format, for example `%1$s` or `%1$d`.

## Chat capabilities

The screen automatically loads history, receives new messages, and sends text. In Compose and the
Android View examples above, the attachment button opens the system file picker and supports
sending multiple files.

The ready-to-use interface supports:

- text messages, links, and quotes;
- images and file attachments;
- product and order cards;
- quick replies;
- contact data collection forms;
- an inquiry form shown when the consultant is offline;
- message delivery states;
- loading earlier messages while scrolling through history.

Contact forms, offline mode, and chat appearance follow the Online Consultant settings received
from the server.

The SDK maintains the WebSocket connection while the application is in the foreground and restores
it after the application returns from the background.
