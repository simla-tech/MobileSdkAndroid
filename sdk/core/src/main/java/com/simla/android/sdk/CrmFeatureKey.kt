package com.simla.android.sdk

import androidx.annotation.RestrictTo

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CrmFeatureKey<T : Any>(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Module key must not be blank" }
    }
}
