package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.R

@Composable
internal fun PersonalDataWarning(
    link: String,
    modifier: Modifier = Modifier,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Surface(
        modifier = modifier,
        color = ConsultantPersonalDataWarningBackground,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.message_failed_outlined),
                contentDescription = null,
                tint = visualTheme.brandColor,
                modifier = Modifier.size(20.dp),
            )
            PersonalDataLinkedText(
                firstPart = stringResource(R.string.crm_consultant_personal_data_notice_prefix),
                linkText = stringResource(R.string.crm_consultant_personal_data_notice_link),
                thirdPart = stringResource(R.string.crm_consultant_personal_data_notice_suffix),
                link = link,
                color = ConsultantSecondaryText,
                linkColor = visualTheme.brandColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ContactFormResultText(
    text: String,
    isError: Boolean,
) {
    if (text.isBlank()) {
        return
    }
    Text(
        text = text,
        color = if (isError) ConsultantStatusError else LocalCrmVisualTheme.current.primaryText,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun ContactFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    keyboardType: KeyboardType,
    error: Boolean,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    placeholder: String? = null,
    errorText: String = stringResource(R.string.crm_consultant_required_field),
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Column(
        modifier = Modifier
            .width(width)
            .defaultMinSize(minHeight = CONTACT_FORM_FIELD_SLOT_MIN_HEIGHT),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(width)
                .defaultMinSize(minHeight = CONTACT_FORM_FIELD_MIN_HEIGHT)
                .focusRequester(focusRequester),
            label = { Text(label) },
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        color = visualTheme.placeholderText,
                    )
                }
            },
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            isError = error,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(4.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = contactFormTextFieldColors(visualTheme),
        )

        Text(
            text = if (error) errorText else "",
            color = ConsultantStatusError,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
        )
    }
}

@Composable
internal fun ContactFormPhoneField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    keyboardType: KeyboardType,
    error: Boolean,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    placeholder: String,
    errorText: String = stringResource(R.string.crm_consultant_required_field),
    enabled: Boolean,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Column(
        modifier = Modifier
            .width(width)
            .defaultMinSize(minHeight = CONTACT_FORM_FIELD_SLOT_MIN_HEIGHT),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(width)
                .defaultMinSize(minHeight = CONTACT_FORM_FIELD_MIN_HEIGHT)
                .focusRequester(focusRequester),
            label = { Text(label) },
            placeholder = {
                Text(
                    text = placeholder,
                    color = visualTheme.placeholderText,
                )
            },
            enabled = enabled,
            singleLine = true,
            isError = error,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(4.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = contactFormTextFieldColors(visualTheme),
        )

        Text(
            text = if (error) errorText else "",
            color = ConsultantStatusError,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
        )
    }
}

@Composable
internal fun ContactFormPersonalDataConsent(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    link: String?,
    required: Boolean,
    showError: Boolean,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        PersonalDataLinkedText(
            firstPart = stringResource(R.string.crm_consultant_personal_data_agreement_prefix),
            linkText = stringResource(R.string.crm_consultant_personal_data_agreement_link),
            thirdPart = "",
            link = link,
            color = if (showError && required && !checked) ConsultantStatusError else ConsultantSecondaryText,
            linkColor = visualTheme.brandColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ContactFormMarketingConsent(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    required: Boolean,
    showError: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = stringResource(R.string.crm_consultant_contact_form_marketing_consent),
            color = if (showError && required && !checked) {
                ConsultantStatusError
            } else {
                ConsultantSecondaryText
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PersonalDataLinkedText(
    firstPart: String,
    linkText: String,
    thirdPart: String,
    link: String?,
    color: Color,
    linkColor: Color,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val annotatedText = remember(firstPart, linkText, thirdPart, link, linkColor) {
        buildAnnotatedString {
            append(firstPart)
            val linkStart = length
            append(linkText)
            val linkEnd = length
            if (!link.isNullOrBlank()) {
                addStringAnnotation(
                    tag = PERSONAL_DATA_LINK_TAG,
                    annotation = link,
                    start = linkStart,
                    end = linkEnd,
                )
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start = linkStart,
                    end = linkEnd,
                )
            }
            append(thirdPart)
        }
    }
    AnnotatedLinkText(
        text = annotatedText,
        color = color,
        style = style,
        modifier = modifier,
        linkTag = PERSONAL_DATA_LINK_TAG,
        linkHighlightColor = linkColor,
    )
}
