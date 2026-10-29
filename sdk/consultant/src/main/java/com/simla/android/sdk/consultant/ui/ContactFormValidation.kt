package com.simla.android.sdk.consultant.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormConfig
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormField
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormUiModel

internal fun shouldShowPersonalDataConsent(form: ConsultantContactFormUiModel): Boolean {
    return form.personalDataConsent && form.config.personalData != null
}

internal fun isContactFormValid(
    config: ConsultantContactFormConfig,
    name: String,
    email: String,
    phone: String,
    comment: String,
    personalDataAccepted: Boolean,
    personalDataConsentEnabled: Boolean,
    marketingConsentAccepted: Boolean,
): Boolean {
    return isContactFormTextFieldValid(config.name, name, CONTACT_FORM_NAME_MAX_LENGTH) &&
            isContactFormEmailFieldValid(config.email, email) &&
            isContactFormTextFieldValid(config.phone, phone, CONTACT_FORM_PHONE_MAX_LENGTH) &&
            isContactFormTextFieldValid(config.comment, comment, CONTACT_FORM_COMMENT_MAX_LENGTH) &&
            (!personalDataConsentEnabled ||
                    config.personalData?.required != true ||
                    personalDataAccepted) &&
            (config.marketingConsent?.required != true || marketingConsentAccepted)
}

internal fun isContactFormTextFieldValid(
    field: ConsultantContactFormField?,
    value: String,
    maxLength: Int,
): Boolean {
    val trimmed = value.trim()
    if (field?.required == true && trimmed.isBlank()) {
        return false
    }

    return trimmed.length <= maxLength
}

internal fun isContactFormEmailFieldValid(
    field: ConsultantContactFormField?,
    value: String,
): Boolean {
    val trimmed = value.trim()
    if (field?.required == true && trimmed.isBlank()) {
        return false
    }
    if (trimmed.isBlank()) {
        return true
    }

    return trimmed.length <= CONTACT_FORM_EMAIL_MAX_LENGTH &&
            CONTACT_FORM_EMAIL_REGEX.matches(trimmed)
}

internal fun contactFormTextFieldError(
    field: ConsultantContactFormField?,
    value: String,
    maxLength: Int,
    requiredFieldError: String,
    maximumCharactersError: String,
): String {
    return if (field?.required == true && value.trim().isBlank()) {
        requiredFieldError
    } else {
        maximumCharactersError
    }
}

internal fun contactFormEmailFieldError(
    field: ConsultantContactFormField?,
    value: String,
    requiredFieldError: String,
    maximumCharactersError: String,
    invalidEmailError: String,
): String {
    val trimmed = value.trim()
    return when {
        field?.required == true && trimmed.isBlank() -> requiredFieldError
        trimmed.length > CONTACT_FORM_EMAIL_MAX_LENGTH -> maximumCharactersError
        else -> invalidEmailError
    }
}

@Composable
internal fun contactFormTextFieldColors(visualTheme: ConsultantVisualTheme): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = visualTheme.areaTextColor,
        unfocusedTextColor = visualTheme.areaTextColor,
        errorTextColor = visualTheme.areaTextColor,
        focusedContainerColor = visualTheme.areaBackgroundColor,
        unfocusedContainerColor = visualTheme.areaBackgroundColor,
        errorContainerColor = visualTheme.areaBackgroundColor,
        focusedBorderColor = visualTheme.brandColor,
        unfocusedBorderColor = visualTheme.areaBorderColor,
        errorBorderColor = ConsultantStatusError,
        cursorColor = visualTheme.brandColor,
        errorCursorColor = visualTheme.brandColor,
        focusedLabelColor = visualTheme.brandColor,
        unfocusedLabelColor = visualTheme.placeholderText,
        errorLabelColor = ConsultantStatusError,
        focusedPlaceholderColor = visualTheme.placeholderText,
        unfocusedPlaceholderColor = visualTheme.placeholderText,
        errorPlaceholderColor = visualTheme.placeholderText,
    )
}
