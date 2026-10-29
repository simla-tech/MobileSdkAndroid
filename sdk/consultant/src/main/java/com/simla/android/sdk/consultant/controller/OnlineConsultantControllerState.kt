package com.simla.android.sdk.consultant.controller

import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal sealed interface OnlineConsultantControllerState {
    data object Idle : OnlineConsultantControllerState
    data object InitialLoading : OnlineConsultantControllerState
    data object Unavailable : OnlineConsultantControllerState

    data class Ready(
        val messages: List<OnlineConsultantMessageUiModel>,
        val isInitialLoading: Boolean,
        val isLoadingOlderMessages: Boolean,
        val canLoadOlderMessages: Boolean,
        val widgetConfig: ConsultantWidgetConfig?,
        val isOffline: Boolean,
    ) : OnlineConsultantControllerState
}
