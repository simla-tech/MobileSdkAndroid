package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.lazy.LazyListState

internal fun LazyListState.isScrolledToOlderMessagesEdge(): Boolean {
    val totalItemsCount = layoutInfo.totalItemsCount
    if (totalItemsCount == 0) {
        return false
    }

    val lastVisibleIndex = layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: return false
    return lastVisibleIndex >= totalItemsCount - 2
}

internal fun shouldAutoLoadOlderMessages(
    isAtOlderMessagesEdge: Boolean,
    oldestVisibleMessageId: String?,
    lastAutoLoadAnchorMessageId: String?,
    canLoadOlderMessages: Boolean,
    isLoadingOlderMessages: Boolean,
    isInitialLoading: Boolean,
): Boolean {
    return isAtOlderMessagesEdge &&
            oldestVisibleMessageId != null &&
            oldestVisibleMessageId != lastAutoLoadAnchorMessageId &&
            canLoadOlderMessages &&
            !isLoadingOlderMessages &&
            !isInitialLoading
}
