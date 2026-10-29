package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormKind
import com.simla.android.sdk.consultant.models.widget.ConsultantWidgetConfig
import com.simla.android.sdk.consultant.ui.icons.ArrowUpward
import com.simla.android.sdk.consultant.ui.icons.AttachFile
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun shouldHideComposerForBlockingContactForm(
    messages: List<OnlineConsultantMessageUiModel>,
    isOffline: Boolean,
): Boolean {
    if (isOffline) {
        return true
    }
    val hasInitialContactForm = messages.any { message ->
        message.contactForm?.kind == ConsultantContactFormKind.CONTACT
    }
    val hasVisitorMessage = messages.any { message ->
        message.author == OnlineConsultantMessageAuthor.VISITOR && message.contactForm == null
    }
    return hasInitialContactForm && !hasVisitorMessage
}

internal fun shouldShowPersonalDataWarning(
    config: ConsultantWidgetConfig?,
    messages: List<OnlineConsultantMessageUiModel>,
): Boolean {
    if (config?.personalDataConsent != true || config.personalDataLink.isNullOrBlank()) {
        return false
    }

    val hasVisibleConsentForm = messages.any { message ->
        val form = message.contactForm
        form?.personalDataConsent == true && form.config.personalData?.required == true
    }

    return !hasVisibleConsentForm
}

@Composable
internal fun OnlineConsultantComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val attachInteractionSource = remember { MutableInteractionSource() }
    val sendInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp, max = 112.dp)
            .shadow(elevation = 3.dp)
            .background(visualTheme.bottomBarColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = attachInteractionSource,
                        indication = ripple(bounded = false),
                        onClick = onAttachClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ConsultantIcons.AttachFile,
                    contentDescription = stringResource(R.string.crm_consultant_attach_file),
                    modifier = Modifier.size(24.dp),
                    tint = ConsultantPlaceholderText,
                )
            }

            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier
                    .padding(start = 12.dp, end = 48.dp)
                    .weight(1f)
                    .heightIn(min = 28.dp, max = 96.dp),
                textStyle = TextStyle(
                    color = visualTheme.areaTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                    letterSpacing = 0.15.sp,
                ),
                cursorBrush = SolidColor(visualTheme.brandColor),
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.crm_consultant_message),
                                color = visualTheme.placeholderText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 24.sp,
                                letterSpacing = 0.15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(visualTheme.brandColor, CircleShape)
                .clickable(
                    interactionSource = sendInteractionSource,
                    indication = ripple(bounded = true, color = visualTheme.mainIconColor),
                    onClick = onSendClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ConsultantIcons.ArrowUpward,
                contentDescription = stringResource(R.string.crm_consultant_send_message),
                modifier = Modifier.size(20.dp),
                tint = visualTheme.mainIconColor,
            )
        }
    }
}
