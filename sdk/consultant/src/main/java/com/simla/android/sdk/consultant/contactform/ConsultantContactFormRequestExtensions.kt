package com.simla.android.sdk.consultant.contactform

import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormConfig
import com.simla.android.sdk.consultant.models.contactform.ConsultantContactFormRequest
import com.simla.android.sdk.util.normalizedContactFormValue

internal fun ConsultantContactFormRequest.hasFilledRequiredContactFormFields(
    config: ConsultantContactFormConfig,
): Boolean {
    return (!config.name.isRequired || !name.isNullOrBlank()) &&
            (!config.email.isRequired || !email.isNullOrBlank()) &&
            (!config.phone.isRequired || !phone.isNullOrBlank()) &&
            (!config.comment.isRequired || !comment.isNullOrBlank()) &&
            !config.personalData.isRequired &&
            !config.marketingConsent.isRequired
}

internal fun ConsultantContactFormRequest.normalized(): ConsultantContactFormRequest {
    return ConsultantContactFormRequest(
        name = name.normalizedContactFormValue(),
        email = email.normalizedContactFormValue(),
        phone = phone.normalizedContactFormValue(),
        comment = comment.normalizedContactFormValue(),
        marketingConsent = marketingConsent,
    )
}

internal fun ConsultantContactFormRequest.merge(
    other: ConsultantContactFormRequest,
): ConsultantContactFormRequest {
    return ConsultantContactFormRequest(
        name = other.name ?: name,
        email = other.email ?: email,
        phone = other.phone ?: phone,
        comment = other.comment ?: comment,
        marketingConsent = other.marketingConsent ?: marketingConsent,
    )
}
