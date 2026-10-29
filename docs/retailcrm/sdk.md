# RetailCRM Android SDK

SDK позволяет подключить к Android-приложению онлайн-консультант, трекинг действий клиентов,
товарные рекомендации и push-уведомления.

## Модули

- `sdk:core` — общий runtime и точка входа `CrmSdk`;
- `sdk:core-okhttp` — стандартный транспорт OkHttp;
- `sdk:consultant` — онлайн-консультант;
- `sdk:tracker` — трекинг действий клиентов;
- `sdk:recommendations` — товарные рекомендации;
- `sdk:push-firebase` — Firebase push-уведомления;
- `sdk:bom` — согласованные версии артефактов SDK.

## Требования

- Для использования онлайн-консультанта в RetailCRM должен быть подключён модуль
  «Онлайн-консультант».
- Android 7.0 (API 24) и выше.
- Android `compileSdk 36`.
- SDK скомпилирован для JVM 17 и Kotlin 2.0.21.

## Подключение

Добавьте Maven Central в `settings.gradle[.kts]`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

В `build.gradle[.kts]` модуля приложения подключите необходимые зависимости. Зависимость делает API
модуля доступным приложению, а запуск модуля настраивается отдельно в `CrmSdk.init(...)`:

```kotlin
dependencies {
    implementation(platform("ru.retailcrm.sdk:sdk-bom:<version>"))

    // Оставьте только необходимые модули.
    // Общий модуль core подключается автоматически.
    implementation("ru.retailcrm.sdk:core-okhttp")     // Стандартный транспорт OkHttp
    implementation("ru.retailcrm.sdk:consultant")      // Онлайн-консультант
    implementation("ru.retailcrm.sdk:tracker")         // Трекинг
    implementation("ru.retailcrm.sdk:recommendations") // Рекомендации
    implementation("ru.retailcrm.sdk:push-firebase")   // Push-уведомления
}
```

## Инициализация

Инициализируйте SDK в `Application.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = CrmSdkConfig(
            // Обязательный токен сайта из настроек
            // онлайн-консультанта или трекера RetailCRM.
            siteToken = "SITE_TOKEN",

            // Сервер по умолчанию — https://c.retailcrm.tech.
            // Для другого сервера:
            // baseUrl = "https://your-server.example",
        )

        CrmSdk.init(
            context = this,
            config = config,

            // Стандартный транспорт OkHttp.
            networkEngine = SdkOkHttpNetworkEngine.create(),

            // Для собственного транспорта подключите core без core-okhttp
            // и передайте свою реализацию SdkNetworkEngine вместо строки выше:
            // networkEngine = CustomSdkNetworkEngine(),

            // Необязательные URI и название начального экрана.
            pageContext = SessionPageContext(
                url = "app://home",
                title = "Главная",
            ),
        ) {
            // Установить и запустить нужные модули вместе с общей сессией SDK.
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

## Идентификация клиента

После входа передайте доступные идентификаторы клиента. Поддерживаются три варианта:

```kotlin
val sdk = CrmSdk.getInstance()

lifecycleScope.launch {
    // Внутренний ID клиента RetailCRM.
    sdk.setCustomerIdentity(CrmCustomerIdentity(crmCustomerId = 37))

    // ID клиента на стороне сайта и код сайта RetailCRM.
    // siteCustomerId и siteCode передаются только вместе.
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(siteCustomerId = "customer-42", siteCode = "main"),
    )

    // Оба способа идентификации.
    // Если модулю достаточно одного идентификатора,
    // SDK использует crmCustomerId в первую очередь.
    sdk.setCustomerIdentity(
        CrmCustomerIdentity(
            crmCustomerId = 37,
            siteCustomerId = "customer-42",
            siteCode = "main",
        ),
    )
}
```

При выходе из аккаунта передайте `null`:

```kotlin
lifecycleScope.launch {
    // Переключает сессию на анонимного пользователя.
    sdk.setCustomerIdentity(null)
}
```

## Использование модулей

После инициализации экземпляр SDK доступен через:

```kotlin
val sdk = CrmSdk.getInstance()
```

Методы доступа возвращают API модулей. Для consultant, tracker и recommendations первый вызов также
лениво установит модуль, если он не был выбран в `init()`. Push-модуль требует конфигурацию и должен
быть установлен в `init()`:

```kotlin
sdk.onlineConsultant() // Требуется модуль consultant.
sdk.tracker()         // Требуется модуль tracker.
sdk.recommendations() // Требуется модуль recommendations.

// Требуется модуль push-firebase и PushNotificationsConfig при инициализации SDK.
sdk.pushNotifications()
```

Подробнее о модулях:

- [Онлайн-консультант](./consultant.md) — отображение чата.
- [Трекер](./tracker.md) — отправка событий.
- [Товарные рекомендации](./recommendations.md) — получение рекомендованных товаров и учёт
  взаимодействий.
- [Push-уведомления](./push-firebase.md) — настройка Firebase и регистрация категорий.

## Проверка проекта

```shell
./gradlew sdkTest apiCheckAll
./gradlew :sdk:core:assembleRetailcrmRelease :sdk:consultant:assembleRetailcrmRelease \
  :sdk:tracker:assembleRetailcrmRelease :sdk:recommendations:assembleRetailcrmRelease \
  :sdk:push-firebase:assembleRetailcrmRelease :sdk:bom:build
```

`apiCheckAll` сравнивает текущий публичный API модулей с сохранёнными `*.api` baseline-файлами и
обнаруживает непреднамеренные изменения публичного контракта. Если публичный API изменён намеренно,
`apiDumpAll` обновляет baseline-файлы. Получившиеся изменения необходимо проверить перед добавлением
в pull request.

## Участие в разработке

Перед созданием pull request:

1. Опишите проблему или предлагаемое изменение в issue.
2. Добавьте тесты для изменённого поведения.
3. Выполните `./gradlew sdkTest apiCheckAll`.
4. Если публичный API изменён намеренно, выполните `./gradlew apiDumpAll`, проверьте обновлённые
   API baseline-файлы и включите их в pull request.

Не добавляйте в репозиторий токены, ключи, `local.properties`, `google-services.json` и другие
секреты.

## Безопасность

Не публикуйте сведения об уязвимостях в открытых issue.

Используйте GitHub Private Vulnerability Reporting: **Security → Report a vulnerability**. Если
форма недоступна, сначала свяжитесь с сопровождающими проекта по непубличному каналу.

## Лицензия

Проект распространяется по лицензии Apache License 2.0. См. [LICENSE](../../LICENSE).
