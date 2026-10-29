package com.simla.android.sdk.consultant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactFormPhoneMaskTest {
    @Test
    fun phone_input_formats_default_russian_template() {
        val normalized = normalizeContactPhoneInput("1234567890")

        assertEquals("71234567890", normalized)
        assertEquals("+7-123-456-78-90", formatContactPhoneDisplay(normalized))
        assertEquals("71234567890", contactPhoneValueForSubmit(normalized))
    }

    @Test
    fun phone_input_keeps_full_prefilled_phone() {
        val normalized = normalizeContactPhoneInput("+7 (999) 111-22-33")

        assertEquals("79991112233", normalized)
        assertEquals("+7-999-111-22-33", formatContactPhoneDisplay(normalized))
    }

    @Test
    fun phone_input_converts_russian_eight_prefix_to_country_code() {
        val normalized = normalizeContactPhoneInput("8 999 111 22 33")

        assertEquals("79991112233", normalized)
        assertEquals("79991112233", contactPhoneValueForSubmit(normalized))
    }

    @Test
    fun phone_submit_value_is_empty_until_web_minimum_number_length() {
        val normalized = normalizeContactPhoneInput("1234567")

        assertEquals("71234567", normalized)
        assertEquals("+7-123-456-7", formatContactPhoneDisplay(normalized))
        assertNull(contactPhoneValueForSubmit(normalized))
    }

    @Test
    fun phone_input_trims_extra_digits_to_template_length() {
        val normalized = normalizeContactPhoneInput("+7 123 456 78 90 55")

        assertEquals("71234567890", normalized)
        assertEquals("+7-123-456-78-90", formatContactPhoneDisplay(normalized))
    }
}
