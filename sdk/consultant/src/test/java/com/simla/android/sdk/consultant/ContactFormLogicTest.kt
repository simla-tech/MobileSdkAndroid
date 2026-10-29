package com.simla.android.sdk.consultant

import com.simla.android.sdk.consultant.contactform.shouldShowContactFormMessage
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE
import com.simla.android.sdk.consultant.models.contactform.CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormConfig
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormField
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFormLogicTest {
    @Test
    fun contact_form_is_shown_at_beginning_before_visitor_messages() {
        assertTrue(
            shouldShowContactFormMessage(
                messages = emptyList(),
                config = widgetConfig(CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE),
                user = ConsultantContactFormRequest(),
                display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                wasSent = false,
            ),
        )
    }

    @Test
    fun contact_form_after_first_message_waits_for_visitor_message() {
        val config = widgetConfig(CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE)

        assertFalse(
            shouldShowContactFormMessage(
                messages = emptyList(),
                config = config,
                user = ConsultantContactFormRequest(),
                display = CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE,
                wasSent = false,
            ),
        )
        assertTrue(
            shouldShowContactFormMessage(
                messages = listOf(visitorMessage()),
                config = config,
                user = ConsultantContactFormRequest(),
                display = CONTACT_FORM_DISPLAY_AFTER_FIRST_MESSAGE,
                wasSent = false,
            ),
        )
    }

    @Test
    fun contact_form_is_not_shown_when_required_fields_are_already_known() {
        assertFalse(
            shouldShowContactFormMessage(
                messages = emptyList(),
                config = widgetConfig(CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE),
                user = ConsultantContactFormRequest(
                    name = "Ivan",
                    phone = "+79990000000",
                ),
                display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                wasSent = false,
            ),
        )
    }

    @Test
    fun contact_form_is_not_duplicated_or_shown_after_submit() {
        val config = widgetConfig(CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE)

        assertFalse(
            shouldShowContactFormMessage(
                messages = listOf(contactFormMessage()),
                config = config,
                user = ConsultantContactFormRequest(),
                display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                wasSent = false,
            ),
        )
        assertFalse(
            shouldShowContactFormMessage(
                messages = emptyList(),
                config = config,
                user = ConsultantContactFormRequest(),
                display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                wasSent = true,
            ),
        )
    }

    private fun widgetConfig(display: String) = ConsultantWidgetConfig(
        useContactForm = true,
        contactFormDisplay = display,
        contactForm = ConsultantContactFormConfig(
            name = ConsultantContactFormField(required = true),
            phone = ConsultantContactFormField(required = true),
        ),
    )

    private fun visitorMessage() = OnlineConsultantMessageUiModel(
        id = "visitor-message",
        author = OnlineConsultantMessageAuthor.VISITOR,
        authorName = "You",
        text = "Hello",
        timeLabel = "12:00",
    )

    private fun contactFormMessage() = OnlineConsultantMessageUiModel(
        id = "contact-form",
        author = OnlineConsultantMessageAuthor.BOT,
        authorName = "Consultant",
        text = "Contacts",
        timeLabel = "",
        contactForm = com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormUiModel(
            config = ConsultantContactFormConfig(name = ConsultantContactFormField(required = true)),
        ),
    )

    @Test
    fun required_marketing_consent_keeps_contact_form_visible() {
        val config = ConsultantWidgetConfig(
            useContactForm = true,
            contactFormDisplay = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
            contactForm = ConsultantContactFormConfig(
                name = ConsultantContactFormField(required = true),
                marketingConsent = ConsultantContactFormField(required = true),
            ),
        )

        assertTrue(
            shouldShowContactFormMessage(
                messages = emptyList(),
                config = config,
                user = ConsultantContactFormRequest(name = "Ivan"),
                display = CONTACT_FORM_DISPLAY_AT_BEGINNING_DIALOGUE,
                wasSent = false,
            ),
        )
    }
}
