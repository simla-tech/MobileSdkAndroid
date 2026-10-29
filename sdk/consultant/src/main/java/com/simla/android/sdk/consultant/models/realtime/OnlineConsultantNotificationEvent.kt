package com.simla.android.sdk.consultant.models.realtime

import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal sealed interface OnlineConsultantNotificationEvent {
    data class IncomingMessage(
        val message: OnlineConsultantMessageUiModel,
    ) : OnlineConsultantNotificationEvent
}
