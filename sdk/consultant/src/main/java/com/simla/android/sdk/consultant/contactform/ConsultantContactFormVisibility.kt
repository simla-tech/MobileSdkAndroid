package com.simla.android.sdk.consultant.contactform

import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun shouldShowContactFormMessage(
    messages: List<OnlineConsultantMessageUiModel>,
    config: ConsultantWidgetConfig?,
    user: ConsultantContactFormRequest,
    display: String,
    wasSent: Boolean,
    isOffline: Boolean = false,
): Boolean {
    config ?: return false
    if (isOffline || wasSent || !config.useContactForm || config.contactFormDisplay != display) {
        return false
    }
    if (!config.contactForm.hasVisibleFields || user.hasFilledRequiredContactFormFields(config.contactForm)) {
        return false
    }
    if (messages.any { it.contactForm != null }) {
        return false
    }

    val hasVisitorMessage = messages.any {
        it.author == OnlineConsultantMessageAuthor.VISITOR && it.contactForm == null
    }

    return when (display) {
        CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE -> !hasVisitorMessage
        CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE -> hasVisitorMessage
        else -> false
    }
}
