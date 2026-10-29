package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ContactFormSubmitButton(
    width: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val visualTheme = LocalCrmVisualTheme.current
    Surface(
        modifier = Modifier
            .width(width)
            .height(CONTACT_FORM_BUTTON_HEIGHT)
            .clip(
                if (visualTheme.roundedElements) RoundedCornerShape(8.dp) else RoundedCornerShape(
                    0.dp
                )
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (enabled) visualTheme.brandColor else ConsultantDisabledBackground,
        shape = if (visualTheme.roundedElements) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (enabled) Color.White else ConsultantSecondaryText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}
