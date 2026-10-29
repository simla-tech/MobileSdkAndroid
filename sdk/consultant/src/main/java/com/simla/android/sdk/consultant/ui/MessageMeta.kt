package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun MessageMetaRow(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
    metaDirection: ConsultantMessageTextDirection,
    inlineStartSpacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    contentColor: Color? = null,
    statusIconTint: Color? = null,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val resolvedContentColor = contentColor ?: metaColorFor(message, visualTheme)
    Row(
        modifier = modifier.padding(
            start = if (metaDirection == ConsultantMessageTextDirection.LTR) inlineStartSpacing else 0.dp,
            end = if (metaDirection == ConsultantMessageTextDirection.RTL) inlineStartSpacing else 0.dp,
        ),
        horizontalArrangement = if (metaDirection == ConsultantMessageTextDirection.RTL) {
            Arrangement.Start
        } else {
            Arrangement.End
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.author == OnlineConsultantMessageAuthor.VISITOR && statusIcon != null) {
            Icon(
                imageVector = statusIcon.icon,
                contentDescription = null,
                tint = statusIconTint ?: visualTheme.mainTextColor,
                modifier = Modifier.size(STATUS_ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = message.timeLabel,
            color = resolvedContentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
        )
    }
}
