package com.simla.android.sdk.consultant.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
internal fun rememberMinimumVisibleLoading(
    isLoading: Boolean,
    minimumVisibleMs: Long = SPINNER_MINIMUM_VISIBLE_MS,
): Boolean {
    var isVisible by remember { mutableStateOf(false) }
    var visibleSinceMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLoading, minimumVisibleMs) {
        if (isLoading) {
            visibleSinceMs = SystemClock.elapsedRealtime()
            isVisible = true
        } else {
            if (!isVisible) {
                return@LaunchedEffect
            }

            val elapsedMs = SystemClock.elapsedRealtime() - visibleSinceMs
            val remainingMs = minimumVisibleMs - elapsedMs
            if (remainingMs > 0) {
                delay(remainingMs)
            }
            isVisible = false
        }
    }

    return isVisible
}
