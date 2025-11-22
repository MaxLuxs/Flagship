package io.maxluxs.flagship.core.util

import kotlin.math.min
import kotlin.math.pow

/**
 * Utility for calculating exponential backoff delays.
 * 
 * Used by both RetryPolicy and RealtimeManager to ensure consistent backoff behavior.
 */
object BackoffCalculator {
    /**
     * Calculate exponential backoff delay.
     * 
     * Formula: initialDelayMs * (factor ^ attempt)
     * 
     * @param attempt Current attempt number (1-based)
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @param factor Exponential factor (default: 2.0)
     * @return Calculated delay in milliseconds
     */
    fun calculateDelay(
        attempt: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        factor: Double = 2.0
    ): Long {
        val delay = initialDelayMs * factor.pow(attempt - 1).toLong()
        return min(delay, maxDelayMs)
    }
    
    /**
     * Calculate next delay for reconnection (incremental backoff).
     * 
     * Used for continuous reconnection scenarios where attempt number
     * is tracked separately.
     * 
     * @param currentDelay Current delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @param factor Multiplier for each step (default: 2.0)
     * @return Next delay in milliseconds
     */
    fun nextDelay(
        currentDelay: Long,
        maxDelayMs: Long,
        factor: Double = 2.0
    ): Long {
        val nextDelay = (currentDelay * factor).toLong()
        return min(nextDelay, maxDelayMs)
    }
}

