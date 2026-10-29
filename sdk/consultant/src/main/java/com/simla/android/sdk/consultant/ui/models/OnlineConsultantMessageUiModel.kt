package com.simla.android.sdk.consultant.ui.models

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormUiModel

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OnlineConsultantMessageUiModel(
    val id: String,
    val author: OnlineConsultantMessageAuthor,
    val authorName: String,
    val text: String,
    val timeLabel: String,
    val sentAt: String? = null,
    val sortKey: String? = null,
    val authorAvatarUrl: String? = null,
    val deliveryStatus: OnlineConsultantMessageDeliveryStatus = OnlineConsultantMessageDeliveryStatus.NONE,
    val imageUrl: String? = null,
    val imageWidthPx: Int? = null,
    val imageHeightPx: Int? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSizeLabel: String? = null,
    val quoteText: String? = null,
    val quoteFileName: String? = null,
    val quoteFileSizeLabel: String? = null,
    val quoteImageUrl: String? = null,
    val quoteMessageId: String? = null,
    val productName: String? = null,
    val productUrl: String? = null,
    val productImageUrl: String? = null,
    val productImageWidthPx: Int? = null,
    val productImageHeightPx: Int? = null,
    val productPriceLabel: String? = null,
    val productQuantityLabel: String? = null,
    val orderNumberLabel: String? = null,
    val orderItems: List<OnlineConsultantOrderItemUiModel> = emptyList(),
    val orderFields: List<OnlineConsultantOrderFieldUiModel> = emptyList(),
    val contactForm: ConsultantContactFormUiModel? = null,
    val autoMessageTitle: String? = null,
    val autoMessageImageUrl: String? = null,
    val autoMessageImageWidthPx: Int? = null,
    val autoMessageImageHeightPx: Int? = null,
    val buttons: List<OnlineConsultantMessageButtonUiModel> = emptyList(),
    val quickResponses: List<OnlineConsultantQuickResponseUiModel> = emptyList(),
)
