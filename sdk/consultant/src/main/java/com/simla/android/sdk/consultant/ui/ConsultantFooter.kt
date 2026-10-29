package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simla.android.sdk.consultant.R
import com.simla.android.sdk.consultant.models.connection.ConsultantDomainConfig

@Composable
internal fun ConsultantFooterBranding(
    domainConfig: ConsultantDomainConfig,
    darkTheme: Boolean,
) {
    val visualTheme = LocalCrmVisualTheme.current
    val uriHandler = LocalUriHandler.current
    val targetUrl = remember(domainConfig.url) {
        consultantFooterTargetUrl(domainConfig.url)
    }
    val logoUrl = remember(domainConfig.logo, darkTheme) {
        consultantFooterLogoUrl(domainConfig.logo, darkTheme)
    }
    var logoLoadFailed by remember(logoUrl) { mutableStateOf(false) }
    val clickModifier = if (targetUrl != null) {
        Modifier.clickable { uriHandler.openUri(targetUrl) }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(31.dp)
            .background(visualTheme.consultantBubbleColor)
            .then(clickModifier),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.crm_consultant_footer_made_in),
            color = visualTheme.messageDateColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
        )
        if (logoUrl != null && !logoLoadFailed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = consultantFooterBrandName(domainConfig.url),
                contentScale = ContentScale.Fit,
                onError = { logoLoadFailed = true },
                modifier = Modifier
                    .width(66.dp)
                    .height(12.dp),
            )
        } else {
            Text(
                text = consultantFooterBrandName(domainConfig.url),
                color = visualTheme.messageDateColor,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

internal fun consultantFooterTargetUrl(url: String?): String? {
    val value = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        value.startsWith("https://", ignoreCase = true) ||
                value.startsWith("http://", ignoreCase = true) -> value

        value.startsWith("//") -> "https:$value"
        else -> null
    }
}

internal fun consultantFooterLogoUrl(logo: String?, darkTheme: Boolean): String? {
    val value = logo?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (darkTheme) {
        value.replace("logo_horiz.svg", "logo_horiz_white.svg")
    } else {
        value
    }
}

internal fun consultantFooterBrandName(url: String?): String {
    return if (url.orEmpty().contains("retailcrm", ignoreCase = true)) "retailCRM" else "Crm"
}
