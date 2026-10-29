package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

internal fun bubbleColorFor(isVisitor: Boolean, visualTheme: ConsultantVisualTheme): Color {
    return if (isVisitor) visualTheme.visitorBubbleColor else visualTheme.consultantBubbleColor
}

internal fun bubbleContentColorFor(isVisitor: Boolean, visualTheme: ConsultantVisualTheme): Color {
    return if (isVisitor) visualTheme.mainTextColor else visualTheme.primaryText
}

internal fun bubbleContentColorFor(
    message: OnlineConsultantMessageUiModel,
    visualTheme: ConsultantVisualTheme,
): Color {
    return bubbleContentColorFor(
        message.author == OnlineConsultantMessageAuthor.VISITOR,
        visualTheme
    )
}

internal fun metaColorFor(
    message: OnlineConsultantMessageUiModel,
    visualTheme: ConsultantVisualTheme,
): Color {
    return if (message.author == OnlineConsultantMessageAuthor.VISITOR) {
        visualTheme.mainTextColor
    } else {
        visualTheme.messageDateColor
    }
}

internal fun bubbleShapeFor(isVisitor: Boolean, rounded: Boolean = true): RoundedCornerShape {
    if (!rounded) {
        return RoundedCornerShape(0.dp)
    }

    return if (isVisitor) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 0.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 0.dp,
            bottomEnd = 12.dp,
        )
    }
}
