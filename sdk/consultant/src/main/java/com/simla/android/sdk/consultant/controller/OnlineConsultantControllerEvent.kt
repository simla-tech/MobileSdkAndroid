package com.simla.android.sdk.consultant.controller

internal sealed interface OnlineConsultantControllerEvent {
    data class InitialConnectFailed(
        val throwable: Throwable,
    ) : OnlineConsultantControllerEvent

    data class MessageSendFailed(
        val throwable: Throwable,
    ) : OnlineConsultantControllerEvent

    data class FileSendFailed(
        val throwable: Throwable,
    ) : OnlineConsultantControllerEvent

    data class LoadOlderMessagesFailed(
        val throwable: Throwable,
    ) : OnlineConsultantControllerEvent

    data class ContactFormSendFailed(
        val throwable: Throwable,
    ) : OnlineConsultantControllerEvent
}
