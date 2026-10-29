package com.simla.android.sdk.consultant.models.contactform

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

import kotlinx.serialization.Serializable

@Serializable
@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsultantContactFormRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val comment: String? = null,
    val marketingConsent: Boolean? = null,
)
