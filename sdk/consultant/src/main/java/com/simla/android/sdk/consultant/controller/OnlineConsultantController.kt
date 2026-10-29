package com.simla.android.sdk.consultant.controller

import android.net.Uri
import com.simla.android.sdk.consultant.OnlineConsultantImpl
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.util.errorSignature
import com.simla.android.sdk.session.SdkUserPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnlineConsultantController private constructor(
    internal val sdk: OnlineConsultantImpl,
    private val scope: CoroutineScope,
    private val ownsScope: Boolean,
) {
    private val stateMutable =
        MutableStateFlow<OnlineConsultantControllerState>(OnlineConsultantControllerState.Idle)
    private val lastErrorMutable = MutableStateFlow<Throwable?>(null)
    private val eventsMutable = MutableSharedFlow<OnlineConsultantControllerEvent>(
        replay = 0,
        extraBufferCapacity = 16,
    )

    private var startJob: Job? = null
    private var collectorsJob: Job? = null

    @Volatile
    private var hasConnectedOnce = false
    private var user = SdkUserPayload()
    private var lastInitialConnectErrorSignature: String? = null

    internal val state: StateFlow<OnlineConsultantControllerState> = stateMutable.asStateFlow()
    internal val lastError: StateFlow<Throwable?> = lastErrorMutable.asStateFlow()
    internal val events: SharedFlow<OnlineConsultantControllerEvent> = eventsMutable.asSharedFlow()
    internal val unreadCount: StateFlow<Int> = sdk.unreadCount
    internal val downloadedFileRegistry: ConsultantDownloadedFileRegistry =
        sdk.downloadedFileRegistry

    internal constructor(
        sdk: OnlineConsultantImpl,
    ) : this(
        sdk = sdk,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ownsScope = true,
    )

    internal constructor(
        sdk: OnlineConsultantImpl,
        scope: CoroutineScope,
    ) : this(
        sdk = sdk,
        scope = scope,
        ownsScope = false,
    )

    internal fun start(
        user: SdkUserPayload = SdkUserPayload(),
    ) {
        this.user = user
        if (startJob?.isActive == true) {
            return
        }

        startCollectors()
        startJob = scope.launch {
            if (!hasConnectedOnce) {
                stateMutable.value = OnlineConsultantControllerState.InitialLoading
            } else {
                publishReadyState()
                return@launch
            }

            while (!hasConnectedOnce) {
                val widgetAvailable = runCatching {
                    val response = sdk.connect()
                    if (response.behaviour?.widget != true) {
                        return@runCatching false
                    }

                    true
                }.onSuccess {
                    lastErrorMutable.value = null
                    lastInitialConnectErrorSignature = null
                }.onFailure { throwable ->
                    lastErrorMutable.value = throwable
                    val errorSignature = throwable.errorSignature()
                    if (lastInitialConnectErrorSignature != errorSignature) {
                        lastInitialConnectErrorSignature = errorSignature
                        eventsMutable.tryEmit(
                            OnlineConsultantControllerEvent.InitialConnectFailed(
                                throwable
                            )
                        )
                    }
                }.getOrNull()

                if (widgetAvailable == true) {
                    hasConnectedOnce = true
                    publishReadyState()
                    return@launch
                }

                if (widgetAvailable == false) {
                    stateMutable.value = OnlineConsultantControllerState.Unavailable
                    return@launch
                }

                delay(WEBSOCKET_RECONNECT_DELAY_MS)
            }
        }
    }

    internal fun sendMessage(text: String) {
        scope.launch {
            runCatching {
                sdk.sendMessage(text)
            }.onFailure { throwable ->
                eventsMutable.tryEmit(OnlineConsultantControllerEvent.MessageSendFailed(throwable))
            }
        }
    }

    internal fun sendFile(uri: Uri) {
        scope.launch {
            runCatching {
                sdk.sendFile(uri)
            }.onFailure { throwable ->
                eventsMutable.tryEmit(OnlineConsultantControllerEvent.FileSendFailed(throwable))
            }
        }
    }

    internal fun loadOlderMessages() {
        scope.launch {
            runCatching {
                sdk.loadOlderMessages()
            }.onFailure { throwable ->
                eventsMutable.tryEmit(
                    OnlineConsultantControllerEvent.LoadOlderMessagesFailed(
                        throwable
                    )
                )
            }
        }
    }

    internal fun submitContactForm(
        kind: ConsultantContactFormKind,
        data: ConsultantContactFormRequest,
    ) {
        scope.launch {
            runCatching {
                when (kind) {
                    ConsultantContactFormKind.CONTACT -> sdk.sendContactForm(data)
                    ConsultantContactFormKind.OFFLINE -> sdk.sendOfflineForm(data)
                }
            }.onFailure { throwable ->
                eventsMutable.tryEmit(
                    OnlineConsultantControllerEvent.ContactFormSendFailed(
                        throwable
                    )
                )
            }
        }
    }

    internal fun markMessageVisible(messageId: String) {
        sdk.scheduleMessageRead(messageId)
    }

    internal fun dispose() {
        startJob?.cancel()
        collectorsJob?.cancel()
        if (ownsScope) {
            scope.cancel()
        }
    }

    private fun startCollectors() {
        if (collectorsJob?.isActive == true) {
            return
        }

        collectorsJob = scope.launch {
            launch {
                sdk.chatMessages.collect {
                    publishReadyState()
                }
            }
            launch {
                sdk.chatHistoryState.collect {
                    publishReadyState()
                }
            }
            launch {
                sdk.widgetConfigState.collect {
                    publishReadyState()
                }
            }
            launch {
                sdk.offlineState.collect {
                    publishReadyState()
                }
            }
        }
    }

    private fun publishReadyState() {
        if (!hasConnectedOnce) {
            return
        }

        val historyState = sdk.chatHistoryState.value
        stateMutable.value = OnlineConsultantControllerState.Ready(
            messages = sdk.chatMessages.value,
            isInitialLoading = historyState.isInitialLoading,
            isLoadingOlderMessages = historyState.isLoadingOlderMessages,
            canLoadOlderMessages = historyState.canLoadOlderMessages,
            widgetConfig = sdk.widgetConfigState.value,
            isOffline = sdk.offlineState.value,
        )
    }

    private companion object {
        const val WEBSOCKET_RECONNECT_DELAY_MS = 3_000L
    }
}
