package io.maxluxs.flagship.core.util

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual inline fun <T> synchronized(lock: Any, block: () -> T): T =
    kotlin.synchronized(lock, block)

