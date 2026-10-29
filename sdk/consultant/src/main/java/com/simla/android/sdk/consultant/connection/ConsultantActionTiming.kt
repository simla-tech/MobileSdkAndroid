package com.simla.android.sdk.consultant.connection

import com.simla.android.sdk.consultant.contactform.shouldShowContactFormMessage

import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun shouldDelayInitialActionMessages(
    messages: List<OnlineConsultantMessageUiModel>,
    config: ConsultantWidgetConfig?,
    user: ConsultantContactFormRequest,
    isOffline: Boolean,
    contactFormWasSent: Boolean,
): Boolean {
    return shouldShowContactFormMessage(
        messages = messages,
        config = config,
        user = user,
        display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
        wasSent = contactFormWasSent,
        isOffline = isOffline,
    )
}
