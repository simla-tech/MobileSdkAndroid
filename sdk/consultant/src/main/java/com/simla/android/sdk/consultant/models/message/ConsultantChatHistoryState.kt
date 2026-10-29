package com.simla.android.sdk.consultant.models.message

internal data class ConsultantChatHistoryState(
    val isInitialLoading: Boolean = false,
    val isLoadingOlderMessages: Boolean = false,
    val canLoadOlderMessages: Boolean = false,
)
