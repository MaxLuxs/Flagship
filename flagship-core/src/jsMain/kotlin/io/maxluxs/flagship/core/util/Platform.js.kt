package io.maxluxs.flagship.core.util

actual fun currentTimeMillis(): Long = 
    kotlin.js.Date.now().toLong()

actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // JS is single-threaded, so synchronization is not needed
    // Just execute the block directly
    return block()
}

