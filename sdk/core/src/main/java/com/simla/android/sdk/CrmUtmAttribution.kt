package com.simla.android.sdk

import androidx.annotation.RestrictTo

@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CrmUtmAttribution(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val term: String? = null,
    val content: String? = null,
) {
    fun isEmpty(): Boolean =
        source == null && medium == null && campaign == null && term == null && content == null
}
