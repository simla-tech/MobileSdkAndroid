# Трекер

Модуль `tracker` передаёт в RetailCRM события из Android-приложения: просмотры экранов и товаров,
нажатия, регистрацию и вход клиента, изменения корзины и оформление заказа.

## Оглавление

- [Подключение](#подключение)
- [Идентификация клиента](#идентификация-клиента)
- [Согласие на трекинг](#согласие-на-трекинг)
- [Отправка событий](#отправка-событий)
    - [Типы событий](#типы-событий)
        - [Просмотры экранов и товаров](#просмотры-экранов-и-товаров)
        - [Регистрация](#регистрация)
        - [Вход в аккаунт](#вход-в-аккаунт)
        - [Обращение за консультацией](#обращение-за-консультацией)
        - [Открытие корзины](#открытие-корзины)
        - [Состав корзины](#состав-корзины)
        - [Оформление заказа](#оформление-заказа)
        - [Подписки](#подписки)
        - [Нажатие на элемент](#нажатие-на-элемент)
        - [Переход к контакту](#переход-к-контакту)
        - [Начало заполнения формы](#начало-заполнения-формы)
        - [Отправка формы](#отправка-формы)
        - [Скачивание файла](#скачивание-файла)
        - [Изменение избранного](#изменение-избранного)
    - [Настраиваемые события](#настраиваемые-события)
    - [Очередь и доставка](#очередь-и-доставка)
    - [Диагностика](#диагностика)

## Подключение

Добавьте модуль в зависимости приложения:

```kotlin
dependencies {
    implementation(platform("ru.retailcrm.sdk:sdk-bom:<version>"))
    implementation("ru.retailcrm.sdk:tracker")
}
```

[Инициализируйте SDK](./sdk.md#инициализация), затем получите трекер:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.tracker.tracker

val tracker = CrmSdk.getInstance().tracker()
```

## Идентификация клиента

Сообщайте SDK о входе, смене аккаунта и выходе клиента через `sdk.setCustomerIdentity(...)`.
Подробнее — в разделе [«Идентификация клиента»](./sdk.md#идентификация-клиента).

Трекер подставляет заданные в SDK `crmCustomerId` и `siteCustomerId` в `system_customer_id` и
`system_customer_external_id`. Вызовы `trackLogin()` и `trackRegister()` передают события и не
заменяют `setCustomerIdentity()`.

Очереди событий хранятся отдельно для каждого сочетания токена сайта и токена пользователя SDK.

## Согласие на трекинг

Если настройки CRM-системы требуют согласия на трекинг, передайте обработчик:

```kotlin
val tracker = CrmSdk.getInstance().tracker(
    consentHandler = TrackerConsentHandler {
        // myConsentRepository — компонент вашего приложения.
        // Получает сохранённое решение или запрашивает его у пользователя.
        // Возвращает TrackerConsentState:
        // GRANTED — согласие получено;
        // DENIED — пользователь отказался;
        // UNKNOWN — решение ещё не получено.
        myConsentRepository.getConsent()
    },
)
```

SDK вызывает обработчик, когда сервер разрешает трекинг и требует согласия. Интерфейс запроса
и сохранение решения реализует приложение.

Если согласие обязательно, события отправляются только после получения `GRANTED`. До завершения
подключения или получения решения события временно буферизуются в памяти. После `GRANTED` они
переносятся в постоянную очередь.

Обработчик можно установить или заменить после получения трекера:

```kotlin
tracker.setConsentHandler(
    TrackerConsentHandler {
        myConsentRepository.getConsent()
    },
)
```

При получении `DENIED` текущая очередь и временный буфер очищаются.

## Отправка событий

Приложение самостоятельно вызывает методы трекера при соответствующих действиях клиента.
Трекинг должен быть включён в настройках сайта RetailCRM. Если сервер отключил трекинг,
события не добавляются в очередь.

### Типы событий

#### Просмотры экранов и товаров

При открытии экрана вызовите `trackPageView()`:

```kotlin
tracker.trackPageView(
    // Абсолютный URI со схемой.
    // Например: app://catalog или myapp://products/42.
    // Относительный путь /catalog не подходит.
    screenUri = "app://products/42",

    // Необязательное название экрана.
    title = "Кроссовки",

    // Необязательные данные просматриваемого товара.
    // Если передаёте product, заполните хотя бы одно поле:
    // offer, productId или непустой brand.
    product = TrackerViewedProduct(
        // Идентификатор торгового предложения:
        // SystemId(42) — внутренний ID в RetailCRM;
        // ExternalId("offer-42") — внешний ID;
        // XmlId("offer-42") — XML ID.
        offer = TrackerOfferIdentifier.ExternalId("offer-42"),

        // Необязательный внутренний ID товара в RetailCRM.
        productId = 42,

        // Необязательный бренд.
        brand = "Brand",
    ),
)
```

После вызова трекер использует URI и название экрана для следующих событий. URI предыдущего
экрана передаётся как источник перехода (`referrer_url`).

#### Регистрация

```kotlin
tracker.trackRegister(
    // Передайте хотя бы один непустой атрибут клиента или подписку.
    customer = TrackerCustomer(
        // Необязательные данные клиента.
        firstName = "Иван",
        lastName = "Иванов",
        patronymic = "Иванович",
        email = "ivan@example.com",
        phone = "+79990000000",

        // Необязательный список подписок; по умолчанию пустой.
        subscriptions = listOf(
            TrackerSubscription(
                // Доступные каналы: EMAIL, SMS, WABA.
                channel = TrackerSubscriptionChannel.EMAIL,
                // Непустой адрес подписки: email или номер телефона.
                address = "ivan@example.com",
                // true — подписка активна, false — отказ от подписки.
                active = true,
            ),
        ),
    ),
)
```

#### Вход в аккаунт

```kotlin
tracker.trackLogin(
    // Обязательный непустой email клиента.
    customerEmail = "ivan@example.com",

    // Необязательный список подписок; по умолчанию пустой.
    subscriptions = listOf(
        TrackerSubscription(
            channel = TrackerSubscriptionChannel.EMAIL,
            address = "ivan@example.com",
            active = true,
        ),
    ),
)
```

#### Обращение за консультацией

```kotlin
tracker.trackConsult(
    // Обязательный непустой email клиента.
    customerEmail = "ivan@example.com",

    // Необязательный список подписок; по умолчанию пустой.
    subscriptions = listOf(
        TrackerSubscription(
            channel = TrackerSubscriptionChannel.EMAIL,
            address = "ivan@example.com",
            active = true,
        ),
    ),
)
```

#### Открытие корзины

```kotlin
tracker.trackOpenCart(
    // Необязательный непустой email клиента.
    customerEmail = "ivan@example.com",
)
```

#### Состав корзины

```kotlin
val items = listOf(
    TrackerCartItem(
        // Идентификатор торгового предложения.
        offer = TrackerOfferIdentifier.ExternalId("offer-42"),

        // Цена за единицу: конечное неотрицательное число.
        price = 4990.0,

        // Количество: целое число больше нуля.
        quantity = 2,
    ),
)

tracker.trackCart(
    // Обязательный непустой идентификатор корзины.
    cartId = "cart-123",

    // Необязательный список позиций; по умолчанию пустой.
    items = items,
)
```

#### Оформление заказа

```kotlin
tracker.trackOrder(
    // Предпочтительно передавать внутренний ID заказа в CRM (1..2147483647).
    // Для внешнего ID или номера заказа используйте соответственно:
    // TrackerOrderIdentifier.ExternalId("order-123")
    // TrackerOrderIdentifier.Number("2026-123").
    identifier = TrackerOrderIdentifier.SystemId(123),

    // Необязательный непустой идентификатор корзины.
    cartId = "cart-123",

    // Необязательный список позиций; по умолчанию пустой.
    items = items,

    // Необязательная стоимость доставки:
    // конечное неотрицательное число.
    deliveryPrice = 300.0,

    // Необязательная итоговая сумма:
    // конечное неотрицательное число.
    total = 10280.0,
)
```

`SystemId` передаётся как `order_id` и используется для назначения источника заказу. Если внутренний
ID недоступен, можно передать `ExternalId`; `Number` сохраняет событие, но не идентифицирует заказ
при выгрузке источников в CRM.

#### Подписки

При изменении подписки вызовите `trackSubscription()`:

```kotlin
tracker.trackSubscription(
    subscription = TrackerSubscription(
        // Доступные каналы: EMAIL, SMS, WABA.
        channel = TrackerSubscriptionChannel.EMAIL,

        // Непустой адрес подписки: email или номер телефона.
        address = "ivan@example.com",

        // true — подписка активна, false — отказ от подписки.
        active = true,
    ),
)
```

#### Нажатие на элемент

```kotlin
tracker.trackClick(
    // Непустое строковое обозначение элемента приложения.
    element = "checkout_button",

    // Необязательный признак перехода с уходом.
    // По умолчанию false.
    leaving = false,
)
```

#### Переход к контакту

```kotlin
tracker.trackContact(
    // Доступные каналы: TELEGRAM, WHATSAPP.
    channel = TrackerContactChannel.TELEGRAM,
)
```

#### Начало заполнения формы

```kotlin
tracker.trackFormStart(
    // Непустое строковое обозначение формы приложения.
    formSelector = "feedback_form",
)
```

#### Отправка формы

```kotlin
tracker.trackFormSubmit(
    // Непустое строковое обозначение формы приложения.
    formSelector = "feedback_form",
)
```

#### Скачивание файла

```kotlin
tracker.trackFileDownload(
    // Обязательное непустое имя файла.
    fileName = "catalog.pdf",

    // Необязательное непустое обозначение элемента приложения.
    element = "download_catalog_button",
)
```

#### Изменение избранного

```kotlin
tracker.trackFavoriteProduct(
    // Идентификатор торгового предложения.
    offer = TrackerOfferIdentifier.ExternalId("offer-42"),

    // false — добавление в избранное, true — удаление.
    // По умолчанию false.
    removed = false,
)
```

### Настраиваемые события

Все методы `track*()` принимают необязательный параметр `code`. Его значение берётся
из настроек события в административной панели RetailCRM.

```kotlin
tracker.trackClick(
    element = "checkout_button",

    // Необязательный непустой код из настроек события в RetailCRM.
    code = "checkout_click",
)
```

### Очередь и доставка

Трекер сохраняет принятые события в локальной базе SQLite и автоматически отправляет их:

- каждые 15 секунд, пока трекинг активен;
- при накоплении 50 событий.

Один запрос содержит до 50 событий. Очередь вмещает до 1000 событий; при переполнении
удаляются самые старые.

При сетевой ошибке события остаются в очереди для повторной отправки. При ответе HTTP `429`
трекер учитывает указанное сервером время ожидания. События, окончательно отклонённые сервером,
удаляются из очереди.

Для немедленной попытки отправить один пакет вызовите:

```kotlin
tracker.flush()
```

`flush()` не гарантирует отправку всей очереди. Если трекинг отключён или действует ограничение
запросов, отправка не выполняется.

События, вызванные до активации трекинга, не добавляются в очередь. Завершение `track*()`
само по себе не подтверждает доставку события на сервер.

### Диагностика

Для наблюдения за обработкой событий установите слушатель:

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

Доступные обновления:

| Обновление       | Значение                                                            |
|------------------|---------------------------------------------------------------------|
| `Queued`         | События добавлены в очередь                                         |
| `Sending`        | Начинается попытка отправки пакета; `payload` содержит JSON запроса |
| `Delivered`      | Сервер принял события                                               |
| `RetryScheduled` | События оставлены для повторной отправки                            |
| `Rejected`       | Событие окончательно отклонено сервером                             |
| `Dropped`        | События удалены при очистке или переполнении очереди                |

Каждое обновление содержит ссылки на события с их ID и видом. `Delivered` подтверждает приём
сервером, но не завершение последующей обработки в RetailCRM.

Слушатель получает только новые обновления. Обработчик может вызываться вне главного потока:
он должен быстро завершаться и не вызывать трекер синхронно. `Sending.payload` может содержать
персональные данные.

Для удаления слушателя:

```kotlin
tracker.setEventUpdateListener(null)
```
