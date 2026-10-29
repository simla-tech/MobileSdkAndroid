package com.simla.android.sdk.consultant

import java.util.concurrent.atomic.AtomicLong

internal class ConsultantGeneration {
    private val value = AtomicLong(0L)

    fun snapshot(): Long = value.get()

    fun advance(): Long = value.incrementAndGet()

    fun isCurrent(snapshot: Long): Boolean = value.get() == snapshot
}
