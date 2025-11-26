package io.maxluxs.flagship.admin.ui.compose.util

import kotlinx.coroutines.delay

/**
 * Retry utility for network operations with exponential backoff.
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    multiplier: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(currentDelay)
                currentDelay = (currentDelay * multiplier).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }
    
    throw lastException ?: Exception("Retry failed after $maxRetries attempts")
}

