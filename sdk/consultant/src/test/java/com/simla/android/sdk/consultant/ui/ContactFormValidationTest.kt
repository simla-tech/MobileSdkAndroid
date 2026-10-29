package com.simla.android.sdk.consultant.ui

import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormConfig
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormField
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFormValidationTest {
    @Test
    fun optional_email_accepts_empty_value() {
        assertTrue(isContactFormEmailFieldValid(ConsultantContactFormField(required = false), ""))
    }

    @Test
    fun required_email_rejects_empty_value() {
        assertFalse(isContactFormEmailFieldValid(ConsultantContactFormField(required = true), ""))
    }

    @Test
    fun email_rejects_invalid_non_empty_value() {
        assertFalse(
            isContactFormEmailFieldValid(
                ConsultantContactFormField(required = false),
                "not-an-email"
            )
        )
    }

    @Test
    fun email_accepts_valid_value_with_spaces() {
        assertTrue(
            isContactFormEmailFieldValid(
                ConsultantContactFormField(required = true),
                " user@example.com "
            )
        )
    }

    @Test
    fun email_rejects_backend_length_overflow() {
        val longEmail = "a".repeat(45) + "@example.com"

        assertFalse(
            isContactFormEmailFieldValid(
                ConsultantContactFormField(required = false),
                longEmail
            )
        )
    }

    @Test
    fun text_field_rejects_backend_length_overflow() {
        assertFalse(
            isContactFormTextFieldValid(
                ConsultantContactFormField(required = false),
                "a".repeat(CONTACT_FORM_NAME_MAX_LENGTH + 1),
                CONTACT_FORM_NAME_MAX_LENGTH,
            ),
        )
    }

    @Test
    fun optional_marketing_consent_accepts_unchecked_value() {
        assertTrue(
            isContactFormValid(
                config = ConsultantContactFormConfig(
                    marketingConsent = ConsultantContactFormField(required = false),
                ),
                name = "",
                email = "",
                phone = "",
                comment = "",
                personalDataAccepted = false,
                personalDataConsentEnabled = false,
                marketingConsentAccepted = false,
            ),
        )
    }

    @Test
    fun required_marketing_consent_rejects_unchecked_value() {
        assertFalse(
            isContactFormValid(
                config = ConsultantContactFormConfig(
                    marketingConsent = ConsultantContactFormField(required = true),
                ),
                name = "",
                email = "",
                phone = "",
                comment = "",
                personalDataAccepted = false,
                personalDataConsentEnabled = false,
                marketingConsentAccepted = false,
            ),
        )
    }

    @Test
    fun required_marketing_consent_accepts_checked_value() {
        assertTrue(
            isContactFormValid(
                config = ConsultantContactFormConfig(
                    marketingConsent = ConsultantContactFormField(required = true),
                ),
                name = "",
                email = "",
                phone = "",
                comment = "",
                personalDataAccepted = false,
                personalDataConsentEnabled = false,
                marketingConsentAccepted = true,
            ),
        )
    }
}
