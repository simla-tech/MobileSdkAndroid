package com.simla.android.sdk.notifications

/** A diagnostic update emitted by the push runtime. */
data class PushDiagnosticEvent(
    val level: PushDiagnosticLevel,
    val message: String,
    val error: Throwable? = null,
)

/** Severity of a push diagnostic update. */
enum class PushDiagnosticLevel {
    INFO,
    ERROR,
}

/** Receives current push runtime diagnostics; previous updates are not replayed. */
fun interface PushDiagnosticListener {
    fun onDiagnosticEvent(event: PushDiagnosticEvent)
}
