package com.simla.android.sdk.consultant.ui

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.consultantController
import com.simla.android.sdk.consultant.controller.OnlineConsultantController
import com.simla.android.sdk.consultant.controller.OnlineConsultantControllerEvent
import com.simla.android.sdk.consultant.controller.OnlineConsultantControllerState
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.storage.ConsultantSharedPreferencesDownloadedFileRegistry
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.debugMessageIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OnlineConsultantView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var messages by mutableStateOf<List<OnlineConsultantMessageUiModel>>(emptyList())
    private val messageListState = OnlineConsultantMessageListState(LazyListState())
    private var dayLabelState by mutableStateOf(context.getString(R.string.crm_consultant_today))
    private var inputTextState by mutableStateOf("")
    private var isInitialLoadingState by mutableStateOf(false)
    private var isLoadingOlderMessagesState by mutableStateOf(false)
    private var canLoadOlderMessagesState by mutableStateOf(false)
    private var isOfflineState by mutableStateOf(false)
    private var isAvailableState by mutableStateOf(true)
    private var widgetConfigState by mutableStateOf<ConsultantWidgetConfig?>(null)
    private var onAttachClick: () -> Unit by mutableStateOf({})
    private var filePicker: ActivityResultLauncher<Array<String>>? = null
    private var filePickerOwner: LifecycleOwner? = null
    private var filePickerObserver: LifecycleEventObserver? = null
    private var onLoadOlderMessages: () -> Unit by mutableStateOf({})
    private var onSendClick: (String) -> Unit by mutableStateOf({})
    private var onQuickResponseClick: (String) -> Unit by mutableStateOf({})
    private var onContactFormSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit by
    mutableStateOf({ _, _ -> })
    private var onMessageVisible: (OnlineConsultantMessageUiModel) -> Unit by mutableStateOf({})
    private var downloadedFileRegistryState by mutableStateOf<ConsultantDownloadedFileRegistry>(
        ConsultantSharedPreferencesDownloadedFileRegistry(context.applicationContext),
    )

    private val composeView = ComposeView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            OnlineConsultantScreen(
                dayLabel = dayLabelState,
                messages = messages,
                inputText = inputTextState,
                onInputTextChange = { inputTextState = it },
                onAttachClick = { onAttachClick() },
                downloadedFileRegistry = downloadedFileRegistryState,
                isInitialLoading = isInitialLoadingState,
                isLoadingOlderMessages = isLoadingOlderMessagesState,
                canLoadOlderMessages = canLoadOlderMessagesState,
                widgetConfig = widgetConfigState,
                isOffline = isOfflineState,
                isAvailable = isAvailableState,
                onLoadOlderMessages = { onLoadOlderMessages() },
                onContactFormSubmit = { kind, data -> onContactFormSubmit(kind, data) },
                onMessageVisible = { message -> onMessageVisible(message) },
                onQuickResponseClick = { title -> onQuickResponseClick(title) },
                onSendClick = {
                    val current = inputTextState
                    onSendClick(current)
                    inputTextState = ""
                },
                messageListState = messageListState,
            )
        }
    }

    init {
        addView(composeView)
    }

    internal fun setDayLabel(value: String) {
        dayLabelState = value
    }

    internal fun setMessages(value: List<OnlineConsultantMessageUiModel>) {
        ConsultantUiDebugLog.d(
            "OnlineConsultantView.setMessages incoming size=${value.size} ids=${value.debugMessageIds()} " +
                    "current size=${messages.size} ids=${messages.debugMessageIds()}",
        )
        if (messages == value) {
            ConsultantUiDebugLog.d("OnlineConsultantView.setMessages skipped: lists are equal")
            return
        }

        messages = value.toList()
        ConsultantUiDebugLog.d(
            "OnlineConsultantView.setMessages result size=${messages.size} ids=${messages.debugMessageIds()}",
        )
    }

    internal fun setInputText(value: String) {
        inputTextState = value
    }

    internal fun setInitialLoading(value: Boolean) {
        isInitialLoadingState = value
    }

    internal fun setLoadingOlderMessages(value: Boolean) {
        isLoadingOlderMessagesState = value
    }

    internal fun setCanLoadOlderMessages(value: Boolean) {
        canLoadOlderMessagesState = value
    }

    internal fun setOffline(value: Boolean) {
        isOfflineState = value
    }

    internal fun setAvailable(value: Boolean) {
        isAvailableState = value
    }

    internal fun setWidgetConfig(value: ConsultantWidgetConfig?) {
        widgetConfigState = value
    }

    internal fun setOnAttachClickListener(listener: () -> Unit) {
        onAttachClick = listener
    }

    internal fun setOnLoadOlderMessagesListener(listener: () -> Unit) {
        onLoadOlderMessages = listener
    }

    internal fun setOnContactFormSubmitListener(
        listener: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit,
    ) {
        onContactFormSubmit = listener
    }

    internal fun setOnMessageVisibleListener(listener: (OnlineConsultantMessageUiModel) -> Unit) {
        onMessageVisible = listener
    }

    internal fun setDownloadedFileRegistry(registry: ConsultantDownloadedFileRegistry) {
        downloadedFileRegistryState = registry
    }

    internal fun setOnSendClickListener(listener: (String) -> Unit) {
        onSendClick = listener
    }

    internal fun setOnQuickResponseClickListener(listener: (String) -> Unit) {
        onQuickResponseClick = listener
    }

    internal fun bind(
        controller: OnlineConsultantController,
        scope: CoroutineScope,
        onAttachClick: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        clearFilePicker()
        setOnAttachClickListener(onAttachClick)
        setDownloadedFileRegistry(controller.downloadedFileRegistry)
        setOnSendClickListener { text ->
            val messageText = text.trim()
            if (messageText.isNotEmpty()) {
                controller.sendMessage(messageText)
            }
        }
        setOnQuickResponseClickListener { title ->
            controller.sendMessage(title)
        }
        setOnLoadOlderMessagesListener {
            controller.loadOlderMessages()
        }
        setOnContactFormSubmitListener { kind, data ->
            controller.submitContactForm(kind, data)
        }
        setOnMessageVisibleListener { message ->
            controller.markMessageVisible(message.id)
        }

        controller.start()

        scope.launch {
            controller.state.collect { state ->
                val readyState = state as? OnlineConsultantControllerState.Ready
                ConsultantUiDebugLog.d(
                    "OnlineConsultantView.controller.state collected state=${state::class.simpleName} " +
                            "messages=${readyState?.messages?.size ?: 0} " +
                            "ids=${readyState?.messages.orEmpty().debugMessageIds()}",
                )
                setMessages(readyState?.messages.orEmpty())
                setInitialLoading(
                    state is OnlineConsultantControllerState.Idle ||
                            state is OnlineConsultantControllerState.InitialLoading ||
                            readyState?.isInitialLoading == true,
                )
                setLoadingOlderMessages(readyState?.isLoadingOlderMessages == true)
                setCanLoadOlderMessages(readyState?.canLoadOlderMessages == true)
                setWidgetConfig(readyState?.widgetConfig ?: controller.sdk.widgetConfigState.value)
                setOffline(readyState?.isOffline == true)
                setAvailable(state !is OnlineConsultantControllerState.Unavailable)
            }
        }
        scope.launch {
            controller.events.collect { event ->
                when (event) {
                    is OnlineConsultantControllerEvent.MessageSendFailed -> {
                        Toast.makeText(
                            context,
                            formatViewMessageSendErrorToast(context, event.throwable),
                            Toast.LENGTH_SHORT,
                        ).show()
                        onError(event.throwable)
                    }

                    is OnlineConsultantControllerEvent.FileSendFailed -> {
                        if (!showUploadSizeErrorToast(context, event.throwable)) {
                            onError(event.throwable)
                        }
                    }

                    is OnlineConsultantControllerEvent.ContactFormSendFailed -> onError(event.throwable)
                    is OnlineConsultantControllerEvent.LoadOlderMessagesFailed -> onError(event.throwable)
                    is OnlineConsultantControllerEvent.InitialConnectFailed -> {
                        controller.sdk.logError(
                            "initial consultant connect failed",
                            event.throwable
                        )
                        Toast.makeText(
                            context,
                            formatViewInitialConnectErrorToast(context, event.throwable),
                            Toast.LENGTH_SHORT,
                        ).show()
                        onError(event.throwable)
                    }
                }
            }
        }
    }

    fun bind(
        sdk: CrmSdk,
        scope: CoroutineScope,
        onAttachClick: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        bind(
            controller = sdk.consultantController(),
            scope = scope,
            onAttachClick = onAttachClick,
            onError = onError,
        )
    }

    /**
     * Binds the chat with a system file picker. Call before [lifecycleOwner] is STARTED.
     * [activityResultKey] must be stable across recreation and unique within the registry.
     */
    fun bind(
        sdk: CrmSdk,
        scope: CoroutineScope,
        activityResultRegistry: ActivityResultRegistry,
        lifecycleOwner: LifecycleOwner,
        activityResultKey: String,
        onError: (Throwable) -> Unit = {},
    ) {
        check(
            lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED &&
                    !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            "Bind the consultant file picker before the LifecycleOwner is STARTED"
        }
        require(activityResultKey.isNotBlank()) { "activityResultKey must not be blank" }
        val controller = sdk.consultantController()
        bind(controller, scope, onAttachClick = {
            runCatching { filePicker?.launch(arrayOf("*/*")) }
                .onFailure(onError)
        }, onError = onError)
        filePicker = activityResultRegistry.register(
            activityResultKey,
            lifecycleOwner,
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }.onFailure { throwable ->
                    controller.sdk.logError(
                        "persistable read permission was not granted for selected file: uri=$uri",
                        throwable,
                    )
                }
                controller.sendFile(uri)
            }
        }
        filePickerOwner = lifecycleOwner
        filePickerObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                clearFilePicker()
                setOnAttachClickListener {}
            }
        }.also { lifecycleOwner.lifecycle.addObserver(it) }
    }

    private fun clearFilePicker() {
        filePicker?.unregister()
        filePicker = null
        filePickerObserver?.let { filePickerOwner?.lifecycle?.removeObserver(it) }
        filePickerObserver = null
        filePickerOwner = null
    }
}
