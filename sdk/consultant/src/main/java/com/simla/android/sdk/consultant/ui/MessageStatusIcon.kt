package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.icons.Done
import com.simla.android.sdk.consultant.ui.icons.DoneAll
import com.simla.android.sdk.consultant.ui.icons.ErrorOutline
import com.simla.android.sdk.consultant.ui.icons.Schedule
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageDeliveryStatus

internal fun statusIconFor(
    status: OnlineConsultantMessageDeliveryStatus,
): StatusIconSpec? {
    return when (status) {
        OnlineConsultantMessageDeliveryStatus.NONE -> null
        OnlineConsultantMessageDeliveryStatus.SENDING -> StatusIconSpec(
            icon = ConsultantIcons.Schedule,
            tint = ConsultantStatusSending,
        )

        OnlineConsultantMessageDeliveryStatus.DELIVERED -> StatusIconSpec(
            icon = ConsultantIcons.Done,
            tint = ConsultantStatusDelivered,
        )

        OnlineConsultantMessageDeliveryStatus.READ -> StatusIconSpec(
            icon = ConsultantIcons.DoneAll,
            tint = ConsultantStatusRead,
        )

        OnlineConsultantMessageDeliveryStatus.ERROR -> StatusIconSpec(
            icon = ConsultantIcons.ErrorOutline,
            tint = ConsultantStatusError,
        )
    }
}
