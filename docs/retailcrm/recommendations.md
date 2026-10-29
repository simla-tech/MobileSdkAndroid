# Товарные рекомендации

Модуль `recommendations` получает товарные рекомендации RetailCRM и передаёт события показа блока и
нажатия на товар. Отображение товаров и переходы реализует приложение.

## Оглавление

- [Подключение](#подключение)
- [Получение рекомендаций](#получение-рекомендаций)
- [Результат запроса](#результат-запроса)
- [Учёт взаимодействий](#учёт-взаимодействий)
- [Обработка ошибок](#обработка-ошибок)

## Подключение

Добавьте модуль в зависимости приложения:

```kotlin
dependencies {
    implementation(platform("ru.retailcrm.sdk:sdk-bom:<version>"))
    implementation("ru.retailcrm.sdk:recommendations")
}
```

[Инициализируйте SDK](./sdk.md#инициализация), затем получите объект, реализующий API
рекомендаций:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.recommendations.recommendations

val recommendations = CrmSdk.getInstance().recommendations()
```

## Получение рекомендаций

Для получения товаров нужна активная механика рекомендаций в RetailCRM. Требования к данным клиента
и товара определяются настройками механики.

SDK автоматически использует идентификатор клиента, переданный через `setCustomerIdentity()`.
Подробнее — в разделе [«Идентификация клиента»](./sdk.md#идентификация-клиента).

```kotlin
import com.simla.android.sdk.recommendations.CrmRecommendationRequest

lifecycleScope.launch {
    val result = recommendations.getProducts(
        request = CrmRecommendationRequest(
            // Обязательный символьный код активной механики рекомендаций.
            mechanicCode = "mobile-home",

            // Обязательный непустой код блока рекомендаций в приложении.
            blockCode = "product_similar",

            // Необязательный внутренний ID товара в RetailCRM, больше нуля.
            // Вместо него можно передать productExternalId = "product-101".
            // Не передавайте оба идентификатора одновременно.
            productId = 101,

            // Для механики, работающей с торговым предложением,
            // вместо товара передайте один из параметров:
            // offerId = 1001 — внутренний ID предложения, больше нуля;
            // offerExternalId = "offer-1001" — внешний ID предложения.
            // Контексты товара и предложения взаимоисключающие.
        ),
    )

    // Отображение result.products реализует приложение.
}
```

## Результат запроса

`getProducts()` возвращает `CrmRecommendationResult`:

```kotlin
// ID выполнения механики. Используется для связи событий с рекомендациями.
val executionId = result.executionId

// Признак персонализации результата.
val personalized = result.personalized

// Тип механики и список требуемых ею данных.
val mechanicType = result.context.mechanicType
val required = result.context.required

// Идентификаторы рекомендованных товаров.
val productIds = result.productIds

// Данные рекомендованных торговых предложений.
// Список может быть пустым.
val products = result.products

products.forEach { product ->
    // ID торгового предложения.
    val offerId = product.id

    // ID товара. Передаётся в trackClick().
    val productId = product.productId

    val name = product.name
    val price = product.price

    // По умолчанию 0.0.
    val oldPrice = product.oldPrice

    val currency = product.currency

    // По умолчанию пустая строка.
    val imageUrl = product.imageUrl
    val url = product.url

    // Признак доступности; по умолчанию true.
    val available = product.available
}
```

## Учёт взаимодействий

Приложение самостоятельно сообщает о показах и нажатиях. Получение товаров через `getProducts()` не
отправляет событие показа.

### Показ блока

После отображения блока вызовите `trackImpression()`:

```kotlin
lifecycleScope.launch {
    recommendations.trackImpression(
        // Результат запроса, товары которого отображены в блоке.
        recommendation = result,

        // Обязательный абсолютный URI экрана со схемой.
        // Например: app://home или myapp://products/101.
        // Относительный путь /home не подходит.
        screenUri = "app://products/101",

        // Необязательное название экрана.
        title = "Карточка товара",
    )
}
```

Метод передаёт событие просмотра экрана с ID выполнения механики. Трекер также использует URI и
название этого экрана для следующих событий.

### Нажатие на товар

При выборе рекомендованного товара вызовите `trackClick()`:

```kotlin
lifecycleScope.launch {
    recommendations.trackClick(
        // Результат запроса, из которого выбран товар.
        recommendation = result,

        // Обязательный ID товара, больше нуля.
        // Используйте product.productId из result.products.
        productId = product.productId,

        // Необязательная позиция товара в отображаемом списке.
        // Передавайте индекс с нуля.
        position = index,

        // Необязательное обозначение элемента приложения.
        element = "recommendations#product_similar",

        // Необязательный признак перехода с уходом.
        // По умолчанию false.
        leaving = true,
    )

    // Переход к выбранному товару реализует приложение.
}
```

Метод передаёт событие нажатия с ID выполнения механики, ID товара и указанной позицией.

События рекомендаций используют настройки трекера,
включая [согласие на трекинг](./tracker.md#согласие-на-трекинг)
и [очередь доставки](./tracker.md#очередь-и-доставка). Завершение `trackImpression()` или
`trackClick()` не подтверждает доставку на сервер.

Слушатель, установленный через `tracker.setEventUpdateListener()`, получает обновления и для событий
рекомендаций: добавление в очередь, отправку, доставку, повторную отправку, отклонение и удаление.
Показ блока представлен видом `PAGE_VIEW`, нажатие на товар — `CLICK`. Обновление `Delivered`
подтверждает приём события сервером, но не завершение обработки в RetailCRM. Подробнее — в
разделе [«Диагностика»](./tracker.md#диагностика).

## Обработка ошибок

При некорректных параметрах SDK выбрасывает `IllegalArgumentException` до отправки запроса. При
неуспешном HTTP-ответе — `CrmRecommendationsHttpException`.

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

        // Отображение result.products.
    } catch (error: CancellationException) {
        throw error
    } catch (error: CrmRecommendationsHttpException) {
        // HTTP-код ответа сервера.
        val httpCode = error.httpCode

        // Сообщения из поля errors ответа; список может быть пустым.
        val errors = error.errors

        // Исходное тело ответа сервера.
        val responseBody = error.responseBody

        // Обработка ошибки в приложении.
    } catch (error: IllegalArgumentException) {
        // Исправьте параметры запроса.
    } catch (error: Exception) {
        // Обработка сетевой ошибки или ошибки чтения ответа.
    }
}
```
