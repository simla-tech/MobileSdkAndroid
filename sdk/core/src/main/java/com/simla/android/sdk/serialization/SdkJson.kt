package com.simla.android.sdk.serialization

import kotlinx.serialization.json.Json

internal val SdkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}
