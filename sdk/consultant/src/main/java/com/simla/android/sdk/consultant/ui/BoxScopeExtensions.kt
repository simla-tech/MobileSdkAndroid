package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun BoxScope.MessageMetaOverlay(
    message: OnlineConsultantMessageUiModel,
    statusIcon: StatusIconSpec?,
) {
    Box(
        modifier = Modifier
            .align(imageMetaOverlayAlignment())
            .padding(META_OVERLAY_EDGE_PADDING)
            .clip(RoundedCornerShape(4.dp))
            .background(ConsultantMetaOverlayColor)
            .padding(horizontal = META_OVERLAY_HORIZONTAL_PADDING),
    ) {
        MessageMetaRow(
            message = message,
            statusIcon = statusIcon,
            metaDirection = ConsultantMessageTextDirection.LTR,
            inlineStartSpacing = 0.dp,
            contentColor = Color.White,
            statusIconTint = Color.White,
        )
    }
}
