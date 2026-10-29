# Push-уведомления через Firebase

Модуль `push-firebase` подключает push-уведомления RetailCRM через Firebase Cloud Messaging:
регистрацию устройства, отображение уведомлений и отправку статусов.

## Оглавление

- [Доступ RetailCRM к отправке уведомлений](#доступ-retailcrm-к-отправке-уведомлений)
- [Подключение](#подключение)
- [Инициализация](#инициализация)
- [Регистрация устройства](#регистрация-устройства)
  [Плановая задержка при регистрации устройства](#плановая-задержка-при-регистрации -устройства)
- [Собственный FirebaseMessagingService](#собственный-firebasemessagingservice)
- [Переходы и данные уведомления](#переходы-и-данные-уведомления)
- [Аналитика push-уведомлений](#аналитика-push-уведомлений)

## Доступ RetailCRM к отправке уведомлений

Чтобы RetailCRM мог отправлять уведомления через ваш Firebase-проект, предоставьте сервисному
аккаунту RetailCRM доступ к Firebase Cloud Messaging API.
Firebase [поддерживает](https://firebase.google.com/docs/cloud-messaging/send/v1-api#authorize_a_service_account_from_a_different_project)
отправку сообщений
сервисным аккаунтом из другого проекта.

1. Откройте [Google Cloud Console](https://console.cloud.google.com/) и выберите проект, к которому
   подключено ваше Android-приложение.

2. Перейдите в **IAM & Admin → IAM** и нажмите **Grant access**.

3. В поле **New principals** укажите сервисный аккаунт RetailCRM:

   ```text
   firebase-adminsdk-fbsvc@push-service-sender.iam.gserviceaccount.com
   ```

4. Назначьте роль **Firebase Cloud Messaging API Admin** (`roles/firebasecloudmessaging.admin`).

5. Нажмите **Save**.

Эта роль предоставляет право отправлять сообщения через FCM API вашего
проекта. [Описание роли в документации Firebase](https://firebase.google.com/docs/projects/iam/roles-predefined-product#cloud-messaging-roles).

## Подключение

В `build.gradle[.kts]` модуля приложения:

```kotlin
dependencies {
    implementation(platform("ru.retailcrm.sdk:sdk-bom:<version>"))
    implementation("ru.retailcrm.sdk:core-okhttp")
    implementation("ru.retailcrm.sdk:push-firebase")
}
```

Настройте Firebase в приложении: добавьте `google-services.json` и подключите плагин
`com.google.gms.google-services`
по [инструкции Firebase](https://firebase.google.com/docs/android/setup).

## Инициализация

При [инициализации SDK](sdk.md#инициализация) передайте настройки push-модуля:

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
                    channelId = "retailcrm_push",
                    channelName = "Уведомления",
                    channelDescription = "Уведомления RetailCRM",
                ),
                // Маленькая иконка уведомления — ресурс приложения.
                smallIconResId = R.drawable.ic_notification,
            ),
        ),
    )
}
```

Если приложение уже создало канал уведомлений, можно его использовать:

```kotlin
notificationChannel = NotificationChannelSetting.UseExisting(
    channelId = "existing_channel",
    smallIconResId = R.drawable.ic_notification,
)
```

На Android 13+ запросите разрешение `POST_NOTIFICATIONS`. Оно уже объявлено в manifest SDK; запрос
выполняется приложением
по [инструкции Android](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Регистрация устройства

После входа клиента в аккаунт задайте его идентификатор и зарегистрируйте устройство:

```kotlin
val sdk = CrmSdk.getInstance()

lifecycleScope.launch {
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(
            siteCustomerId = "customer-42",
            siteCode = "main",
        ),
    )

    // Подписаться на указанные категории.
    sdk.pushNotifications().registerDevice(
        categories = setOf("system", "news"),
    )
}
```

Для отказа от всех подписок передайте `null`:

```kotlin
lifecycleScope.launch {
    sdk.pushNotifications().registerDevice(categories = null)
}
```

SDK использует внутренний CRM ID либо пару `siteCustomerId + siteCode`, заданные через
`setCustomerIdentity()`. Если указаны оба способа, в запрос передаются оба. Без идентификации
клиента регистрация завершится ошибкой.

SDK самостоятельно получает Firebase installation ID (FID), а при его недоступности использует FCM
token.

### Плановая задержка при регистрации устройства

`registerDevice()` сохраняет данные на backend Онлайн-консультанта.

Backend передаёт зарегистрированные токены и категории в RetailCRM фоновой задачей в начале каждого
часа. Поэтому подписка или отписка обычно начинает действовать после следующей успешной
синхронизации — примерно в течение 60 минут после вызова `registerDevice()`. При ошибках передачи
задержка может быть больше.

## Собственный FirebaseMessagingService

По умолчанию SDK обрабатывает сообщения встроенным service. Если приложение использует собственный
`FirebaseMessagingService`, отключите встроенный в `build.gradle[.kts]`:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["crmPushFcmServiceEnabled"] = "false"
    }
}
```

В собственном service передавайте события SDK:

```kotlin
class MyFirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val handled = CrmSdk.getInstance()
            .firebasePushNotifications()
            .handleMessage(this, message)

        if (!handled) {
            // Обработка остальных push-уведомлений приложения.
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

## Переходы и данные уведомления

Следующие поля настраиваются в RetailCRM при конфигурации push-уведомления и передаются SDK в `data`
сообщения:

| Поле           | Назначение                                                   |
|----------------|--------------------------------------------------------------|
| `click_action` | Адрес перехода или значение `Intent.action`                  |
| `ca_<ключ>`    | Пара ключ/значение для передачи в extras запускаемого Intent |
| `image_url`    | URL изображения для раскрытого уведомления                   |
| `buttons`      | JSON-массив до двух кнопок с полями `title` и `value`        |

Поведение `click_action`:

- `"myapp://orders/123"` — SDK открывает URI через `ACTION_VIEW`.
- `"OPEN_ORDER"` — SDK запускает стартовую Activity приложения и передаёт ей
  `intent.action = "OPEN_ORDER"`.
- Поле отсутствует — SDK запускает стартовую Activity приложения с обычным Intent запуска.

Пары ключ/значение из полей `ca_*` передаются приложению через Intent extras. Префикс `ca_`
удаляется из ключа, значение передаётся без изменений. Например:

```json
{
    "click_action": "OPEN_ORDER",
    "ca_orderId": "123"
}
```

В Activity, открытой из уведомления:

```kotlin
// Значение click_action.
val action = intent.action // "OPEN_ORDER"

// Поле ca_orderId передано как extra с ключом orderId.
val orderId = intent.getStringExtra("orderId") // "123"
```

## Аналитика push-уведомлений

SDK автоматически отправляет статусы взаимодействия с уведомлением:

| Событие                               | Статус      |
|---------------------------------------|-------------|
| Получение уведомления                 | `delivered` |
| Нажатие на уведомление или его кнопку | `clicked`   |
| Смахивание уведомления                | `swiped`    |
