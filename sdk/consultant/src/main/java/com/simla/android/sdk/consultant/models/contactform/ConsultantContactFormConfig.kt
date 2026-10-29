package com.simla.android.sdk.consultant.models.contactform

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

import kotlinx.serialization.Serializable

@Serializable
@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsultantContactFormConfig(
    val name: ConsultantContactFormField? = null,
    val email: ConsultantContactFormField? = null,
    val phone: ConsultantContactFormField? = null,
    val comment: ConsultantContactFormField? = null,
    val personalData: ConsultantContactFormField? = null,
    val marketingConsent: ConsultantContactFormField? = null,
) {
    val hasVisibleFields: Boolean
        get() = name != null || email != null || phone != null || comment != null ||
                personalData != null || marketingConsent != null
}
