# Онлайн-консультант

Модуль `consultant` добавляет готовый экран чата для общения клиента с менеджерами CRM-системы,
зарегистрированной в RetailCRM. Экран доступен для Jetpack Compose и Android View.

## Оглавление

- [Подключение](#подключение)
- [Jetpack Compose](#jetpack-compose)
- [Android View](#android-view)
    - [Fragment](#fragment)
    - [Activity](#activity)
- [Идентификация клиента](#идентификация-клиента)
- [События в реальном времени](#события-в-реальном-времени)
- [Непрочитанные сообщения](#непрочитанные-сообщения)
- [Локализация](#локализация)
- [Возможности чата](#возможности-чата)

## Подключение

Добавьте модуль в зависимости приложения:

```kotlin
dependencies {
    implementation(platform("ru.retailcrm.sdk:sdk-bom:<version>"))
    implementation("ru.retailcrm.sdk:consultant")
}
```

Перед отображением чата [инициализируйте SDK](./sdk.md#инициализация).

## Jetpack Compose

Разместите `OnlineConsultantChatScreen` на экране приложения:

```kotlin
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.ui.OnlineConsultantChatScreen

@Composable
fun ConsultantScreen() {
    OnlineConsultantChatScreen(
        // Инициализированный экземпляр SDK.
        sdk = CrmSdk.getInstance(),

        // Необязательный Modifier для размеров и размещения экрана.
        modifier = Modifier.fillMaxSize(),

        // Необязательный обработчик ошибок подключения,
        // отправки сообщений и загрузки истории.
        // По умолчанию пустой.
        onError = { error ->
            Log.e("Consultant", "Ошибка чата", error)
        },
    )
}
```

## Android View

Добавьте `OnlineConsultantView` в XML-разметку:

```xml

<com.simla.android.sdk.consultant.ui.OnlineConsultantView android:id="@+id/consultantView"
    android:layout_width="match_parent" android:layout_height="match_parent" />
```

### Fragment

Подключите SDK в переопределённом `onViewCreated()`:

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

            // Подписка на состояние чата отменяется
            // при уничтожении View фрагмента.
            scope = viewLifecycleOwner.lifecycleScope,

            // Реестр Activity для системного выбора файлов.
            activityResultRegistry =
                requireActivity().activityResultRegistry,

            // Выбор файлов привязан к жизненному циклу View.
            // Вызов bind() должен выполняться до состояния STARTED.
            lifecycleOwner = viewLifecycleOwner,

            // Непустой ключ регистрации выбора файлов.
            // Должен сохраняться при пересоздании экрана
            // и быть уникальным в пределах реестра Activity.
            activityResultKey = "consultant_fragment_files",

            // Необязательный обработчик ошибок чата
            // и запуска системного выбора файлов.
            onError = { error ->
                Log.e("Consultant", "Ошибка чата", error)
            },
        )
    }
}
```

### Activity

Подключите SDK в `onCreate()` после установки разметки:

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

            // Подписка на состояние чата отменяется
            // при уничтожении Activity.
            scope = lifecycleScope,

            // Реестр Activity для системного выбора файлов.
            activityResultRegistry = this.activityResultRegistry,

            // Выбор файлов привязан к жизненному циклу Activity.
            // Вызов bind() должен выполняться до состояния STARTED.
            lifecycleOwner = this,

            // Непустой ключ, уникальный в пределах реестра Activity.
            // Используйте тот же ключ после пересоздания Activity.
            activityResultKey = "consultant_activity_files",

            // Необязательный обработчик ошибок чата
            // и запуска системного выбора файлов.
            onError = { error ->
                Log.e("Consultant", "Ошибка чата", error)
            },
        )
    }
}
```

## Идентификация клиента

Приложение должно сообщать SDK о входе, смене аккаунта и выходе клиента через
`sdk.setCustomerIdentity(...)`. SDK не отслеживает авторизацию в приложении самостоятельно.
Подробнее — в разделе [«Идентификация клиента»](./sdk.md#идентификация-клиента).

При передаче нового идентификатора SDK отключает текущую сессию и автоматически подключает
сессию указанного клиента. Отдельно переподключать чат не требуется.

При выходе из аккаунта вызовите `sdk.setCustomerIdentity(null)`: этот вызов сообщает SDK о выходе
и переключает сессию на анонимного пользователя.

SDK сохраняет токен пользователя консультанта отдельно для каждого identity. В `POST /connect`
передаётся только `siteCustomerId` как `X-Site-Customer-Id`; внутренний `crmCustomerId` не подменяет
этот
заголовок. При идентификации только по CRM ID переписка восстанавливается по сохранённому SDK-токену
на текущей установке.

## События в реальном времени

При инициализации онлайн-консультанта SDK автоматически подписывается на события через WebSocket.
Благодаря этому новые сообщения и изменения состояния чата отображаются без дополнительных
действий со стороны приложения.

Подписка учитывает жизненный цикл приложения: при переходе в фоновый режим SDK отключается от
WebSocket, а после возвращения приложения на передний план автоматически подключается и
подписывается на события повторно.

## Непрочитанные сообщения

Количество непрочитанных сообщений доступно через `StateFlow<Int>`:

```kotlin
val consultant = CrmSdk.getInstance().onlineConsultant()
val unreadCount = consultant.unreadCount
```

Например, в Compose:

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

Готовый экран чата автоматически отмечает видимые входящие сообщения прочитанными. Если счётчик
отключён в настройках онлайн-консультанта RetailCRM, `unreadCount` возвращает `0`.

## Локализация

SDK содержит английские строки по умолчанию, а также русский и испанский переводы. Язык
определяется конфигурацией ресурсов приложения.

Чтобы изменить текст, переопределите публичный строковый ресурс `crm_consultant_*` в приложении:

```xml
<!-- res/values-ru/strings.xml -->
<resources>
    <string name="crm_consultant_message">Напишите сообщение</string>
    <string name="crm_consultant_send_message">Отправить</string>
</resources>
```

При переопределении строк с параметрами сохраняйте их формат, например `%1$s` или `%1$d`.

## Возможности чата

Экран автоматически загружает историю, получает новые сообщения и отправляет текст. В Compose
и приведённых примерах Android View кнопка вложения открывает системный выбор файлов и позволяет
отправить несколько файлов.

Готовый интерфейс поддерживает:

- текстовые сообщения, ссылки и цитаты;
- изображения и файловые вложения;
- карточки товаров и заказов;
- быстрые ответы;
- формы сбора контактных данных;
- форму обращения, когда консультант находится офлайн;
- отображение состояния отправки сообщений;
- загрузку более ранних сообщений при прокрутке истории.

Контактные формы, режим офлайн и оформление чата применяются по настройкам онлайн-консультанта,
полученным от сервера.

SDK автоматически поддерживает WebSocket-соединение, пока приложение находится на переднем плане,
и восстанавливает его после возвращения из фона.
