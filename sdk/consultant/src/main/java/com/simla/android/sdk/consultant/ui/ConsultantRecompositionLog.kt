package com.simla.android.sdk.consultant.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

@Composable
internal fun ConsultantRecompositionLog(
    name: String,
    details: () -> String = { "" },
) {
    val counter = remember { RecompositionCounter() }
    SideEffect {
        counter.value += 1
        val suffix = details().takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        ConsultantUiDebugLog.d("recompose $name count=${counter.value}$suffix")
    }
}

private class RecompositionCounter(var value: Int = 0)
