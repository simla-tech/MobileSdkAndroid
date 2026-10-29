package com.simla.android.sdk.consultant.serialization

import kotlinx.serialization.json.JsonElement

internal fun jsonString(value: String): JsonElement =
    kotlinx.serialization.json.JsonPrimitive(value)
