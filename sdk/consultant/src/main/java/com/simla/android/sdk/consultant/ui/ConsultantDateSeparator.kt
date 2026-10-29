package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ConsultantDateSeparator(label: String) {
    val visualTheme = LocalCrmVisualTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = visualTheme.secondaryText,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
