package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormState
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun ContactFormMessageBubble(
    message: OnlineConsultantMessageUiModel,
    contactForm: ConsultantContactFormUiModel,
    bubbleWidth: androidx.compose.ui.unit.Dp,
    formContentWidth: androidx.compose.ui.unit.Dp,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    onSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val shape = bubbleShapeFor(isVisitor = false, rounded = visualTheme.roundedElements)
    val bodyAnnotatedText = remember(message.text, visualTheme.brandColor) {
        messageHtmlAnnotatedString(
            source = message.text,
            linkColor = visualTheme.brandColor,
        )
    }
    Surface(
        modifier = Modifier
            .width(bubbleWidth)
            .clip(shape),
        color = visualTheme.consultantBubbleColor,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(CONTACT_FORM_BUBBLE_PADDING),
            verticalArrangement = Arrangement.spacedBy(CONTACT_FORM_ITEM_SPACING),
        ) {
            if (message.text.isNotBlank()) {
                AnnotatedLinkText(
                    text = bodyAnnotatedText,
                    color = visualTheme.primaryText,
                    style = bodyStyle,
                    linkHighlightColor = visualTheme.brandColor,
                    modifier = Modifier.width(formContentWidth),
                )
            }

            ContactFormContent(
                form = contactForm,
                width = formContentWidth,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
internal fun ContactFormContent(
    form: ConsultantContactFormUiModel,
    width: androidx.compose.ui.unit.Dp,
    onSubmit: (ConsultantContactFormKind, ConsultantContactFormRequest) -> Unit,
) {
    var name by remember(form.user.name) { mutableStateOf(form.user.name.orEmpty()) }
    var email by remember(form.user.email) { mutableStateOf(form.user.email.orEmpty()) }
    var comment by remember(form.user.comment) { mutableStateOf(form.user.comment.orEmpty()) }
    val initialPhone =
        remember(form.user.phone) { normalizeContactPhoneInput(form.user.phone.orEmpty()) }
    var phone by remember(form.user.phone) { mutableStateOf(initialPhone) }
    var phoneInput by remember(form.user.phone) {
        val displayValue = formatContactPhoneDisplay(initialPhone)
        mutableStateOf(
            TextFieldValue(
                text = displayValue,
                selection = TextRange(displayValue.length),
            ),
        )
    }
    var personalDataAccepted by remember(form.personalDataConsent) { mutableStateOf(false) }
    var marketingConsentAccepted by remember(form.user.marketingConsent) {
        mutableStateOf(form.user.marketingConsent == true)
    }
    var showErrors by remember { mutableStateOf(false) }

    val config = form.config
    val isSending = form.state == ConsultantContactFormState.SENDING
    val isSubmitted = form.state == ConsultantContactFormState.SENT ||
            form.state == ConsultantContactFormState.SENT_ERROR
    val focusManager = LocalFocusManager.current
    val inputOrder = remember(config.name, config.email, config.phone, config.comment) {
        buildList {
            if (config.name != null) add(ContactFormInputField.Name)
            if (config.email != null) add(ContactFormInputField.Email)
            if (config.phone != null) add(ContactFormInputField.Phone)
            if (config.comment != null) add(ContactFormInputField.Comment)
        }
    }
    val nameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val commentFocusRequester = remember { FocusRequester() }

    fun focusRequesterFor(field: ContactFormInputField): FocusRequester {
        return when (field) {
            ContactFormInputField.Name -> nameFocusRequester
            ContactFormInputField.Email -> emailFocusRequester
            ContactFormInputField.Phone -> phoneFocusRequester
            ContactFormInputField.Comment -> commentFocusRequester
        }
    }

    fun nextFieldAfter(field: ContactFormInputField): ContactFormInputField? {
        val index = inputOrder.indexOf(field)
        return inputOrder.getOrNull(index + 1)
    }

    fun submitForm() {
        showErrors = true
        val phoneForSubmit = contactPhoneValueForSubmit(phone)
        if (!isContactFormValid(
                config = config,
                name = name,
                email = email,
                phone = phoneForSubmit.orEmpty(),
                comment = comment,
                personalDataAccepted = personalDataAccepted,
                personalDataConsentEnabled = form.personalDataConsent,
                marketingConsentAccepted = marketingConsentAccepted,
            )
        ) {
            return
        }

        onSubmit(
            form.kind,
            ConsultantContactFormRequest(
                name = name.trim().takeIf { it.isNotEmpty() },
                email = email.trim().takeIf { it.isNotEmpty() },
                phone = phoneForSubmit,
                comment = comment.trim().takeIf { it.isNotEmpty() },
                marketingConsent = config.marketingConsent?.let { marketingConsentAccepted },
            ),
        )
    }

    fun imeActionFor(field: ContactFormInputField): ImeAction {
        return if (inputOrder.lastOrNull() == field) ImeAction.Done else ImeAction.Next
    }

    fun keyboardActionsFor(field: ContactFormInputField): KeyboardActions {
        return KeyboardActions(
            onNext = {
                nextFieldAfter(field)?.let { focusRequesterFor(it).requestFocus() }
            },
            onDone = {
                submitForm()
                focusManager.clearFocus()
            },
        )
    }

    fun updatePhoneInput(value: TextFieldValue) {
        val normalized = normalizeContactPhoneInput(value.text)
        val displayValue = formatContactPhoneDisplay(normalized)
        phone = normalized
        phoneInput = TextFieldValue(
            text = displayValue,
            selection = TextRange(displayValue.length),
        )
    }

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(CONTACT_FORM_ITEM_SPACING),
    ) {
        if (isSubmitted) {
            ContactFormResultText(
                text = form.resultText
                    ?: form.errorText
                    ?: if (form.state == ConsultantContactFormState.SENT_ERROR) {
                        stringResource(R.string.crm_consultant_contact_form_send_error)
                    } else {
                        ""
                    },
                isError = form.state == ConsultantContactFormState.SENT_ERROR,
            )
            return@Column
        }

        if (config.name != null) {
            ContactFormField(
                label = stringResource(R.string.crm_consultant_name),
                value = name,
                onValueChange = { name = it },
                width = width,
                keyboardType = KeyboardType.Text,
                error = showErrors && !isContactFormTextFieldValid(
                    config.name,
                    name,
                    CONTACT_FORM_NAME_MAX_LENGTH
                ),
                errorText = contactFormTextFieldError(
                    config.name,
                    name,
                    CONTACT_FORM_NAME_MAX_LENGTH,
                    stringResource(R.string.crm_consultant_required_field),
                    stringResource(
                        R.string.crm_consultant_maximum_characters,
                        CONTACT_FORM_NAME_MAX_LENGTH,
                    ),
                ),
                focusRequester = nameFocusRequester,
                imeAction = imeActionFor(ContactFormInputField.Name),
                keyboardActions = keyboardActionsFor(ContactFormInputField.Name),
                enabled = !isSending,
            )
        }

        if (config.email != null) {
            ContactFormField(
                label = stringResource(R.string.crm_consultant_email),
                value = email,
                onValueChange = { email = it },
                width = width,
                keyboardType = KeyboardType.Email,
                error = showErrors && !isContactFormEmailFieldValid(config.email, email),
                errorText = contactFormEmailFieldError(
                    config.email,
                    email,
                    stringResource(R.string.crm_consultant_required_field),
                    stringResource(
                        R.string.crm_consultant_maximum_characters,
                        CONTACT_FORM_EMAIL_MAX_LENGTH,
                    ),
                    stringResource(R.string.crm_consultant_invalid_email),
                ),
                focusRequester = emailFocusRequester,
                imeAction = imeActionFor(ContactFormInputField.Email),
                keyboardActions = keyboardActionsFor(ContactFormInputField.Email),
                enabled = !isSending,
            )
        }

        if (config.phone != null) {
            ContactFormPhoneField(
                label = stringResource(R.string.crm_consultant_phone),
                value = phoneInput,
                onValueChange = { updatePhoneInput(it) },
                width = width,
                keyboardType = KeyboardType.Phone,
                error = showErrors && !isContactFormTextFieldValid(
                    config.phone,
                    contactPhoneValueForSubmit(phone).orEmpty(),
                    CONTACT_FORM_PHONE_MAX_LENGTH,
                ),
                errorText = contactFormTextFieldError(
                    config.phone,
                    contactPhoneValueForSubmit(phone).orEmpty(),
                    CONTACT_FORM_PHONE_MAX_LENGTH,
                    stringResource(R.string.crm_consultant_required_field),
                    stringResource(
                        R.string.crm_consultant_maximum_characters,
                        CONTACT_FORM_PHONE_MAX_LENGTH,
                    ),
                ),
                placeholder = DEFAULT_CONTACT_PHONE_PLACEHOLDER,
                focusRequester = phoneFocusRequester,
                imeAction = imeActionFor(ContactFormInputField.Phone),
                keyboardActions = keyboardActionsFor(ContactFormInputField.Phone),
                enabled = !isSending,
            )
        }

        if (config.comment != null) {
            ContactFormField(
                label = stringResource(R.string.crm_consultant_comment),
                value = comment,
                onValueChange = { comment = it },
                width = width,
                keyboardType = KeyboardType.Text,
                error = showErrors && !isContactFormTextFieldValid(
                    config.comment,
                    comment,
                    CONTACT_FORM_COMMENT_MAX_LENGTH,
                ),
                errorText = contactFormTextFieldError(
                    config.comment,
                    comment,
                    CONTACT_FORM_COMMENT_MAX_LENGTH,
                    stringResource(R.string.crm_consultant_required_field),
                    stringResource(
                        R.string.crm_consultant_maximum_characters,
                        CONTACT_FORM_COMMENT_MAX_LENGTH,
                    ),
                ),
                focusRequester = commentFocusRequester,
                imeAction = imeActionFor(ContactFormInputField.Comment),
                keyboardActions = keyboardActionsFor(ContactFormInputField.Comment),
                enabled = !isSending,
                singleLine = false,
                minLines = 3,
            )
        }

        if (shouldShowPersonalDataConsent(form)) {
            ContactFormPersonalDataConsent(
                checked = personalDataAccepted,
                onCheckedChange = { if (!isSending) personalDataAccepted = it },
                link = form.personalDataLink,
                required = config.personalData?.required == true,
                showError = showErrors,
            )
        }

        config.marketingConsent?.let { marketingConsent ->
            ContactFormMarketingConsent(
                checked = marketingConsentAccepted,
                onCheckedChange = { if (!isSending) marketingConsentAccepted = it },
                required = marketingConsent.required,
                showError = showErrors,
            )
        }

        ContactFormSubmitButton(
            width = width,
            enabled = !isSending,
            label = if (isSending) {
                stringResource(R.string.crm_consultant_sending)
            } else {
                stringResource(R.string.crm_consultant_send)
            },
            onClick = { submitForm() },
        )
    }
}
