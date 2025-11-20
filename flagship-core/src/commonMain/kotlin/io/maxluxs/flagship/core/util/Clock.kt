package io.maxluxs.flagship.core.util

interface Clock {
    fun currentTimeMillis(): Long
}

object SystemClock : Clock {
    override fun currentTimeMillis(): Long = io.maxluxs.flagship.core.util.currentTimeMillis()
}

