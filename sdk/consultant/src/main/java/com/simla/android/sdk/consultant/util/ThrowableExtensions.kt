package com.simla.android.sdk.consultant.util


internal fun Throwable.errorSignature(): String {
    return "${this::class.java.name}:${message.orEmpty()}:${cause?.let { it::class.java.name }}"
}
