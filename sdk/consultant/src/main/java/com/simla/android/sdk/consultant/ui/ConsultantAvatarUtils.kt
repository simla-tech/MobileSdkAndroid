package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImagePainter
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import kotlin.math.roundToInt

internal fun imageDimensionsFromPainter(painter: AsyncImagePainter): Pair<Int, Int>? {
    val intrinsicSize = (painter.state as? AsyncImagePainter.State.Success)
        ?.painter
        ?.intrinsicSize
        ?: return null
    if (!intrinsicSize.width.isFinite() || !intrinsicSize.height.isFinite()) {
        return null
    }

    val width = intrinsicSize.width.roundToInt()
    val height = intrinsicSize.height.roundToInt()
    return if (width > 0 && height > 0) {
        width to height
    } else {
        null
    }
}

internal fun avatarBackgroundFor(author: OnlineConsultantMessageAuthor): Color {
    return when (author) {
        OnlineConsultantMessageAuthor.VISITOR -> ConsultantVisitorAvatarBg
        OnlineConsultantMessageAuthor.CONSULTANT -> Color.White
        OnlineConsultantMessageAuthor.BOT -> Color.White
    }
}

internal fun initialsFrom(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "U" }
}
