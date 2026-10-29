package com.simla.android.sdk.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonElement?.asFlexibleString(): String? {
    return when (this) {
        null, JsonNull -> null
        is JsonPrimitive -> content
        else -> toString()
    }
}
