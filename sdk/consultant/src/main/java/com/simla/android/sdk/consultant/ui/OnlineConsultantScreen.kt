package com.simla.android.sdk.consultant.ui

import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.CrmSdk
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.consultantController
import com.simla.android.sdk.consultant.controller.OnlineConsultantController
import com.simla.android.sdk.consultant.controller.OnlineConsultantControllerEvent
import com.simla.android.sdk.consultant.controller.OnlineConsultantControllerState
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.models.widget.WIDGET_BACKGROUND_DARK
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.storage.ConsultantSharedPreferencesDownloadedFileRegistry
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import com.simla.android.sdk.consultant.ui.models.debugMessageIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun OnlineConsultantChatScreen(
    controller: OnlineConsultantController,
    modifier: Modifier = Modifier,
    messageListState: OnlineConsultantMessageListState = rememberOnlineConsultantMessageListState(
        controller
    ),
    onAttachClick: (() -> Unit)? = null,
    onError: (Throwable) -> Unit = {},
) {
    val state by controller.state.collectAsState()
    val lastError by controller.lastError.collectAsState()
    val loadingWidgetConfig by controller.sdk.widgetConfigState.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var lastShownInitialErrorSignature by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) {
            controller.sdk.logDebug("file picker result is empty")
            return@rememberLauncherForActivityResult
        }

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

    LaunchedEffect(controller) {
        controller.start()
    }

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is OnlineConsultantControllerEvent.MessageSendFailed -> {
                    Toast.makeText(
                        context,
                        formatMessageSendErrorToast(context, event.throwable),
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
                    controller.sdk.logError("initial consultant connect failed", event.throwable)
                    onError(event.throwable)
                }
            }
        }
    }

    LaunchedEffect(lastError) {
        val throwable = lastError ?: return@LaunchedEffect
        val signature = throwable.uiErrorSignature()
        if (lastShownInitialErrorSignature == signature) {
            return@LaunchedEffect
        }

        lastShownInitialErrorSignature = signature
        Toast.makeText(
            context,
            formatInitialConnectErrorToast(context, throwable),
            Toast.LENGTH_SHORT,
        ).show()
    }

    val readyState = state as? OnlineConsultantControllerState.Ready
    OnlineConsultantScreen(
        modifier = modifier,
        messages = readyState?.messages.orEmpty(),
        inputText = inputText,
        onInputTextChange = { inputText = it },
        onAttachClick = onAttachClick ?: {
            controller.sdk.logDebug("file picker launch requested")
            filePicker.launch(arrayOf("*/*"))
        },
        downloadedFileRegistry = controller.downloadedFileRegistry,
        isInitialLoading = state is OnlineConsultantControllerState.Idle ||
                state is OnlineConsultantControllerState.InitialLoading ||
                readyState?.isInitialLoading == true,
        isLoadingOlderMessages = readyState?.isLoadingOlderMessages == true,
        canLoadOlderMessages = readyState?.canLoadOlderMessages == true,
        onLoadOlderMessages = { controller.loadOlderMessages() },
        onContactFormSubmit = { kind, data -> controller.submitContactForm(kind, data) },
        onMessageVisible = { message -> controller.markMessageVisible(message.id) },
        onQuickResponseClick = { title -> controller.sendMessage(title) },
        onSendClick = {
            val messageText = inputText.trim()
            if (messageText.isEmpty()) {
                return@OnlineConsultantScreen
            }

            controller.sendMessage(messageText)
            inputText = ""
        },
        dayLabel = stringResource(R.string.crm_consultant_today),
        // /connect publishes the widget theme before the websocket subscription and history
        // loading complete. Use it immediately so the initial loader does not keep rendering
        // with the fallback blue theme and then switch to the consultant theme.
        widgetConfig = readyState?.widgetConfig ?: loadingWidgetConfig,
        isOffline = readyState?.isOffline == true,
        isAvailable = state !is OnlineConsultantControllerState.Unavailable,
        messageListState = messageListState,
    )
}

@Composable
fun OnlineConsultantChatScreen(
    sdk: CrmSdk,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    OnlineConsultantChatScreen(
        controller = sdk.consultantController(),
        modifier = modifier,
        onError = onError,
    )
}

@Composable
internal fun OnlineConsultantScreen(
    messages: List<OnlineConsultantMessageUiModel>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    dayLabel: String = stringResource(R.string.crm_consultant_today),
    onMessageLayoutClick: ((String) -> Unit)? = null,
    downloadedFileRegistry: ConsultantDownloadedFileRegistry? = null,
    isInitialLoading: Boolean = false,
    isLoadingOlderMessages: Boolean = false,
    canLoadOlderMessages: Boolean = false,
    onLoadOlderMessages: () -> Unit = {},
    onContactFormSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit = { _, _ -> },
    onMessageVisible: (OnlineConsultantMessageUiModel) -> Unit = {},
    onQuickResponseClick: (String) -> Unit = {},
    widgetConfig: ConsultantWidgetConfig? = null,
    isOffline: Boolean = false,
    isAvailable: Boolean = true,
    messageListState: OnlineConsultantMessageListState = rememberOnlineConsultantMessageListState(),
) {
    OnlineConsultantLayout(
        messages = messages,
        inputText = inputText,
        onInputTextChange = onInputTextChange,
        onAttachClick = onAttachClick,
        onSendClick = onSendClick,
        modifier = modifier,
        dayLabel = dayLabel,
        onMessageLayoutClick = onMessageLayoutClick,
        downloadedFileRegistry = downloadedFileRegistry,
        isInitialLoading = isInitialLoading,
        isLoadingOlderMessages = isLoadingOlderMessages,
        canLoadOlderMessages = canLoadOlderMessages,
        onLoadOlderMessages = onLoadOlderMessages,
        onContactFormSubmit = onContactFormSubmit,
        onMessageVisible = onMessageVisible,
        onQuickResponseClick = onQuickResponseClick,
        widgetConfig = widgetConfig,
        isOffline = isOffline,
        isAvailable = isAvailable,
        messageListState = messageListState,
        showComposer = true,
    )
}

@Composable
internal fun OnlineConsultantLayout(
    messages: List<OnlineConsultantMessageUiModel>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    dayLabel: String = stringResource(R.string.crm_consultant_today),
    onMessageLayoutClick: ((String) -> Unit)? = null,
    downloadedFileRegistry: ConsultantDownloadedFileRegistry? = null,
    isInitialLoading: Boolean = false,
    isLoadingOlderMessages: Boolean = false,
    canLoadOlderMessages: Boolean = false,
    onLoadOlderMessages: () -> Unit = {},
    onContactFormSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit = { _, _ -> },
    onMessageVisible: (OnlineConsultantMessageUiModel) -> Unit = {},
    onQuickResponseClick: (String) -> Unit = {},
    widgetConfig: ConsultantWidgetConfig? = null,
    isOffline: Boolean = false,
    isAvailable: Boolean = true,
    messageListState: OnlineConsultantMessageListState,
    showComposer: Boolean,
) {
    ConsultantRecompositionLog("OnlineConsultantLayout") {
        "messages=${messages.size} ids=${messages.debugMessageIds()} " +
                "initialLoading=$isInitialLoading olderLoading=$isLoadingOlderMessages " +
                "canLoadOlder=$canLoadOlderMessages offline=$isOffline"
    }
    val listState = messageListState.lazyListState
    val context = LocalContext.current
    val resolvedDownloadedFileRegistry = remember(context, downloadedFileRegistry) {
        downloadedFileRegistry ?: ConsultantSharedPreferencesDownloadedFileRegistry(
            context.applicationContext,
        )
    }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedFileMessage by remember { mutableStateOf<OnlineConsultantMessageUiModel?>(null) }
    var pendingQuoteScroll by remember { mutableStateOf<ConsultantPendingQuoteScroll?>(null) }
    var quoteHighlight by remember { mutableStateOf<ConsultantQuoteHighlight?>(null) }
    var lastAutoLoadAnchorMessageId by remember { mutableStateOf<String?>(null) }
    val currentOnLoadOlderMessages by rememberUpdatedState(onLoadOlderMessages)
    val currentCanLoadOlderMessages by rememberUpdatedState(canLoadOlderMessages)
    val currentIsLoadingOlderMessages by rememberUpdatedState(isLoadingOlderMessages)
    val currentIsInitialLoading by rememberUpdatedState(isInitialLoading)
    val showInitialLoading = rememberMinimumVisibleLoading(isInitialLoading)
    val showOlderMessagesLoading = rememberMinimumVisibleLoading(isLoadingOlderMessages)
    val todayLabel = stringResource(R.string.crm_consultant_today)
    val yesterdayLabel = stringResource(R.string.crm_consultant_yesterday)
    val nextListItems = remember(messages, todayLabel, yesterdayLabel) {
        ConsultantUiDebugLog.d(
            "buildMessageListItems requested messages=${messages.size} ids=${messages.debugMessageIds()}",
        )
        buildMessageListItems(messages, todayLabel, yesterdayLabel)
    }
    val displayedListItems = remember { mutableStateListOf<ConsultantChatListItem>() }
    var newerMessageGeneration by remember { mutableIntStateOf(0) }
    val listTransformProgress = remember { Animatable(1f) }
    val rootBoundsInWindow = remember {
        arrayOfNulls<androidx.compose.ui.geometry.Rect>(1)
    }
    val listBoundsInWindow = remember {
        arrayOfNulls<androidx.compose.ui.geometry.Rect>(1)
    }
    SideEffect {
        val incoming = nextListItems.asReversed()
        if (incoming.hasNewerMessageThan(displayedListItems)) {
            ConsultantUiDebugLog.d(
                "newer message detected first=${listState.firstVisibleItemIndex}:" +
                        "${listState.firstVisibleItemScrollOffset} " +
                        "scrolling=${listState.isScrollInProgress} " +
                        "resetGeneration=${newerMessageGeneration + 1}",
            )
            newerMessageGeneration += 1
        }
        Snapshot.withMutableSnapshot {
            displayedListItems.reconcileByKey(incoming)
        }
    }
    LaunchedEffect(newerMessageGeneration, listState) {
        if (newerMessageGeneration == 0) {
            return@LaunchedEffect
        }
        if (listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        ) {
            ConsultantUiDebugLog.d(
                "list reset skipped generation=$newerMessageGeneration: already at newest edge",
            )
            return@LaunchedEffect
        }

        ConsultantUiDebugLog.d(
            "list reset start generation=$newerMessageGeneration " +
                    "first=${listState.firstVisibleItemIndex}:" +
                    "${listState.firstVisibleItemScrollOffset} " +
                    "scrolling=${listState.isScrollInProgress}",
        )
        listTransformProgress.snapTo(0f)
        listState.requestScrollToItem(0)
        withFrameNanos { }
        listTransformProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LIST_RESET_TRANSFORM_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        ConsultantUiDebugLog.d(
            "list reset end generation=$newerMessageGeneration " +
                    "first=${listState.firstVisibleItemIndex}:" +
                    "${listState.firstVisibleItemScrollOffset}",
        )
    }

    if (ConsultantUiDebugLog.isEnabled) {
        LaunchedEffect(listState) {
            var scrollTraceId = 0
            var scrollStartedAtMs = 0L
            var wasScrolling = false
            val frames = ArrayDeque<Pair<Long, String>>()
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val firstVisibleItem = visibleItems.firstOrNull()
                val lastVisibleItem = visibleItems.lastOrNull()
                val state =
                    "first=${listState.firstVisibleItemIndex}:" +
                            "${listState.firstVisibleItemScrollOffset} " +
                            "anchor=${firstVisibleItem?.key}@${firstVisibleItem?.offset} " +
                            "last=${lastVisibleItem?.index}:${lastVisibleItem?.key}@" +
                            "${lastVisibleItem?.offset} visible=${visibleItems.size}/" +
                            "${layoutInfo.totalItemsCount} viewport=" +
                            "${layoutInfo.viewportStartOffset}..${layoutInfo.viewportEndOffset} " +
                            "canBack=${listState.canScrollBackward} " +
                            "canForward=${listState.canScrollForward}"
                listState.isScrollInProgress to state
            }
                .distinctUntilChanged()
                .collect { (isScrolling, state) ->
                    val nowMs = SystemClock.uptimeMillis()
                    if (isScrolling) {
                        if (!wasScrolling) {
                            scrollTraceId += 1
                            scrollStartedAtMs = nowMs
                            frames.clear()
                            ConsultantUiDebugLog.d("scroll#$scrollTraceId start $state")
                        }
                        if (frames.size == 240) {
                            frames.removeFirst()
                        }
                        frames.addLast(nowMs - scrollStartedAtMs to state)
                    } else if (wasScrolling) {
                        val completedTraceId = scrollTraceId
                        val completedFrames = frames.toList()
                        ConsultantUiDebugLog.d(
                            "scroll#$completedTraceId end " +
                                    "duration=${nowMs - scrollStartedAtMs}ms " +
                                    "frames=${completedFrames.size} $state",
                        )
                        launch(Dispatchers.Default) {
                            completedFrames.chunked(12).forEachIndexed { index, chunk ->
                                ConsultantUiDebugLog.d(
                                    "scroll#$completedTraceId trace[$index] " +
                                            chunk.joinToString(" | ") { (elapsedMs, frame) ->
                                                "t=${elapsedMs}ms $frame"
                                            },
                                )
                            }
                        }
                        frames.clear()
                    } else {
                        ConsultantUiDebugLog.d("layout state $state")
                    }
                    wasScrolling = isScrolling
                }
        }
    }
    ConsultantRecompositionLog("OnlineConsultantMessageListItems") {
        "items=${displayedListItems.size} keys=${displayedListItems.debugListItemKeys()}"
    }
    val shouldHideComposer =
        remember(messages, isOffline) {
            shouldHideComposerForBlockingContactForm(
                messages,
                isOffline
            )
        }
    val showPersonalDataWarning = remember(widgetConfig, messages) {
        shouldShowPersonalDataWarning(widgetConfig, messages)
    }
    val listItemsStartIndex = 1

    LaunchedEffect(
        pendingQuoteScroll,
        displayedListItems,
        listItemsStartIndex,
        canLoadOlderMessages,
        isLoadingOlderMessages,
        isInitialLoading,
    ) {
        val pending = pendingQuoteScroll ?: return@LaunchedEffect
        val targetIndex = findQuotedMessageListItemIndex(displayedListItems, pending)
        if (targetIndex != null) {
            listState.animateScrollToItem(listItemsStartIndex + targetIndex)
            val targetMessage =
                (displayedListItems[targetIndex] as? ConsultantChatListItem.Message)?.message
            if (targetMessage != null) {
                quoteHighlight = ConsultantQuoteHighlight(
                    messageId = targetMessage.id,
                    nonce = (quoteHighlight?.nonce ?: 0) + 1,
                )
            }
            pendingQuoteScroll = null
            return@LaunchedEffect
        }

        if (canLoadOlderMessages && !isLoadingOlderMessages && !isInitialLoading) {
            currentOnLoadOlderMessages()
        } else if (!canLoadOlderMessages && !isLoadingOlderMessages) {
            pendingQuoteScroll = null
        }
    }

    LaunchedEffect(listState, displayedListItems, listItemsStartIndex) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { visibleItem ->
                    val itemIndex = visibleItem.index - listItemsStartIndex
                    (displayedListItems.getOrNull(itemIndex) as? ConsultantChatListItem.Message)?.message
                }
                .filter { message ->
                    message.author != OnlineConsultantMessageAuthor.VISITOR &&
                            message.deliveryStatus != OnlineConsultantMessageDeliveryStatus.READ
                }
                .maxByOrNull { message -> message.id.toLongOrNull() ?: 0L }
        }
            .distinctUntilChanged()
            .collect { message ->
                if (message != null) {
                    onMessageVisible(message)
                }
            }
    }

    LaunchedEffect(listState, displayedListItems, listItemsStartIndex) {
        snapshotFlow {
            val oldestVisibleMessageId = listState.layoutInfo.visibleItemsInfo
                .asReversed()
                .firstNotNullOfOrNull { visibleItem ->
                    val itemIndex = visibleItem.index - listItemsStartIndex
                    (displayedListItems.getOrNull(itemIndex) as? ConsultantChatListItem.Message)
                        ?.message
                        ?.id
                }
            listState.isScrolledToOlderMessagesEdge() to oldestVisibleMessageId
        }
            .distinctUntilChanged()
            .collect { (shouldLoadOlder, oldestVisibleMessageId) ->
                if (shouldAutoLoadOlderMessages(
                        isAtOlderMessagesEdge = shouldLoadOlder,
                        oldestVisibleMessageId = oldestVisibleMessageId,
                        lastAutoLoadAnchorMessageId = lastAutoLoadAnchorMessageId,
                        canLoadOlderMessages = currentCanLoadOlderMessages,
                        isLoadingOlderMessages = currentIsLoadingOlderMessages,
                        isInitialLoading = currentIsInitialLoading,
                    )
                ) {
                    lastAutoLoadAnchorMessageId = oldestVisibleMessageId
                    currentOnLoadOlderMessages()
                }
            }
    }

    val initialLoadingColor = initialLoadingIndicatorColor(
        widgetConfig = widgetConfig,
        hostPrimaryColor = MaterialTheme.colorScheme.primary,
    )
    CompositionLocalProvider(LocalCrmVisualTheme provides widgetConfig.toVisualTheme()) {
        val visualTheme = LocalCrmVisualTheme.current
        val density = LocalDensity.current
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val navigationBottomPx = WindowInsets.navigationBars.getBottom(density)
        SideEffect {
            ConsultantUiDebugLog.d(
                "insets imeBottom=$imeBottomPx navigationBottom=$navigationBottomPx",
            )
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    if (rootBoundsInWindow[0] != bounds) {
                        ConsultantUiDebugLog.d(
                            "bounds root changed ${rootBoundsInWindow[0]?.toDebugString()}" +
                                    " -> ${bounds.toDebugString()}",
                        )
                        rootBoundsInWindow[0] = bounds
                    }
                    if (imeBottomPx > 0 && bounds.top < 0f) {
                        ConsultantUiDebugLog.d(
                            "IME layout warning: root is shifted above window; " +
                                    "host Activity must use windowSoftInputMode=adjustResize",
                        )
                    }
                },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = visualTheme.screenBackground,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    if (showPersonalDataWarning) {
                        PersonalDataWarning(
                            link = widgetConfig?.personalDataLink.orEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val messageBubbleMaxWidth =
                            ((maxWidth - 32.dp).coerceAtLeast(0.dp) * MAX_BUBBLE_WIDTH_FRACTION)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(visualTheme.screenBackground)
                                .padding(horizontal = 16.dp)
                                .graphicsLayer {
                                    alpha = 0.78f + 0.22f * listTransformProgress.value
                                    translationY =
                                        (1f - listTransformProgress.value) *
                                                LIST_RESET_TRANSLATION_DP.dp.toPx()
                                }
                                .onGloballyPositioned { coordinates ->
                                    val bounds = coordinates.boundsInWindow()
                                    if (listBoundsInWindow[0] != bounds) {
                                        ConsultantUiDebugLog.d(
                                            "bounds list changed " +
                                                    "${listBoundsInWindow[0]?.toDebugString()}" +
                                                    " -> ${bounds.toDebugString()}",
                                        )
                                        listBoundsInWindow[0] = bounds
                                    }
                                },
                            state = listState,
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                        ) {
                        item(key = "bottom-spacer") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        itemsIndexed(
                            items = displayedListItems,
                            key = { _, item -> item.key },
                            contentType = { _, item -> item::class },
                        ) { _, item ->
                            ConsultantRecompositionLog("LazyColumn.item") {
                                when (item) {
                                    is ConsultantChatListItem.DateSeparator ->
                                        "key=${item.key} type=date label=${item.label}"

                                    is ConsultantChatListItem.Message ->
                                        "key=${item.key} type=message id=${item.message.id} " +
                                                "status=${item.message.deliveryStatus} textHash=${item.message.text.hashCode()}"
                                }
                            }
                            when (item) {
                                is ConsultantChatListItem.DateSeparator -> ConsultantDateSeparator(
                                    item.label
                                )

                                is ConsultantChatListItem.Message -> Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec = tween(durationMillis = 150),
                                            // The list can snap to the newest item while IME resizes
                                            // the viewport. Placement animation would then use stale
                                            // item offsets and could draw messages over date separators.
                                            placementSpec = null,
                                            fadeOutSpec = null,
                                        ),
                                ) {
                                    OnlineConsultantMessageBubble(
                                        message = item.message,
                                        maxBubbleWidth = messageBubbleMaxWidth,
                                        imageDimensionsByUrl = messageListState.imageDimensionsByUrl,
                                        isFollowFromPrevious = isFollowMessage(
                                            item.previousMessage,
                                            item.message,
                                        ),
                                        isFollowToNext = item.nextMessage
                                            ?.let { isFollowMessage(item.message, it) }
                                            ?: false,
                                        onLayoutClick = onMessageLayoutClick,
                                        onImageClick = { imageUrl ->
                                            fullscreenImageUrl = imageUrl
                                        },
                                        onFileIconClick = { message ->
                                            selectedFileMessage = message
                                        },
                                        highlightNonce = quoteHighlight
                                            ?.takeIf { it.messageId == item.message.id }
                                            ?.nonce,
                                        onHighlightConsumed = { nonce ->
                                            if (quoteHighlight?.messageId == item.message.id &&
                                                quoteHighlight?.nonce == nonce
                                            ) {
                                                quoteHighlight = null
                                            }
                                        },
                                        onQuoteClick = { message ->
                                            pendingQuoteScroll = ConsultantPendingQuoteScroll(
                                                targetMessageId = message.quoteMessageId,
                                                quoteText = message.quoteText,
                                                sourceMessageId = message.id,
                                            )
                                        },
                                        onContactFormSubmit = onContactFormSubmit,
                                        onQuickResponseClick = onQuickResponseClick,
                                    )
                                }
                            }
                        }

                        if (showOlderMessagesLoading) {
                            item(key = "older-messages-loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = visualTheme.brandColor,
                                    )
                                }
                            }
                        }
                        }
                    }

                    if (isAvailable && showComposer && !shouldHideComposer) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                ConsultantUiDebugLog.d(
                                    "bounds composer=${
                                        coordinates.boundsInWindow().toDebugString()
                                    }",
                                )
                            },
                        ) {
                            OnlineConsultantComposer(
                                inputText = inputText,
                                onInputTextChange = onInputTextChange,
                                onAttachClick = onAttachClick,
                                onSendClick = onSendClick,
                            )
                        }
                    }

                    widgetConfig?.domainConfig
                        ?.takeIf { isAvailable && it.footer }
                        ?.let { domainConfig ->
                            ConsultantFooterBranding(
                                domainConfig = domainConfig,
                                darkTheme = widgetConfig.backgroundTheme == WIDGET_BACKGROUND_DARK,
                            )
                        }

                }
            }

            if (!isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(visualTheme.screenBackground)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.crm_consultant_widget_disabled_error),
                        color = visualTheme.secondaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            FullscreenImageViewer(
                imageUrl = fullscreenImageUrl,
                onClose = { fullscreenImageUrl = null },
            )

            val fileMessage = selectedFileMessage
            if (fileMessage != null) {
                FileMessageActionsSheet(
                    message = fileMessage,
                    downloadedFileRegistry = resolvedDownloadedFileRegistry,
                    onDismiss = { selectedFileMessage = null },
                    onError = { throwable ->
                        Toast.makeText(
                            context,
                            throwable.message
                                ?: context.getString(R.string.crm_consultant_handle_file_error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }

            if (showInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(visualTheme.screenBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = initialLoadingColor)
                }
            }
        }
    }
}

private const val LIST_RESET_TRANSFORM_DURATION_MS = 180
private const val LIST_RESET_TRANSLATION_DP = 16

private fun androidx.compose.ui.geometry.Rect.toDebugString(): String =
    "(${left.toInt()},${top.toInt()})-(${right.toInt()},${bottom.toInt()})"
