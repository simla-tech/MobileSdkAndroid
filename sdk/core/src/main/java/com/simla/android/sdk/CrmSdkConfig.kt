package com.simla.android.sdk

import com.simla.android.sdk.core.BuildConfig

data class CrmSdkConfig(
    val siteToken: String,
    val baseUrl: String = BuildConfig.DEFAULT_BASE_URL,
    val debug: Boolean = false,
) {
    val clientApiBaseUrl: String
        get() = normalizedClientApiBaseUrl(baseUrl)

    companion object {
        internal fun normalizedClientApiBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')

            return if (trimmed.endsWith("/api/client/v1")) {
                trimmed
            } else {
                "$trimmed/api/client/v1"
            }
        }
    }
}
