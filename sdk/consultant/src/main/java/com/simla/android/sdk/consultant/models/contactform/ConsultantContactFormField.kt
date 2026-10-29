package com.simla.android.sdk.consultant.models.contactform

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

import kotlinx.serialization.Serializable

@Serializable
@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsultantContactFormField(
    val required: Boolean = false,
)
