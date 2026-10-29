package com.simla.android.sdk.consultant.mapping

import com.simla.android.sdk.consultant.models.message.ConsultantMessageAttachments
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantMessageButtonUiModel
import com.simla.android.sdk.consultant.ui.models.OnlineConsultantQuickResponseUiModel

internal fun ConsultantMessageAttachments?.toUiButtons(): List<OnlineConsultantMessageButtonUiModel> {
    return this?.links.orEmpty().mapNotNull { button ->
        val label = button.label?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
        OnlineConsultantMessageButtonUiModel(
            label = label,
            url = button.url?.trim().takeUnless { it.isNullOrEmpty() },
        )
    }
}

internal fun ConsultantMessageAttachments?.toUiQuickResponses(): List<OnlineConsultantQuickResponseUiModel> {
    return this?.suggestions.orEmpty().mapNotNull { suggestion ->
        val title =
            suggestion.title?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
        OnlineConsultantQuickResponseUiModel(title = title)
    }
}
