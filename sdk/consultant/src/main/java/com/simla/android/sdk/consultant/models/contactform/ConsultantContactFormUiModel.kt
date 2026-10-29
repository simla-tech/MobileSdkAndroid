package com.simla.android.sdk.consultant.models.contactform

import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsultantContactFormUiModel(
    val config: ConsultantContactFormConfig,
    val user: ConsultantContactFormRequest = ConsultantContactFormRequest(),
    val kind: ConsultantContactFormKind = ConsultantContactFormKind.CONTACT,
    val state: ConsultantContactFormState = ConsultantContactFormState.START,
    val resultText: String? = null,
    val errorText: String? = null,
    val personalDataConsent: Boolean = false,
    val personalDataLink: String? = null,
)
