package com.simla.android.sdk.consultant.models.connection


internal data class ReconnectState(
    val attempt: Int,
    val delayMs: Long,
    val updatedAt: String?,
)
