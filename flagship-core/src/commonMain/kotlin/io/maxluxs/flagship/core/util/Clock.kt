package io.maxluxs.flagship.core.util

import kotlin.time.Clock as KotlinClock
import kotlin.time.ExperimentalTime

interface Clock {
    fun currentTimeMillis(): Long
}

object SystemClock : Clock {
    @OptIn(ExperimentalTime::class)
    override fun currentTimeMillis(): Long = KotlinClock.System.now().toEpochMilliseconds()
}

/**
 * Extension function for convenience - alias for currentTimeMillis()
 */
fun Clock.currentTimeMs(): Long = currentTimeMillis()

