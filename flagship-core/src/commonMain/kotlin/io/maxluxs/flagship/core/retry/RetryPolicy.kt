package io.maxluxs.flagship.core.retry

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry policy for failed provider operations.
 * 
 * Defines how many times to retry and how long to wait between attempts.
 */
interface RetryPolicy {
    /**
     * Maximum number of retry attempts.
     */
    val maxAttempts: Int

    /**
     * Calculate delay before next retry attempt.
     * 
     * @param attempt Current attempt number (1-based)
     * @return Delay in milliseconds
     */
    fun getDelayMs(attempt: Int): Long

    /**
     * Check if operation should be retried.
     * 
     * @param attempt Current attempt number (1-based)
     * @param error The error that occurred
     * @return true if should retry, false otherwise
     */
    fun shouldRetry(attempt: Int, error: Throwable): Boolean
}

/**
 * Exponential backoff retry policy.
 * 
 * Delays grow exponentially: initialDelayMs * (2 ^ attempt)
 * 
 * Example delays with initialDelayMs=1000:
 * - Attempt 1: 1s
 * - Attempt 2: 2s
 * - Attempt 3: 4s
 * - Attempt 4: 8s (capped at maxDelayMs)
 * 
 * @property maxAttempts Maximum number of retry attempts (default: 3)
 * @property initialDelayMs Initial delay in milliseconds (default: 1000ms)
 * @property maxDelayMs Maximum delay in milliseconds (default: 30000ms)
 * @property factor Exponential factor (default: 2.0)
 */
class ExponentialBackoffRetry(
    override val maxAttempts: Int = 3,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
    private val factor: Double = 2.0
) : RetryPolicy {
    override fun getDelayMs(attempt: Int): Long {
        val delay = initialDelayMs * factor.pow(attempt - 1).toLong()
        return min(delay, maxDelayMs)
    }

    override fun shouldRetry(attempt: Int, error: Throwable): Boolean {
        return attempt < maxAttempts
    }
}

/**
 * No retry policy - never retry on failure.
 */
object NoRetryPolicy : RetryPolicy {
    override val maxAttempts: Int = 1

    override fun getDelayMs(attempt: Int): Long = 0

    override fun shouldRetry(attempt: Int, error: Throwable): Boolean = false
}

/**
 * Execute a suspending operation with retry policy.
 * 
 * @param policy The retry policy to use
 * @param operation The operation to execute
 * @return The result of the operation
 * @throws Exception if all retries are exhausted
 */
suspend fun <T> retryWithPolicy(
    policy: RetryPolicy,
    operation: suspend () -> T
): T {
    var attempt = 1
    var lastError: Throwable? = null

    while (attempt <= policy.maxAttempts) {
        try {
            return operation()
        } catch (e: Exception) {
            lastError = e
            
            if (!policy.shouldRetry(attempt, e)) {
                throw e
            }

            if (attempt < policy.maxAttempts) {
                val delayMs = policy.getDelayMs(attempt)
                delay(delayMs)
            }

            attempt++
        }
    }

    throw lastError ?: Exception("Retry policy exhausted")
}

