package com.simla.android.sdk.consultant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.simla.android.sdk.CrmSdkConfig
import com.simla.android.sdk.consultant.connection.shouldDelayInitialActionMessages
import com.simla.android.sdk.consultant.contactform.merge
import com.simla.android.sdk.consultant.contactform.normalized
import com.simla.android.sdk.consultant.contactform.shouldShowContactFormMessage
import com.simla.android.sdk.consultant.controller.OnlineConsultantController
import com.simla.android.sdk.consultant.error.OnlineConsultantUploadSizeException
import com.simla.android.sdk.consultant.localization.ConsultantStrings
import com.simla.android.sdk.consultant.mapping.fallbackMessageText
import com.simla.android.sdk.consultant.mapping.formatMessageTime
import com.simla.android.sdk.consultant.mapping.formatMoneyLabel
import com.simla.android.sdk.consultant.mapping.formatOrderNumberLabel
import com.simla.android.sdk.consultant.mapping.formatQuantityLabel
import com.simla.android.sdk.consultant.mapping.latestRealtimeMarker
import com.simla.android.sdk.consultant.mapping.mapDeliveryStatus
import com.simla.android.sdk.consultant.mapping.resolveManagerAuthorName
import com.simla.android.sdk.consultant.mapping.resolveManagerAvatarUrl
import com.simla.android.sdk.consultant.mapping.toUiButtons
import com.simla.android.sdk.consultant.mapping.toUiFields
import com.simla.android.sdk.consultant.mapping.toUiItems
import com.simla.android.sdk.consultant.mapping.toUiQuickResponses
import com.simla.android.sdk.consultant.models.action.ConsultantSiteAction
import com.simla.android.sdk.consultant.models.connection.ConsultantConnectResponse
import com.simla.android.sdk.consultant.models.connection.ConsultantOfflineStatus
import com.simla.android.sdk.consultant.models.connection.ReconnectState
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormState
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormUiModel
import com.simla.android.sdk.consultant.models.file.ConsultantFileLinkResponse
import com.simla.android.sdk.consultant.models.file.ConsultantFileUploadResponse
import com.simla.android.sdk.consultant.models.file.ConsultantUploadSource
import com.simla.android.sdk.consultant.models.message.ConsultantChatHistoryState
import com.simla.android.sdk.consultant.models.message.ConsultantMessage
import com.simla.android.sdk.consultant.models.message.ConsultantMessageItem
import com.simla.android.sdk.consultant.models.message.ConsultantMessagesListResponse
import com.simla.android.sdk.consultant.models.message.ConsultantSendMessageBody
import com.simla.android.sdk.consultant.models.message.ConsultantSendMessageItem
import com.simla.android.sdk.consultant.models.message.ConsultantSendMessageRequest
import com.simla.android.sdk.consultant.models.message.ConsultantSendMessageResponse
import com.simla.android.sdk.consultant.models.message.toDto
import com.simla.android.sdk.consultant.models.realtime.ConsultantWebSocketEnvelope
import com.simla.android.sdk.consultant.models.realtime.ConsultantWebSocketEvent
import com.simla.android.sdk.consultant.models.realtime.OnlineConsultantNotificationEvent
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.realtime.latestRealtimeMarker
import com.simla.android.sdk.consultant.serialization.SdkJson
import com.simla.android.sdk.consultant.serialization.asFlexibleString
import com.simla.android.sdk.consultant.serialization.jsonString
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.lastIncomingReadMessageId
import com.simla.android.sdk.consultant.ui.models.sortedForDisplay
import com.simla.android.sdk.consultant.upload.formatFileSizeLabel
import com.simla.android.sdk.consultant.upload.isImageUpload
import com.simla.android.sdk.consultant.upload.mimeTypeFromFileName
import com.simla.android.sdk.consultant.upload.mimeTypeFromImageSignature
import com.simla.android.sdk.consultant.upload.resolveUploadMimeType
import com.simla.android.sdk.consultant.upload.uploadSizeError
import com.simla.android.sdk.error.CrmSdkException
import com.simla.android.sdk.error.sdkHttpException
import com.simla.android.sdk.logging.SdkLogger
import com.simla.android.sdk.network.SdkHttpRequest
import com.simla.android.sdk.network.SdkHttpResponse
import com.simla.android.sdk.network.SdkNetworkEngine
import com.simla.android.sdk.network.SdkRawHttpRequest
import com.simla.android.sdk.network.SdkWebSocketRequest
import com.simla.android.sdk.network.SdkWebSocketSession
import com.simla.android.sdk.network.SdkWebSocketSessionListener
import com.simla.android.sdk.network.withQueryParameters
import com.simla.android.sdk.runtime.CrmRuntime
import com.simla.android.sdk.session.SdkConnectRequest
import com.simla.android.sdk.session.SdkSession
import com.simla.android.sdk.session.SdkUserPayload
import com.simla.android.sdk.session.SessionPageContext
import com.simla.android.sdk.storage.SdkSessionStorage
import com.simla.android.sdk.util.fileNameFromUrl
import com.simla.android.sdk.util.toIso8601Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.ByteArrayOutputStream

internal class OnlineConsultantImpl(
    private val appContext: Context,
    internal val config: CrmSdkConfig,
    private val logger: SdkLogger,
    private val networkEngine: SdkNetworkEngine,
    sessionStorage: SdkSessionStorage? = null,
    internal val downloadedFileRegistry: ConsultantDownloadedFileRegistry,
    runtime: CrmRuntime? = null,
) : OnlineConsultant {
    private val runtime = runtime ?: CrmRuntime(
        config,
        networkEngine,
        logger,
        requireNotNull(sessionStorage) { "Session storage or shared runtime is required" },
    )
    private val strings = ConsultantStrings(appContext)
    private val stateMutex = Mutex()
    private val sessionGeneration = ConsultantGeneration()
    private val webSocketGeneration = ConsultantGeneration()
    internal val sdkScope = this.runtime.scope
    private val realtimeEventsMutable = MutableSharedFlow<ConsultantWebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    private val notificationEventsMutable = MutableSharedFlow<OnlineConsultantNotificationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    private val chatMessagesMutable =
        MutableStateFlow<List<OnlineConsultantMessageUiModel>>(emptyList())
    private val chatHistoryStateMutable = MutableStateFlow(ConsultantChatHistoryState())
    private val widgetConfigMutable = MutableStateFlow<ConsultantWidgetConfig?>(null)
    private val offlineStateMutable = MutableStateFlow(false)
    private val unreadCountMutable = MutableStateFlow(0)

    internal val controller: OnlineConsultantController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OnlineConsultantController(this)
    }

    private var activeWebSocket: SdkWebSocketSession? = null
    private var reconnectJob: Job? = null
    private var shouldReconnectWebSocket = false
    private var reconnectAttempts = 0
    private var lastWebSocketUpdatedAt: String? = null
    private var loadedHistoryMessagesCount = 0
    private var widgetConfig: ConsultantWidgetConfig? = null
    private var isOffline = false
    private var contactFormUser = ConsultantContactFormRequest()
    private var contactFormWasSent = false
    private var offlineFormWasSent = false
    private var pendingInitialActionMessages: List<OnlineConsultantMessageUiModel> = emptyList()
    private var lastReadMessageId = 0L
    private var readMessageJob: Job? = null

    internal val realtimeEvents: SharedFlow<ConsultantWebSocketEvent> =
        realtimeEventsMutable.asSharedFlow()
    internal val notificationEvents: SharedFlow<OnlineConsultantNotificationEvent> =
        notificationEventsMutable.asSharedFlow()
    internal val chatMessages: StateFlow<List<OnlineConsultantMessageUiModel>> =
        chatMessagesMutable.asStateFlow()
    internal val chatHistoryState: StateFlow<ConsultantChatHistoryState> =
        chatHistoryStateMutable.asStateFlow()
    internal val widgetConfigState: StateFlow<ConsultantWidgetConfig?> =
        widgetConfigMutable.asStateFlow()
    internal val offlineState: StateFlow<Boolean> = offlineStateMutable.asStateFlow()
    override val unreadCount: StateFlow<Int> = unreadCountMutable.asStateFlow()

    init {
        this.runtime.addListener(object : com.simla.android.sdk.runtime.CrmRuntimeListener {
            override suspend fun onConnected(
                response: com.simla.android.sdk.runtime.CrmConnectResponse,
                pageContext: SessionPageContext,
                user: SdkUserPayload,
            ) {
                val parsed = applyConnectResponse(response, user)
                if (
                    parsed.behaviour?.widget == true &&
                    this@OnlineConsultantImpl.runtime.isAppInForeground()
                ) {
                    subscribeWebSocket()
                }
            }

            override suspend fun onDisconnected() {
                resetAfterRuntimeDisconnect()
            }

            override suspend fun onForeground() {
                if (currentConnectResponse()?.behaviour?.widget == true) {
                    subscribeWebSocket()
                }
            }

            override suspend fun onBackground() {
                disconnectWebSocket()
            }

            override fun onClosed() {
                close()
            }
        })
    }

    internal suspend fun connect(): ConsultantConnectResponse {
        runtime.awaitConnected()
        return currentConnectResponse()
            ?: throw CrmSdkException("Connect response is unavailable")
    }

    private suspend fun applyConnectResponse(
        coreResponse: com.simla.android.sdk.runtime.CrmConnectResponse,
        user: SdkUserPayload,
    ): ConsultantConnectResponse {
        chatHistoryStateMutable.value = ConsultantChatHistoryState(isInitialLoading = true)
        try {
            val parsed = try {
                SdkJson.decodeFromString<ConsultantConnectResponse>(coreResponse.rawBody)
            } catch (throwable: Throwable) {
                throw CrmSdkException("Unable to parse connect response", throwable)
            }

            val parsedWidgetConfig = parsed.config?.let { configJson ->
                runCatching {
                    SdkJson.decodeFromJsonElement<ConsultantWidgetConfig>(configJson)
                }.onFailure { throwable ->
                    logger.e("unable to parse widget config", throwable)
                }.getOrNull()
            }
            val resolvedContactFormUser = ConsultantContactFormRequest(
                name = parsed.user.name ?: user.name,
                email = parsed.user.email ?: user.email,
                phone = parsed.user.phone ?: user.phone,
            )
            val initialOffline = parsed.behaviour?.isOffline == true
            val historyUiMessages = parsed.messages
                .sortedBy { it.realtimeMarker.orEmpty() }
                .mapNotNull { message -> toUiMessage(message, parsedWidgetConfig) }
            val actionUiMessages = parsed.actions.toInitialActionMessages(
                config = parsedWidgetConfig,
                serverTime = parsed.ts.asFlexibleString(),
            )
            val shouldDelayActionMessages = shouldDelayInitialActionMessages(
                messages = historyUiMessages,
                config = parsedWidgetConfig,
                user = resolvedContactFormUser,
                isOffline = initialOffline,
                contactFormWasSent = false,
            )
            val initialActionMessages = if (shouldDelayActionMessages) {
                emptyList()
            } else {
                actionUiMessages
            }
            val initialUiMessages = (historyUiMessages + initialActionMessages)
                .withVirtualMessages(
                    config = parsedWidgetConfig,
                    user = resolvedContactFormUser,
                    isOffline = initialOffline,
                    contactFormWasSent = false,
                    offlineFormWasSent = false,
                )

            stateMutex.withLock {
                lastWebSocketUpdatedAt = parsed.messages.latestRealtimeMarker()
                logger.d("connect websocket updated_at marker: $lastWebSocketUpdatedAt")
                loadedHistoryMessagesCount = parsed.messages.size
                widgetConfig = parsedWidgetConfig
                isOffline = initialOffline
                contactFormUser = resolvedContactFormUser
                contactFormWasSent = false
                offlineFormWasSent = false
                pendingInitialActionMessages = if (shouldDelayActionMessages) {
                    actionUiMessages
                } else {
                    emptyList()
                }
                lastReadMessageId = initialUiMessages.lastIncomingReadMessageId()
            }
            widgetConfigMutable.value = parsedWidgetConfig
            offlineStateMutable.value = initialOffline
            chatMessagesMutable.value = initialUiMessages
            refreshUnreadCount()
            chatHistoryStateMutable.value = ConsultantChatHistoryState(
                isInitialLoading = false,
                canLoadOlderMessages = parsed.messages.isNotEmpty(),
            )
            requestMissingFileLinks(parsed.messages)
            return parsed
        } catch (throwable: Throwable) {
            chatHistoryStateMutable.value = ConsultantChatHistoryState(isInitialLoading = false)
            throw throwable
        }
    }

    internal suspend fun loadOlderMessages(): List<ConsultantMessage> {
        val pageSize = DEFAULT_MESSAGES_PAGE_SIZE.coerceIn(1, 1000)
        val offset = stateMutex.withLock {
            if (chatHistoryStateMutable.value.isInitialLoading ||
                chatHistoryStateMutable.value.isLoadingOlderMessages ||
                !chatHistoryStateMutable.value.canLoadOlderMessages
            ) {
                return emptyList()
            }

            chatHistoryStateMutable.value = chatHistoryStateMutable.value.copy(
                isLoadingOlderMessages = true,
            )
            loadedHistoryMessagesCount
        }

        var fetchedMessagesCount: Int? = null
        return try {
            val response = networkEngine.getJson(
                SdkHttpRequest(
                    url = resolveClientApiUrl("messages").withQueryParameters(
                        "limit" to pageSize.toString(),
                        "offset" to offset.toString(),
                    ),
                    headers = buildAuthorizedHttpHeaders(),
                ),
            )

            if (!response.isSuccessful) {
                throw sdkHttpException(
                    strings.get(R.string.crm_consultant_load_messages_error),
                    response
                )
            }

            val parsed = try {
                SdkJson.decodeFromString<ConsultantMessagesListResponse>(response.body)
            } catch (throwable: Throwable) {
                throw CrmSdkException(
                    strings.get(R.string.crm_consultant_load_messages_error),
                    throwable,
                )
            }

            mergeHistoryMessages(parsed.messages)
            fetchedMessagesCount = parsed.messages.size
            stateMutex.withLock {
                loadedHistoryMessagesCount += parsed.messages.size
            }
            requestMissingFileLinks(parsed.messages)
            parsed.messages
        } finally {
            chatHistoryStateMutable.value = chatHistoryStateMutable.value.copy(
                isLoadingOlderMessages = false,
                canLoadOlderMessages = fetchedMessagesCount?.let { it >= pageSize }
                    ?: chatHistoryStateMutable.value.canLoadOlderMessages,
            )
        }
    }

    internal suspend fun sendMessage(
        text: String,
        user: SdkUserPayload = SdkUserPayload(),
    ): ConsultantSendMessageResponse {
        val trimmedText = text.trim()
        require(trimmedText.isNotEmpty()) {
            strings.get(R.string.crm_consultant_message_blank_error)
        }
        val pendingCreatedAtMillis = System.currentTimeMillis()
        val pendingSentAt = pendingCreatedAtMillis.toIso8601Timestamp()
        val pendingMessageId = "$LOCAL_MESSAGE_ID_PREFIX$pendingCreatedAtMillis"
        val shouldShowContactFormAfterThisMessage = stateMutex.withLock {
            !contactFormWasSent &&
                    widgetConfig?.contactFormDisplay == CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE &&
                    chatMessagesMutable.value.none {
                        it.author == OnlineConsultantMessageAuthor.VISITOR && it.contactForm == null
                    }
        }

        upsertUiMessage(
            OnlineConsultantMessageUiModel(
                id = pendingMessageId,
                author = OnlineConsultantMessageAuthor.VISITOR,
                authorName = user.name ?: strings.you,
                text = trimmedText,
                timeLabel = formatMessageTime(pendingSentAt, strings),
                sentAt = pendingSentAt,
                deliveryStatus = OnlineConsultantMessageDeliveryStatus.SENDING,
            ),
        )

        val response = try {
            networkEngine.postJson(
                SdkHttpRequest(
                    url = resolveClientApiUrl("messages"),
                    headers = buildAuthorizedHttpHeaders(),
                    body = SdkJson.encodeToString(
                        ConsultantSendMessageRequest(
                            message = ConsultantSendMessageBody(
                                type = TEXT_MESSAGE_TYPE,
                                text = trimmedText,
                            ),
                            user = user.toDto(),
                        ),
                    ),
                ),
            )
        } catch (throwable: Throwable) {
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw throwable
        }

        if (!response.isSuccessful) {
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw sdkHttpException(
                strings.get(R.string.crm_consultant_send_message_error),
                response
            )
        }

        val parsed = try {
            SdkJson.decodeFromString<ConsultantSendMessageResponse>(response.body)
        } catch (throwable: Throwable) {
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw CrmSdkException(
                strings.get(R.string.crm_consultant_send_message_error),
                throwable,
            )
        }

        val outgoingMessage = ConsultantMessage(
            id = parsed.message.id?.let(::jsonString),
            type = TEXT_MESSAGE_TYPE,
            content = trimmedText,
            time = parsed.message.time,
            status = parsed.message.status,
            fromMe = true,
            updatedAt = parsed.message.updatedAt,
        )

        replaceUiMessage(
            oldId = pendingMessageId,
            newMessage = toUiMessage(outgoingMessage) ?: return parsed,
        )
        markIncomingMessagesReadLocally()
        stateMutex.withLock {
            loadedHistoryMessagesCount += 1
        }
        refreshRealtimeMarker(outgoingMessage.realtimeMarker)
        if (shouldShowContactFormAfterThisMessage) {
            refreshVirtualMessages()
        }
        return parsed
    }

    internal suspend fun sendFile(
        uri: Uri,
        user: SdkUserPayload = SdkUserPayload(),
    ): ConsultantSendMessageResponse {
        logger.d("send file requested: uri=$uri")
        val uploadSource = readUploadSource(uri)
        logger.d(
            "send file source resolved: uri=$uri, name=${uploadSource.fileName}, " +
                    "mime=${uploadSource.mimeType}, type=${uploadSource.messageType}, " +
                    "size=${uploadSource.sizeBytes}",
        )
        val pendingCreatedAtMillis = System.currentTimeMillis()
        val pendingSentAt = pendingCreatedAtMillis.toIso8601Timestamp()
        val pendingMessageId =
            "$LOCAL_MESSAGE_ID_PREFIX${uploadSource.messageType}-$pendingCreatedAtMillis"
        val shouldShowContactFormAfterThisMessage = stateMutex.withLock {
            !contactFormWasSent &&
                    widgetConfig?.contactFormDisplay == CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE &&
                    chatMessagesMutable.value.none {
                        it.author == OnlineConsultantMessageAuthor.VISITOR && it.contactForm == null
                    }
        }

        upsertUiMessage(
            OnlineConsultantMessageUiModel(
                id = pendingMessageId,
                author = OnlineConsultantMessageAuthor.VISITOR,
                authorName = user.name ?: strings.you,
                text = "",
                timeLabel = formatMessageTime(pendingSentAt, strings),
                sentAt = pendingSentAt,
                deliveryStatus = OnlineConsultantMessageDeliveryStatus.SENDING,
                imageUrl = uploadSource.uri.toString().takeIf {
                    uploadSource.messageType == IMAGE_MESSAGE_TYPE
                },
                fileUrl = uploadSource.uri.toString().takeIf {
                    uploadSource.messageType == FILE_MESSAGE_TYPE
                },
                fileName = uploadSource.fileName.takeIf {
                    uploadSource.messageType == FILE_MESSAGE_TYPE
                },
                fileSizeLabel = uploadSource.sizeBytes.formatFileSizeLabel(),
            ),
        )

        val uploadedFileId = try {
            uploadFileBytes(uploadSource)
        } catch (throwable: Throwable) {
            logger.e("file upload step failed: uri=$uri", throwable)
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw throwable
        }
        logger.d(
            "file upload step completed: uri=$uri, type=${uploadSource.messageType}, " +
                    "uploadedFileId=$uploadedFileId",
        )

        val response = try {
            logger.d(
                "send file message requested: uri=$uri, type=${uploadSource.messageType}, " +
                        "uploadedFileId=$uploadedFileId",
            )
            networkEngine.postJson(
                SdkHttpRequest(
                    url = resolveClientApiUrl("messages"),
                    headers = buildAuthorizedHttpHeaders(),
                    body = SdkJson.encodeToString(
                        ConsultantSendMessageRequest(
                            message = ConsultantSendMessageBody(
                                type = uploadSource.messageType,
                                items = listOf(
                                    ConsultantSendMessageItem(
                                        id = uploadedFileId,
                                        caption = uploadSource.fileName,
                                    ),
                                ),
                            ),
                            user = user.toDto(),
                        ),
                    ),
                ),
            )
        } catch (throwable: Throwable) {
            logger.e("send file message request failed: uri=$uri", throwable)
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw throwable
        }
        logger.d(
            "send file message response: uri=$uri, type=${uploadSource.messageType}, " +
                    "http=${response.code}",
        )

        if (!response.isSuccessful) {
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw sdkHttpException(
                strings.get(R.string.crm_consultant_send_message_error),
                response
            )
        }

        val parsed = try {
            SdkJson.decodeFromString<ConsultantSendMessageResponse>(response.body)
        } catch (throwable: Throwable) {
            logger.e("unable to parse send file message response: uri=$uri", throwable)
            updateUiMessageStatus(pendingMessageId, OnlineConsultantMessageDeliveryStatus.ERROR)
            throw CrmSdkException(
                strings.get(R.string.crm_consultant_send_message_error),
                throwable,
            )
        }
        logger.d(
            "send file completed: uri=$uri, type=${uploadSource.messageType}, " +
                    "messageId=${parsed.message.id?.let(::jsonString)}",
        )

        val outgoingMessage = ConsultantMessage(
            id = parsed.message.id?.let(::jsonString),
            type = uploadSource.messageType,
            content = "",
            items = listOf(
                ConsultantMessageItem(
                    id = jsonString(uploadedFileId),
                    url = uploadSource.uri.toString(),
                    caption = uploadSource.fileName,
                    size = uploadSource.sizeBytes,
                ),
            ),
            time = parsed.message.time,
            status = parsed.message.status,
            fromMe = true,
            updatedAt = parsed.message.updatedAt,
        )

        replaceUiMessage(
            oldId = pendingMessageId,
            newMessage = toUiMessage(outgoingMessage) ?: return parsed,
        )
        markIncomingMessagesReadLocally()
        stateMutex.withLock {
            loadedHistoryMessagesCount += 1
        }
        refreshRealtimeMarker(outgoingMessage.realtimeMarker)
        if (shouldShowContactFormAfterThisMessage) {
            refreshVirtualMessages()
        }
        return parsed
    }

    internal suspend fun sendFiles(
        uris: List<Uri>,
        user: SdkUserPayload = SdkUserPayload(),
    ): List<ConsultantSendMessageResponse> {
        logger.d("send files requested: count=${uris.size}")
        return uris.map { uri -> sendFile(uri, user) }
    }

    internal fun logDebug(message: String) {
        logger.d(message)
    }

    internal fun logError(message: String, throwable: Throwable? = null) {
        logger.e(message, throwable)
    }

    internal suspend fun sendContactForm(
        data: ConsultantContactFormRequest,
    ) {
        val normalizedData = data.normalized()
        updateContactFormState(
            ConsultantContactFormKind.CONTACT,
            ConsultantContactFormState.SENDING
        )
        val response = try {
            networkEngine.postJson(
                SdkHttpRequest(
                    url = resolveClientApiUrl("contact/form"),
                    headers = buildAuthorizedHttpHeaders(),
                    body = SdkJson.encodeToString(normalizedData),
                ),
            )
        } catch (throwable: Throwable) {
            updateContactFormState(
                ConsultantContactFormKind.CONTACT,
                ConsultantContactFormState.SENT_ERROR
            )
            throw throwable
        }

        if (!response.isSuccessful) {
            updateContactFormState(
                ConsultantContactFormKind.CONTACT,
                ConsultantContactFormState.SENT_ERROR
            )
            throw sdkHttpException(
                strings.get(R.string.crm_consultant_contact_form_error),
                response
            )
        }

        stateMutex.withLock {
            contactFormUser = contactFormUser.merge(normalizedData)
            contactFormWasSent = true
        }
        releasePendingInitialActionMessages()
    }

    internal suspend fun sendOfflineForm(
        data: ConsultantContactFormRequest,
    ) {
        val normalizedData = data.normalized()
        updateContactFormState(
            ConsultantContactFormKind.OFFLINE,
            ConsultantContactFormState.SENDING
        )
        val response = try {
            networkEngine.postJson(
                SdkHttpRequest(
                    url = resolveClientApiUrl("offline/form"),
                    headers = buildAuthorizedHttpHeaders(),
                    body = SdkJson.encodeToString(normalizedData),
                ),
            )
        } catch (throwable: Throwable) {
            updateContactFormState(
                ConsultantContactFormKind.OFFLINE,
                ConsultantContactFormState.SENT_ERROR
            )
            throw throwable
        }

        if (!response.isSuccessful) {
            updateContactFormState(
                ConsultantContactFormKind.OFFLINE,
                ConsultantContactFormState.SENT_ERROR
            )
            throw sdkHttpException(
                strings.get(R.string.crm_consultant_offline_form_error),
                response
            )
        }

        stateMutex.withLock {
            contactFormUser = contactFormUser.merge(normalizedData)
            offlineFormWasSent = true
        }
        refreshVirtualMessages()
    }

    internal suspend fun resetAfterRuntimeDisconnect() {
        sessionGeneration.advance()
        disconnectWebSocket()
        stateMutex.withLock {
            lastWebSocketUpdatedAt = null
            widgetConfig = null
            isOffline = false
            contactFormUser = ConsultantContactFormRequest()
            contactFormWasSent = false
            offlineFormWasSent = false
            pendingInitialActionMessages = emptyList()
            lastReadMessageId = 0L
            readMessageJob?.cancel()
            readMessageJob = null
        }
        chatMessagesMutable.value = emptyList()
        chatHistoryStateMutable.value = ConsultantChatHistoryState()
        widgetConfigMutable.value = null
        offlineStateMutable.value = false
        unreadCountMutable.value = 0
    }

    internal fun scheduleMessageRead(messageId: String) {
        sdkScope.launch {
            val numericMessageId = prepareMessageRead(messageId) ?: return@launch
            readMessageJob?.cancel()
            readMessageJob = sdkScope.launch {
                delay(READ_MESSAGE_DEBOUNCE_MS)
                runCatching {
                    postMessageRead(numericMessageId)
                }.onFailure { throwable ->
                    logger.e("unable to mark message $numericMessageId as read", throwable)
                }
            }
        }
    }

    internal suspend fun markMessageRead(messageId: String) {
        val numericMessageId = prepareMessageRead(messageId) ?: return
        readMessageJob?.cancel()
        readMessageJob = null
        postMessageRead(numericMessageId)
    }

    internal suspend fun subscribeWebSocket(
        updatedAt: String? = null,
    ): SharedFlow<ConsultantWebSocketEvent> {
        val widgetAvailable = runtime.currentConnectResponse()?.behaviour?.widget == true
        if (!widgetAvailable) {
            logger.d("websocket subscription skipped: consultant widget is unavailable")
            return realtimeEvents
        }
        val resolvedUpdatedAt = updatedAt ?: stateMutex.withLock { lastWebSocketUpdatedAt }

        stateMutex.withLock {
            shouldReconnectWebSocket = AUTO_RECONNECT_WEBSOCKET
            reconnectAttempts = 0
            lastWebSocketUpdatedAt = resolvedUpdatedAt
        }

        cancelReconnectJob()
        closeCurrentWebSocket()
        openWebSocketConnection(resolvedUpdatedAt)

        return realtimeEvents
    }

    internal suspend fun disconnectWebSocket(
        code: Int = NORMAL_CLOSURE_STATUS,
        reason: String = "Closed by SDK",
    ) {
        webSocketGeneration.advance()
        stateMutex.withLock {
            shouldReconnectWebSocket = false
            reconnectAttempts = 0
            lastWebSocketUpdatedAt = null
        }

        cancelReconnectJob()
        closeCurrentWebSocket(code, reason)
    }

    internal suspend fun currentSession(): SdkSession? = runtime.currentSession()

    internal suspend fun currentConnectResponse(): ConsultantConnectResponse? =
        runtime.currentConnectResponse()?.let { response ->
            runCatching {
                SdkJson.decodeFromString<ConsultantConnectResponse>(response.rawBody)
            }.getOrNull()
        }

    internal suspend fun currentWidgetConfig(): ConsultantWidgetConfig? = stateMutex.withLock {
        widgetConfig
    }


    internal fun buildConnectRequest(
        userToken: String? = null,
        pageContext: SessionPageContext = SessionPageContext(),
        user: SdkUserPayload = SdkUserPayload(),
    ): SdkConnectRequest {
        return SdkConnectRequest(
            user = user.copy(token = userToken ?: user.token),
            pageTitle = pageContext.title,
            url = pageContext.url,
            referUrl = pageContext.referUrl,
        )
    }

    internal fun close() {
        sessionGeneration.advance()
        webSocketGeneration.advance()
        controller.dispose()
        shouldReconnectWebSocket = false
        reconnectJob?.cancel()
        reconnectJob = null
        activeWebSocket?.close(NORMAL_CLOSURE_STATUS, "SDK closed")
        activeWebSocket = null
    }

    private fun parseWebSocketEvent(
        text: String,
        expectedSessionGeneration: Long,
    ): ConsultantWebSocketEvent {
        val envelope = runCatching {
            SdkJson.decodeFromString<ConsultantWebSocketEnvelope>(text)
        }.getOrNull()

        val parsedMessage = envelope
            ?.takeIf { it.type == MESSAGE_EVENT_TYPE }
            ?.data
            ?.let(::parseMessagePayload)
        val parsedOfflineStatus = envelope
            ?.takeIf { it.type == OFFLINE_STATUS_EVENT_TYPE }
            ?.data
            ?.let(::parseOfflineStatusPayload)

        if (parsedMessage != null) {
            mergeIncomingMessage(parsedMessage, expectedSessionGeneration)
        }
        if (parsedOfflineStatus != null) {
            updateOfflineStatus(
                value = parsedOfflineStatus.isOffline,
                expectedSessionGeneration = expectedSessionGeneration,
            )
        }

        return ConsultantWebSocketEvent.Event(
            type = envelope?.type,
            data = envelope?.data,
            rawText = text,
            message = parsedMessage,
        )
    }

    private fun emitRealtimeEvent(event: ConsultantWebSocketEvent) {
        sdkScope.launch {
            realtimeEventsMutable.emit(event)
        }
    }

    private suspend fun openWebSocketConnection(updatedAt: String?) {
        val expectedSessionGeneration = sessionGeneration.snapshot()
        val expectedWebSocketGeneration = webSocketGeneration.advance()
        val session = runtime.currentSession() ?: run {
            logger.d("websocket subscription skipped: consultant session is unavailable")
            return
        }

        val listener = object : SdkWebSocketSessionListener {
            override fun onOpen(response: SdkHttpResponse) {
                if (!isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                    logger.d("ignored websocket open from a stale customer session")
                    return
                }
                logger.d("websocket connected")
                sdkScope.launch {
                    stateMutex.withLock {
                        if (!isCurrentWebSocket(
                                expectedSessionGeneration,
                                expectedWebSocketGeneration,
                            )
                        ) {
                            return@withLock
                        }
                        reconnectAttempts = 0
                    }
                }
                emitRealtimeEvent(
                    ConsultantWebSocketEvent.Connected(
                        code = response.code,
                        headers = response.headers,
                    ),
                )
            }

            override fun onTextMessage(text: String) {
                sdkScope.launch {
                    val event = stateMutex.withLock {
                        if (!isCurrentWebSocket(
                                expectedSessionGeneration,
                                expectedWebSocketGeneration,
                            )
                        ) {
                            logger.d("ignored websocket text event from a stale customer session")
                            return@withLock null
                        }
                        logger.d("websocket text event: $text")
                        parseWebSocketEvent(text, expectedSessionGeneration)
                    } ?: return@launch

                    if (isCurrentWebSocket(
                            expectedSessionGeneration,
                            expectedWebSocketGeneration,
                        )
                    ) {
                        realtimeEventsMutable.emit(event)
                    }
                }
            }

            override fun onBinaryMessage(payload: ByteArray) {
                if (!isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                    return
                }
                emitRealtimeEvent(ConsultantWebSocketEvent.Binary(payload))
            }

            override fun onClosing(code: Int, reason: String) {
                if (!isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                    return
                }
                emitRealtimeEvent(ConsultantWebSocketEvent.Closing(code, reason))
            }

            override fun onClosed(code: Int, reason: String) {
                if (!isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                    logger.d("ignored websocket close from a stale customer session")
                    return
                }
                clearWebSocket(expectedWebSocketGeneration)
                emitRealtimeEvent(ConsultantWebSocketEvent.Closed(code, reason))
                scheduleReconnectIfNeeded(expectedSessionGeneration, expectedWebSocketGeneration)
            }

            override fun onFailure(
                throwable: Throwable,
                response: com.simla.android.sdk.network.SdkHttpResponse?,
            ) {
                if (!isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                    logger.d("ignored websocket failure from a stale customer session")
                    return
                }
                logger.e("websocket failure", throwable)
                clearWebSocket(expectedWebSocketGeneration)
                emitRealtimeEvent(
                    ConsultantWebSocketEvent.Failure(
                        throwable = throwable,
                        responseCode = response?.code,
                        responseMessage = response?.message,
                    ),
                )
                scheduleReconnectIfNeeded(expectedSessionGeneration, expectedWebSocketGeneration)
            }
        }

        val request = buildSdkWebSocketRequest(
            url = resolveClientApiUrl("ws"),
            session = session,
            updatedAt = updatedAt,
        )

        val webSocket = networkEngine.openWebSocket(
            request = request,
            listener = listener,
        )
        stateMutex.withLock {
            if (isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)) {
                activeWebSocket = webSocket
            } else {
                webSocket.close(NORMAL_CLOSURE_STATUS, "Stale customer session")
            }
        }
    }

    internal fun resolveClientApiUrl(path: String): String {
        return runtime.resolveClientApiUrl(path)
    }

    internal suspend fun buildAuthorizedHttpHeaders(
        includeCustomerSiteId: Boolean = true,
    ): Map<String, String> = runtime.buildAuthorizedHttpHeaders(includeCustomerSiteId)

    private suspend fun readUploadSource(uri: Uri): ConsultantUploadSource =
        withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            logger.d("read upload source started: uri=$uri")
            val fileName = queryOpenableDisplayName(uri)
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: strings.attachment
            val declaredSize = queryOpenableSize(uri)
            val declaredMimeType = resolver.getType(uri)
            logger.d(
                "selected file metadata: uri=$uri, name=$fileName, " +
                        "declaredMime=$declaredMimeType, declaredSize=$declaredSize",
            )
            if (declaredSize != null && declaredSize > MAX_FILE_UPLOAD_BYTES) {
                logger.e(
                    "selected file rejected by declared size: uri=$uri, " +
                            "declaredSize=$declaredSize, max=$MAX_FILE_UPLOAD_BYTES",
                )
                throw OnlineConsultantUploadSizeException(
                    uploadSizeError(FILE_MESSAGE_TYPE, MAX_FILE_UPLOAD_BYTES, strings),
                )
            }

            val bytes = try {
                resolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_UPLOAD_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        total += read
                        if (total > MAX_FILE_UPLOAD_BYTES) {
                            logger.e(
                                "selected file rejected while reading: uri=$uri, " +
                                        "readSize=$total, max=$MAX_FILE_UPLOAD_BYTES",
                            )
                            throw OnlineConsultantUploadSizeException(
                                uploadSizeError(FILE_MESSAGE_TYPE, MAX_FILE_UPLOAD_BYTES, strings),
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                } ?: throw CrmSdkException(
                    strings.get(R.string.crm_consultant_file_upload_error),
                )
            } catch (throwable: Throwable) {
                logger.e("unable to read selected file: uri=$uri", throwable)
                throw throwable
            }
            logger.d("selected file read completed: uri=$uri, size=${bytes.size}")
            val mimeType = resolveUploadMimeType(
                declaredMimeType = declaredMimeType,
                fileName = fileName,
                bytes = bytes,
            )
            val messageType = if (isImageUpload(mimeType, fileName, bytes)) {
                IMAGE_MESSAGE_TYPE
            } else {
                FILE_MESSAGE_TYPE
            }
            logger.d(
                "selected file classified: uri=$uri, name=$fileName, " +
                        "declaredMime=$declaredMimeType, resolvedMime=$mimeType, " +
                        "messageType=$messageType, magicMime=${mimeTypeFromImageSignature(bytes)}, " +
                        "extensionMime=${mimeTypeFromFileName(fileName)}",
            )
            val maxSizeBytes = when (messageType) {
                IMAGE_MESSAGE_TYPE -> MAX_IMAGE_UPLOAD_BYTES
                else -> MAX_FILE_UPLOAD_BYTES
            }
            if (bytes.size > maxSizeBytes) {
                logger.e(
                    "selected file rejected by resolved type size: uri=$uri, " +
                            "type=$messageType, size=${bytes.size}, max=$maxSizeBytes",
                )
                throw OnlineConsultantUploadSizeException(
                    uploadSizeError(messageType, maxSizeBytes, strings),
                )
            }

            ConsultantUploadSource(
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                messageType = messageType,
                bytes = bytes,
                sizeBytes = bytes.size.toLong(),
            )
        }

    private fun queryOpenableDisplayName(uri: Uri): String? {
        return queryOpenableColumn(uri, OpenableColumns.DISPLAY_NAME)?.takeIf { it.isNotBlank() }
    }

    private fun queryOpenableSize(uri: Uri): Long? {
        return queryOpenableColumn(uri, OpenableColumns.SIZE)?.toLongOrNull()
    }

    private fun queryOpenableColumn(uri: Uri, column: String): String? {
        return appContext.contentResolver.query(uri, arrayOf(column), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val index = cursor.getColumnIndex(column)
                if (index >= 0 && !cursor.isNull(index)) {
                    cursor.getString(index)
                } else {
                    null
                }
            }
    }

    private suspend fun uploadFileBytes(uploadSource: ConsultantUploadSource): String {
        logger.d(
            "upload file bytes requested: uri=${uploadSource.uri}, " +
                    "mime=${uploadSource.mimeType}, size=${uploadSource.sizeBytes}",
        )
        val response = networkEngine.postRaw(
            SdkRawHttpRequest(
                url = resolveClientApiUrl("files"),
                headers = buildAuthorizedHttpHeaders(),
                body = uploadSource.bytes,
                contentType = uploadSource.mimeType,
            ),
        )
        logger.d(
            "upload file bytes response: uri=${uploadSource.uri}, http=${response.code}",
        )

        if (!response.isSuccessful) {
            throw sdkHttpException(strings.get(R.string.crm_consultant_file_upload_error), response)
        }

        val parsed = try {
            SdkJson.decodeFromString<ConsultantFileUploadResponse>(response.body)
        } catch (throwable: Throwable) {
            logger.e("unable to parse file upload response: uri=${uploadSource.uri}", throwable)
            throw CrmSdkException(
                strings.get(R.string.crm_consultant_file_upload_error),
                throwable,
            )
        }

        return parsed.idValue?.takeIf { it.isNotBlank() }
            ?: throw CrmSdkException(
                strings.get(R.string.crm_consultant_file_upload_error),
            )
    }

    private fun clearWebSocket(expectedWebSocketGeneration: Long) {
        sdkScope.launch {
            stateMutex.withLock {
                if (webSocketGeneration.isCurrent(expectedWebSocketGeneration)) {
                    activeWebSocket = null
                }
            }
        }
    }

    private fun scheduleReconnectIfNeeded(
        expectedSessionGeneration: Long,
        expectedWebSocketGeneration: Long,
    ) {
        sdkScope.launch {
            val reconnectState = stateMutex.withLock {
                if (
                    !shouldReconnectWebSocket ||
                    !isCurrentWebSocket(expectedSessionGeneration, expectedWebSocketGeneration)
                ) {
                    return@withLock null
                }

                reconnectAttempts += 1
                ReconnectState(
                    attempt = reconnectAttempts,
                    delayMs = WEBSOCKET_RECONNECT_DELAY_MS,
                    updatedAt = lastWebSocketUpdatedAt,
                )
            } ?: return@launch

            cancelReconnectJob()
            emitRealtimeEvent(
                ConsultantWebSocketEvent.Reconnecting(
                    attempt = reconnectState.attempt,
                    delayMs = reconnectState.delayMs,
                ),
            )

            reconnectJob = launch {
                delay(reconnectState.delayMs)

                val shouldReconnect = stateMutex.withLock {
                    shouldReconnectWebSocket &&
                            isCurrentWebSocket(
                                expectedSessionGeneration,
                                expectedWebSocketGeneration,
                            )
                }
                if (!shouldReconnect) {
                    return@launch
                }

                runCatching {
                    openWebSocketConnection(reconnectState.updatedAt)
                }.onFailure { throwable ->
                    logger.e("websocket reconnect failed", throwable)
                    emitRealtimeEvent(
                        ConsultantWebSocketEvent.Failure(
                            throwable = throwable,
                        ),
                    )
                    scheduleReconnectIfNeeded(
                        expectedSessionGeneration = expectedSessionGeneration,
                        expectedWebSocketGeneration = webSocketGeneration.snapshot(),
                    )
                }
            }
        }
    }

    private fun isCurrentWebSocket(
        expectedSessionGeneration: Long,
        expectedWebSocketGeneration: Long,
    ): Boolean = sessionGeneration.isCurrent(expectedSessionGeneration) &&
            webSocketGeneration.isCurrent(expectedWebSocketGeneration)

    private fun cancelReconnectJob() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun closeCurrentWebSocket(
        code: Int = NORMAL_CLOSURE_STATUS,
        reason: String = "Closed by SDK",
    ) {
        webSocketGeneration.advance()
        val webSocket = activeWebSocket ?: return
        webSocket.close(code, reason)
        activeWebSocket = null
    }

    private fun parseMessagePayload(payload: JsonElement): ConsultantMessage? {
        return runCatching {
            SdkJson.decodeFromJsonElement<ConsultantMessage>(payload)
        }.onFailure { throwable ->
            logger.e("unable to parse websocket message payload", throwable)
        }.getOrNull()
    }

    private fun parseOfflineStatusPayload(payload: JsonElement): ConsultantOfflineStatus? {
        return runCatching {
            SdkJson.decodeFromJsonElement<ConsultantOfflineStatus>(payload)
        }.onFailure { throwable ->
            logger.e("unable to parse websocket offline status payload", throwable)
        }.getOrNull()
    }

    private fun updateOfflineStatus(
        value: Boolean,
        expectedSessionGeneration: Long? = null,
    ) {
        sdkScope.launch {
            stateMutex.withLock {
                if (
                    expectedSessionGeneration != null &&
                    !sessionGeneration.isCurrent(expectedSessionGeneration)
                ) {
                    return@withLock
                }
                isOffline = value
            }
            if (
                expectedSessionGeneration != null &&
                !sessionGeneration.isCurrent(expectedSessionGeneration)
            ) {
                return@launch
            }
            offlineStateMutable.value = value
            refreshVirtualMessages()
        }
    }

    private fun mergeIncomingMessage(
        message: ConsultantMessage,
        expectedSessionGeneration: Long,
    ) {
        if (!sessionGeneration.isCurrent(expectedSessionGeneration)) {
            return
        }
        val uiMessage = toUiMessage(message, widgetConfigMutable.value) ?: return
        val isNewMessage = chatMessagesMutable.value.none { it.id == uiMessage.id }
        logger.d(
            "messages update: mergeIncomingMessage id=${uiMessage.id} isNew=$isNewMessage " +
                    "before=${chatMessagesMutable.value.size} status=${uiMessage.deliveryStatus} " +
                    "imageUrl=${uiMessage.imageUrl != null} fileUrl=${uiMessage.fileUrl != null}",
        )
        refreshRealtimeMarker(message.realtimeMarker, expectedSessionGeneration)
        upsertUiMessage(uiMessage)
        if (isNewMessage && shouldEmitIncomingMessageNotification(uiMessage)) {
            notificationEventsMutable.tryEmit(
                OnlineConsultantNotificationEvent.IncomingMessage(uiMessage),
            )
        }
        if (isNewMessage) {
            sdkScope.launch {
                stateMutex.withLock {
                    if (!sessionGeneration.isCurrent(expectedSessionGeneration)) {
                        return@withLock
                    }
                    loadedHistoryMessagesCount += 1
                }
            }
        }
        requestMissingFileLinks(listOf(message), expectedSessionGeneration)
    }

    private suspend fun refreshVirtualMessages() {
        stateMutex.withLock {
            val currentConfig = widgetConfig ?: return@withLock
            chatMessagesMutable.value = chatMessagesMutable.value.withVirtualMessages(
                config = currentConfig,
                user = contactFormUser,
                isOffline = isOffline,
                contactFormWasSent = contactFormWasSent,
                offlineFormWasSent = offlineFormWasSent,
            )
            refreshUnreadCount()
        }
    }

    private suspend fun releasePendingInitialActionMessages() {
        stateMutex.withLock {
            val currentConfig = widgetConfig ?: return@withLock
            val pendingMessages = pendingInitialActionMessages
            pendingInitialActionMessages = emptyList()

            val currentMessages = chatMessagesMutable.value.filterNot {
                it.isVirtualSupportMessage
            }
            chatMessagesMutable.value = (currentMessages + pendingMessages).withVirtualMessages(
                config = currentConfig,
                user = contactFormUser,
                isOffline = isOffline,
                contactFormWasSent = contactFormWasSent,
                offlineFormWasSent = offlineFormWasSent,
            )
            refreshUnreadCount()
        }
    }

    private fun List<OnlineConsultantMessageUiModel>.withVirtualMessages(
        config: ConsultantWidgetConfig?,
        user: ConsultantContactFormRequest,
        isOffline: Boolean,
        contactFormWasSent: Boolean,
        offlineFormWasSent: Boolean,
    ): List<OnlineConsultantMessageUiModel> {
        val realMessages = filterNot { it.isVirtualSupportMessage }
        config ?: return realMessages.sortedForDisplay()

        val virtualMessages = buildList {
            if (shouldShowOfflineMessage(
                    config = config,
                    isOffline = isOffline,
                )
            ) {
                add(
                    config.toOfflineMessage(
                        user = user,
                        wasSent = offlineFormWasSent,
                    ),
                )
                return@buildList
            }

            if (shouldShowContactFormMessage(
                    messages = realMessages,
                    config = config,
                    user = user,
                    display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                    wasSent = contactFormWasSent,
                    isOffline = isOffline,
                ) || shouldShowContactFormMessage(
                    messages = realMessages,
                    config = config,
                    user = user,
                    display = CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE,
                    wasSent = contactFormWasSent,
                    isOffline = isOffline,
                )
            ) {
                add(config.toContactFormMessage(user))
                return@buildList
            }

            if (shouldShowIntroMessage(
                    messages = realMessages,
                    config = config,
                    isOffline = isOffline,
                )
            ) {
                add(config.toIntroMessage())
            }
        }

        return (realMessages + virtualMessages).sortedForDisplay()
    }

    private val OnlineConsultantMessageUiModel.isVirtualSupportMessage: Boolean
        get() = id == CONTACT_FORM_MESSAGE_ID ||
                id == OFFLINE_MESSAGE_ID ||
                id == INTRO_MESSAGE_ID

    private val OnlineConsultantMessageUiModel.isIncomingRealMessage: Boolean
        get() = !isVirtualSupportMessage && author != OnlineConsultantMessageAuthor.VISITOR

    private fun shouldShowOfflineMessage(
        config: ConsultantWidgetConfig,
        isOffline: Boolean,
    ): Boolean {
        return isOffline && config.useOfflineMode
    }

    private fun shouldShowIntroMessage(
        messages: List<OnlineConsultantMessageUiModel>,
        config: ConsultantWidgetConfig,
        isOffline: Boolean,
    ): Boolean {
        return !isOffline &&
                config.introText?.isNotBlank() == true &&
                messages.none { it.author == OnlineConsultantMessageAuthor.VISITOR }
    }

    private fun mergeHistoryMessages(messages: List<ConsultantMessage>) {
        val uiMessages = messages.mapNotNull { message ->
            toUiMessage(message, widgetConfigMutable.value)
        }
        if (uiMessages.isEmpty()) {
            return
        }

        logger.d(
            "messages update: mergeHistoryMessages incoming=${uiMessages.size} " +
                    "ids=${uiMessages.joinToString(limit = 6) { it.id }} before=${chatMessagesMutable.value.size}",
        )
        chatMessagesMutable.update { existing ->
            val mutable = existing.filterNot { it.isVirtualSupportMessage }.toMutableList()
            uiMessages.forEach { message ->
                val existingIndex = mutable.indexOfFirst { it.id == message.id }
                if (existingIndex >= 0) {
                    mutable[existingIndex] =
                        message.withPreservedResolvedFileLink(mutable[existingIndex])
                } else {
                    mutable += message
                }
            }
            mutable.withVirtualMessages(
                config = widgetConfigMutable.value,
                user = contactFormUser,
                isOffline = offlineStateMutable.value,
                contactFormWasSent = contactFormWasSent,
                offlineFormWasSent = offlineFormWasSent,
            )
        }
        refreshUnreadCount()
    }

    private fun JsonArray?.toInitialActionMessages(
        config: ConsultantWidgetConfig?,
        serverTime: String?,
    ): List<OnlineConsultantMessageUiModel> {
        // Auto-message actions are disabled in the mobile SDK for now.
        // The web widget decides when to show them using browser-only context
        // like tracker events, time on site/page, geoip, scroll position and
        // page-leave signals. The mobile app does not collect that context yet,
        // so showing even "immediate" actions here may diverge from backend/web
        // action semantics. Keep the old mapping code commented out until the
        // mobile action contract is defined.
        return emptyList()

//        if (this == null) {
//            return emptyList()
//        }
//
//        return flatMapIndexed { actionIndex, actionJson ->
//            val action = runCatching {
//                SdkJson.decodeFromJsonElement<ConsultantSiteAction>(actionJson)
//            }.onFailure { throwable ->
//                logger.e("unable to parse connect action", throwable)
//            }.getOrNull() ?: return@flatMapIndexed emptyList()
//
//            action.toImmediateAutoMessages(
//                actionIndex = actionIndex,
//                config = config,
//                serverTime = serverTime,
//            )
//        }
    }

    private fun ConsultantSiteAction.toImmediateAutoMessages(
        actionIndex: Int,
        config: ConsultantWidgetConfig?,
        serverTime: String?,
    ): List<OnlineConsultantMessageUiModel> {
        if (type != SITE_ACTION_TYPE_CHAT_MESSAGE || !isImmediateChatAction) {
            return emptyList()
        }

        val uiButtons = buttons.toUiButtons()
        val quickResponses = buttons.toUiQuickResponses()
        val title = header?.trim().takeUnless { it.isNullOrEmpty() }
        val imageUrl = image?.trim()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        val actionMessageTexts = messages.takeIf { it.isNotEmpty() } ?: listOf("")

        return actionMessageTexts.mapIndexedNotNull { messageIndex, rawText ->
            val text = rawText.trim()
            if (
                text.isBlank() &&
                title.isNullOrBlank() &&
                imageUrl.isNullOrBlank() &&
                uiButtons.isEmpty() &&
                quickResponses.isEmpty()
            ) {
                return@mapIndexedNotNull null
            }

            OnlineConsultantMessageUiModel(
                id = "local-action-" +
                        "${id?.asFlexibleString() ?: actionIndex.toString()}-$messageIndex",
                author = OnlineConsultantMessageAuthor.BOT,
                authorName = resolveManagerAuthorName(config, strings),
                text = text,
                timeLabel = formatMessageTime(serverTime, strings),
                sentAt = serverTime,
                sortKey = serverTime,
                authorAvatarUrl = resolveManagerAvatarUrl(config),
                deliveryStatus = OnlineConsultantMessageDeliveryStatus.DELIVERED,
                autoMessageTitle = title,
                autoMessageImageUrl = imageUrl,
                buttons = uiButtons,
                quickResponses = quickResponses,
            )
        }
    }

    private fun toUiMessage(
        message: ConsultantMessage,
        config: ConsultantWidgetConfig? = widgetConfigMutable.value,
    ): OnlineConsultantMessageUiModel? {
        val id = message.idValue ?: return null
        val imageItem = message.items.firstOrNull {
            message.type == IMAGE_MESSAGE_TYPE &&
                    (!it.imageUrl.isNullOrBlank() || !it.idValue.isNullOrBlank())
        }
        val fileItem = message.items.firstOrNull {
            message.type == FILE_MESSAGE_TYPE &&
                    (!it.fileUrl.isNullOrBlank() || !it.idValue.isNullOrBlank())
        }
        val buttons = message.attachments.toUiButtons()
        val quickResponses = message.attachments.toUiQuickResponses()
        val product = message.product
        val order = message.order
        val text = message.content?.trim().takeUnless { it.isNullOrEmpty() }
            ?: product?.let { "" }
            ?: order?.let { "" }
            ?: fallbackMessageText(message.type, strings)
            ?: imageItem?.let { "" }
            ?: fileItem?.let { "" }
            ?: (buttons + quickResponses).takeIf { it.isNotEmpty() }?.let { "" }
            ?: return null
        val quoteText = message.quoteContent?.trim().takeUnless { it.isNullOrEmpty() }
        val isActionMessage = message.fromMe != true && message.user?.displayName.isNullOrBlank()

        val author = if (message.fromMe == true) {
            OnlineConsultantMessageAuthor.VISITOR
        } else if (!isActionMessage && message.user != null) {
            OnlineConsultantMessageAuthor.CONSULTANT
        } else {
            OnlineConsultantMessageAuthor.BOT
        }

        val authorName = when (author) {
            OnlineConsultantMessageAuthor.VISITOR -> strings.you
            OnlineConsultantMessageAuthor.CONSULTANT -> resolveManagerAuthorName(
                config = config,
                strings = strings,
                fallbackName = message.user?.displayName,
            )

            OnlineConsultantMessageAuthor.BOT -> resolveManagerAuthorName(config, strings)
        }
        val authorAvatarUrl = when (author) {
            OnlineConsultantMessageAuthor.VISITOR -> null
            else -> resolveManagerAvatarUrl(
                config = config,
                fallbackAvatarUrl = message.user?.avatar
            )
        }

        return OnlineConsultantMessageUiModel(
            id = id,
            author = author,
            authorName = authorName,
            text = text,
            timeLabel = formatMessageTime(message.realtimeMarker, strings),
            sentAt = message.realtimeMarker,
            sortKey = message.realtimeMarker,
            authorAvatarUrl = authorAvatarUrl,
            deliveryStatus = mapDeliveryStatus(message.status),
            imageUrl = imageItem?.imageUrl,
            imageWidthPx = imageItem?.width,
            imageHeightPx = imageItem?.height,
            fileUrl = fileItem?.fileUrl,
            fileName = fileItem?.displayFileName ?: fileItem?.fileUrl?.fileNameFromUrl(),
            fileSizeLabel = fileItem?.fileSizeBytes?.formatFileSizeLabel(),
            quoteText = quoteText,
            quoteMessageId = message.quoteMessageIdValue,
            productName = product?.name,
            productUrl = product?.url,
            productImageUrl = product?.img,
            productImageWidthPx = product?.img?.let { DEFAULT_PRODUCT_IMAGE_SIZE_PX },
            productImageHeightPx = product?.img?.let { DEFAULT_PRODUCT_IMAGE_SIZE_PX },
            productPriceLabel = product?.cost?.formatMoneyLabel(),
            productQuantityLabel = product?.quantity?.formatQuantityLabel(product.unit, strings),
            orderNumberLabel = order?.formatOrderNumberLabel(strings),
            orderItems = order?.toUiItems().orEmpty(),
            orderFields = order?.toUiFields(strings).orEmpty(),
            buttons = buttons,
            quickResponses = quickResponses,
        )
    }

    private fun requestMissingFileLinks(
        messages: List<ConsultantMessage>,
        expectedSessionGeneration: Long? = null,
    ) {
        messages.forEach { message ->
            val messageId = message.idValue ?: return@forEach
            val item = message.items.firstOrNull { !it.idValue.isNullOrBlank() } ?: return@forEach
            if (!item.fileUrl.isNullOrBlank()) {
                return@forEach
            }

            val messageType = message.type
            when (messageType) {
                IMAGE_MESSAGE_TYPE,
                FILE_MESSAGE_TYPE -> requestFileLink(
                    messageId = messageId,
                    messageType = messageType,
                    item = item,
                    expectedSessionGeneration = expectedSessionGeneration,
                )
            }
        }
    }

    private fun requestFileLink(
        messageId: String,
        messageType: String,
        item: ConsultantMessageItem,
        expectedSessionGeneration: Long? = null,
    ) {
        val itemId = item.idValue ?: return
        sdkScope.launch {
            val response = runCatching {
                networkEngine.getJson(
                    SdkHttpRequest(
                        url = resolveClientApiUrl("files/$itemId"),
                        headers = buildAuthorizedHttpHeaders(),
                    ),
                )
            }.onFailure { throwable ->
                logger.e("unable to request file link for item $itemId", throwable)
            }.getOrNull() ?: return@launch

            if (!response.isSuccessful) {
                logger.e("file link request failed with HTTP ${response.code}: ${response.body}")
                return@launch
            }

            val parsed = runCatching {
                SdkJson.decodeFromString<ConsultantFileLinkResponse>(response.body)
            }.onFailure { throwable ->
                logger.e("unable to parse file link response: ${response.body}", throwable)
            }.getOrNull() ?: return@launch

            val url = parsed.url?.takeUnless { it.isBlank() } ?: return@launch
            if (
                expectedSessionGeneration != null &&
                !sessionGeneration.isCurrent(expectedSessionGeneration)
            ) {
                return@launch
            }
            updateUiMessageFileLink(
                messageId = messageId,
                messageType = messageType,
                url = url,
            )
        }
    }

    private fun updateUiMessageFileLink(
        messageId: String,
        messageType: String,
        url: String,
    ) {
        logger.d("messages update: updateUiMessageFileLink id=$messageId type=$messageType")
        chatMessagesMutable.update { existing ->
            existing.map { message ->
                if (message.id != messageId) {
                    message
                } else {
                    when (messageType) {
                        IMAGE_MESSAGE_TYPE -> message.copy(imageUrl = url)
                        FILE_MESSAGE_TYPE -> message.copy(fileUrl = url)
                        else -> message
                    }
                }
            }
        }
    }

    private fun upsertUiMessage(message: OnlineConsultantMessageUiModel) {
        chatMessagesMutable.update { existing ->
            val mutable = existing.filterNot { it.isVirtualSupportMessage }.toMutableList()
            val existingIndex = mutable.indexOfFirst { it.id == message.id }
            logger.d(
                "messages update: upsertUiMessage id=${message.id} existingIndex=$existingIndex " +
                        "before=${existing.size} realBefore=${mutable.size} " +
                        "imageUrl=${message.imageUrl != null} fileUrl=${message.fileUrl != null}",
            )
            if (existingIndex >= 0) {
                mutable[existingIndex] =
                    message.withPreservedResolvedFileLink(mutable[existingIndex])
            } else {
                mutable += message
            }
            val result = mutable.sortedForDisplay()
            logger.d(
                "messages update: upsertUiMessage result size=${result.size} " +
                        "ids=${result.joinToString(limit = 6) { it.id }}",
            )
            result
        }
        refreshUnreadCount()
    }

    private fun replaceUiMessage(
        oldId: String,
        newMessage: OnlineConsultantMessageUiModel,
    ) {
        logger.d("messages update: replaceUiMessage oldId=$oldId newId=${newMessage.id}")
        chatMessagesMutable.update { existing ->
            val mutable = existing.filterNot { it.isVirtualSupportMessage }.toMutableList()
            val existingIndex = mutable.indexOfFirst { it.id == oldId }
            if (existingIndex >= 0) {
                mutable[existingIndex] =
                    newMessage.withPreservedResolvedFileLink(mutable[existingIndex])
            } else {
                val byServerId = mutable.indexOfFirst { it.id == newMessage.id }
                if (byServerId >= 0) {
                    mutable[byServerId] =
                        newMessage.withPreservedResolvedFileLink(mutable[byServerId])
                } else {
                    mutable += newMessage
                }
            }
            mutable.sortedForDisplay()
        }
        refreshUnreadCount()
    }

    private fun OnlineConsultantMessageUiModel.withPreservedResolvedFileLink(
        existing: OnlineConsultantMessageUiModel?,
    ): OnlineConsultantMessageUiModel {
        if (existing == null) {
            return this
        }

        return copy(
            imageUrl = imageUrl ?: existing.imageUrl,
            fileUrl = fileUrl ?: existing.fileUrl,
            fileName = fileName ?: existing.fileName,
            fileSizeLabel = fileSizeLabel ?: existing.fileSizeLabel,
        )
    }

    private fun updateContactFormState(
        kind: ConsultantContactFormKind,
        state: ConsultantContactFormState,
    ) {
        chatMessagesMutable.update { messages ->
            messages.map { message ->
                val form = message.contactForm
                if (form?.kind == kind) {
                    message.copy(contactForm = form.copy(state = state))
                } else {
                    message
                }
            }
        }
    }

    private fun updateUiMessageStatus(
        id: String,
        status: OnlineConsultantMessageDeliveryStatus,
    ) {
        chatMessagesMutable.update { existing ->
            existing.map { message ->
                if (message.id == id) {
                    message.copy(deliveryStatus = status)
                } else {
                    message
                }
            }
        }
        refreshUnreadCount()
    }

    private fun markIncomingMessagesReadUpTo(messageId: Long) {
        chatMessagesMutable.update { existing ->
            existing.map { message ->
                val currentMessageId = message.id.toLongOrNull()
                if (
                    currentMessageId != null &&
                    currentMessageId <= messageId &&
                    message.isIncomingRealMessage &&
                    message.deliveryStatus != OnlineConsultantMessageDeliveryStatus.READ
                ) {
                    message.copy(deliveryStatus = OnlineConsultantMessageDeliveryStatus.READ)
                } else {
                    message
                }
            }
        }
        refreshUnreadCount()
    }

    private fun markIncomingMessageRead(messageId: String) {
        chatMessagesMutable.update { existing ->
            existing.map { message ->
                if (
                    message.id == messageId &&
                    message.isIncomingRealMessage &&
                    message.deliveryStatus != OnlineConsultantMessageDeliveryStatus.READ
                ) {
                    message.copy(deliveryStatus = OnlineConsultantMessageDeliveryStatus.READ)
                } else {
                    message
                }
            }
        }
        refreshUnreadCount()
    }

    private fun markIncomingMessagesReadLocally() {
        chatMessagesMutable.update { existing ->
            existing.map { message ->
                if (
                    message.isIncomingRealMessage &&
                    message.deliveryStatus != OnlineConsultantMessageDeliveryStatus.READ
                ) {
                    message.copy(deliveryStatus = OnlineConsultantMessageDeliveryStatus.READ)
                } else {
                    message
                }
            }
        }
        refreshUnreadCount()
    }

    private fun refreshUnreadCount() {
        val config = widgetConfigMutable.value
        unreadCountMutable.value = if (config?.useUnreadCounter == false) {
            0
        } else {
            chatMessagesMutable.value.count { message ->
                message.isIncomingRealMessage &&
                        message.deliveryStatus != OnlineConsultantMessageDeliveryStatus.READ
            }
        }
    }

    private suspend fun prepareMessageRead(messageId: String): Long? {
        val numericMessageId = messageId.toLongOrNull()
        if (numericMessageId == null) {
            markIncomingMessageRead(messageId)
            return null
        }
        val shouldMarkRead = stateMutex.withLock {
            if (numericMessageId <= lastReadMessageId) {
                false
            } else {
                lastReadMessageId = numericMessageId
                true
            }
        }
        if (!shouldMarkRead) {
            return null
        }

        markIncomingMessagesReadUpTo(numericMessageId)
        return numericMessageId
    }

    private suspend fun postMessageRead(messageId: Long) {
        val response = networkEngine.postJson(
            SdkHttpRequest(
                url = resolveClientApiUrl("messages/$messageId/read"),
                headers = buildAuthorizedHttpHeaders(),
            ),
        )
        if (!response.isSuccessful) {
            throw sdkHttpException(strings.get(R.string.crm_consultant_mark_read_error), response)
        }
    }

    private fun shouldEmitIncomingMessageNotification(
        message: OnlineConsultantMessageUiModel,
    ): Boolean {
        return widgetConfigMutable.value?.useSoundAlerts == true && message.isIncomingRealMessage
    }

    private fun refreshRealtimeMarker(
        marker: String?,
        expectedSessionGeneration: Long? = null,
    ) {
        sdkScope.launch {
            stateMutex.withLock {
                if (
                    expectedSessionGeneration != null &&
                    !sessionGeneration.isCurrent(expectedSessionGeneration)
                ) {
                    return@withLock
                }
                if (!marker.isNullOrBlank()) {
                    lastWebSocketUpdatedAt = latestRealtimeMarker(lastWebSocketUpdatedAt, marker)
                }
            }
        }
    }

    private fun ConsultantWidgetConfig.toContactFormMessage(
        user: ConsultantContactFormRequest,
    ): OnlineConsultantMessageUiModel {
        return OnlineConsultantMessageUiModel(
            id = CONTACT_FORM_MESSAGE_ID,
            author = OnlineConsultantMessageAuthor.BOT,
            authorName = resolveManagerAuthorName(this, strings),
            text = contactFormText?.trim().takeUnless { it.isNullOrEmpty() }
                ?: strings.get(R.string.crm_consultant_contact_form_default_text),
            timeLabel = "",
            deliveryStatus = OnlineConsultantMessageDeliveryStatus.NONE,
            authorAvatarUrl = resolveManagerAvatarUrl(this),
            contactForm = ConsultantContactFormUiModel(
                config = contactForm,
                user = user,
                kind = ConsultantContactFormKind.CONTACT,
                state = ConsultantContactFormState.START,
                personalDataConsent = personalDataConsent,
                personalDataLink = personalDataLink,
            ),
        )
    }

    private fun ConsultantWidgetConfig.toIntroMessage(): OnlineConsultantMessageUiModel {
        return OnlineConsultantMessageUiModel(
            id = INTRO_MESSAGE_ID,
            author = OnlineConsultantMessageAuthor.BOT,
            authorName = resolveManagerAuthorName(this, strings),
            text = introText?.trim().orEmpty(),
            timeLabel = "",
            deliveryStatus = OnlineConsultantMessageDeliveryStatus.NONE,
            authorAvatarUrl = resolveManagerAvatarUrl(this),
        )
    }

    private fun ConsultantWidgetConfig.toOfflineMessage(
        user: ConsultantContactFormRequest,
        wasSent: Boolean,
    ): OnlineConsultantMessageUiModel {
        val resultText = offlineFormResultText?.trim().takeUnless { it.isNullOrEmpty() }
            ?: strings.get(R.string.crm_consultant_offline_form_default_result)
        return OnlineConsultantMessageUiModel(
            id = OFFLINE_MESSAGE_ID,
            author = OnlineConsultantMessageAuthor.BOT,
            authorName = resolveManagerAuthorName(this, strings),
            text = offlineText?.trim().takeUnless { it.isNullOrEmpty() }
                ?: strings.get(R.string.crm_consultant_offline_form_default_text),
            timeLabel = "",
            deliveryStatus = OnlineConsultantMessageDeliveryStatus.NONE,
            authorAvatarUrl = resolveManagerAvatarUrl(this),
            contactForm = offlineForm.takeIf { it.hasVisibleFields }?.let {
                ConsultantContactFormUiModel(
                    config = it,
                    user = user,
                    kind = ConsultantContactFormKind.OFFLINE,
                    state = if (wasSent) {
                        ConsultantContactFormState.SENT
                    } else {
                        ConsultantContactFormState.START
                    },
                    resultText = resultText,
                    personalDataConsent = personalDataConsent,
                    personalDataLink = personalDataLink,
                )
            },
        )
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val MESSAGE_EVENT_TYPE = "message"
        private const val OFFLINE_STATUS_EVENT_TYPE = "offline_status"
        private const val SITE_ACTION_TYPE_CHAT_MESSAGE = "chat_message"
        private const val TEXT_MESSAGE_TYPE = "text"
        private const val IMAGE_MESSAGE_TYPE = "image"
        private const val FILE_MESSAGE_TYPE = "file"
        private const val LOCAL_MESSAGE_ID_PREFIX = "local-"
        private const val INTRO_MESSAGE_ID = "local-intro"
        private const val OFFLINE_MESSAGE_ID = "local-offline"
        private const val CONTACT_FORM_MESSAGE_ID = "local-contact-form"
        private const val DEFAULT_PRODUCT_IMAGE_SIZE_PX = 80
        private const val MAX_IMAGE_UPLOAD_BYTES = 3 * 1024 * 1024L
        private const val MAX_FILE_UPLOAD_BYTES = 5 * 1024 * 1024L
        private const val DEFAULT_UPLOAD_BUFFER_SIZE = 8 * 1024
        private const val READ_MESSAGE_DEBOUNCE_MS = 500L
        private const val DEFAULT_MESSAGES_PAGE_SIZE = 50
        private const val AUTO_RECONNECT_WEBSOCKET = true
        private const val WEBSOCKET_RECONNECT_DELAY_MS = 3_000L

    }
}

internal fun buildSdkWebSocketRequest(
    url: String,
    session: SdkSession,
    updatedAt: String?,
): SdkWebSocketRequest = SdkWebSocketRequest(
    url = if (updatedAt.isNullOrBlank()) {
        url
    } else {
        url.withQueryParameters("updated_at" to updatedAt)
    },
    headers = mapOf(
        "X-User-Token" to session.userToken,
        "X-Session-Id" to session.sessionId,
    ),
)
