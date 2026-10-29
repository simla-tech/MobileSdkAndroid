package com.simla.android.sdk.consultant.ui.models

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OnlineConsultantOrderItemUiModel(
    val numberLabel: String,
    val name: String,
    val details: String,
    val url: String? = null,
)
