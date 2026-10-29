package com.simla.android.sdk.consultant.ui

internal fun Throwable.uiErrorSignature(): String {
    return "${this::class.java.name}:${message.orEmpty()}:${cause?.let { it::class.java.name }}"
}
