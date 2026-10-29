package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.simla.android.sdk.consultant.ui.icons.ConsultantIcons
import com.simla.android.sdk.consultant.ui.icons.SmartToy
import com.simla.android.sdk.consultant.ui.icons.SupportAgent
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageAuthor
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageUiModel

@Composable
internal fun ConsultantAvatar(
    message: OnlineConsultantMessageUiModel,
) {
    val painter = rememberAsyncImagePainter(model = message.authorAvatarUrl)
    val imageState = painter.state

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(avatarBackgroundFor(message.author))
            .border(1.dp, ConsultantAvatarBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (message.authorAvatarUrl != null) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (message.authorAvatarUrl == null || imageState !is AsyncImagePainter.State.Success) {
            ConsultantAvatarFallback(message)
        }
    }
}

@Composable
private fun ConsultantAvatarFallback(
    message: OnlineConsultantMessageUiModel,
) {
    when (message.author) {
        OnlineConsultantMessageAuthor.BOT -> {
            Icon(
                imageVector = ConsultantIcons.SmartToy,
                contentDescription = null,
                tint = ConsultantBotIconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        OnlineConsultantMessageAuthor.CONSULTANT -> {
            Icon(
                imageVector = ConsultantIcons.SupportAgent,
                contentDescription = null,
                tint = OnlineConsultantIconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        else -> {
            Text(
                text = initialsFrom(message.authorName),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}
