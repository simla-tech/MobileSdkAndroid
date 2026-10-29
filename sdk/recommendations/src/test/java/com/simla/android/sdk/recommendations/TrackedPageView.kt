package com.simla.android.sdk.recommendations

import kotlinx.serialization.json.JsonObject

internal data class TrackedPageView(
    val screenUri: String,
    val title: String?,
    val data: JsonObject,
)
