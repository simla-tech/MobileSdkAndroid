package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.graphics.Color

internal fun Color.darken(amount: Float = 0.10f): Color {
    val factor = 1f - amount.coerceIn(0f, 1f)
    return Color(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = alpha,
    )
}
