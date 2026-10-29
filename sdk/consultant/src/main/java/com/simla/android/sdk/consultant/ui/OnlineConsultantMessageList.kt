package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.storage.ConsultantDownloadedFileRegistry
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun OnlineConsultantMessageList(
    messages: List<OnlineConsultantMessageUiModel>,
    modifier: Modifier = Modifier,
    dayLabel: String = stringResource(R.string.crm_consultant_today),
    onMessageLayoutClick: ((String) -> Unit)? = null,
    downloadedFileRegistry: ConsultantDownloadedFileRegistry? = null,
    isLoadingOlderMessages: Boolean = false,
    canLoadOlderMessages: Boolean = false,
    onLoadOlderMessages: () -> Unit = {},
    onMessageVisible: (OnlineConsultantMessageUiModel) -> Unit = {},
    onQuickResponseClick: (String) -> Unit = {},
    widgetConfig: ConsultantWidgetConfig? = null,
    isOffline: Boolean = false,
    isAvailable: Boolean = true,
    messageListState: OnlineConsultantMessageListState = rememberOnlineConsultantMessageListState(),
) {
    OnlineConsultantLayout(
        messages = messages,
        inputText = "",
        onInputTextChange = {},
        onAttachClick = {},
        onSendClick = {},
        modifier = modifier,
        dayLabel = dayLabel,
        onMessageLayoutClick = onMessageLayoutClick,
        downloadedFileRegistry = downloadedFileRegistry,
        isInitialLoading = false,
        isLoadingOlderMessages = isLoadingOlderMessages,
        canLoadOlderMessages = canLoadOlderMessages,
        onLoadOlderMessages = onLoadOlderMessages,
        onContactFormSubmit = { _, _ -> },
        onMessageVisible = onMessageVisible,
        onQuickResponseClick = onQuickResponseClick,
        widgetConfig = widgetConfig,
        isOffline = isOffline,
        isAvailable = isAvailable,
        messageListState = messageListState,
        showComposer = false,
    )
}

@Composable
internal fun rememberOnlineConsultantMessageListState(): OnlineConsultantMessageListState {
    return rememberSaveable(saver = OnlineConsultantMessageListState.Saver) {
        OnlineConsultantMessageListState(LazyListState())
    }
}

@Composable
internal fun rememberOnlineConsultantMessageListState(owner: Any): OnlineConsultantMessageListState {
    return remember(owner) {
        synchronized(messageListStateCache) {
            messageListStateCache.getOrPut(owner) {
                OnlineConsultantMessageListState(LazyListState())
            }
        }
    }
}
