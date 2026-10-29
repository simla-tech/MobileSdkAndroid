package com.simla.android.sdk

import androidx.annotation.RestrictTo

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CrmFeature<T : Any> {
    val key: CrmFeatureKey<T>

    fun install(context: CrmFeatureContext): T
}
