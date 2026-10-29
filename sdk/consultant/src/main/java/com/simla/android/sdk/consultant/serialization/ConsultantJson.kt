package com.simla.android.sdk.consultant.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

internal val SdkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

internal fun JsonElement?.asFlexibleString(): String? = when (this) {
    null, JsonNull -> null
    is JsonPrimitive -> content
    else -> toString()
}
