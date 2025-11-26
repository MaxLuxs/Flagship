package io.maxluxs.flagship.core.circuit

import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Circuit breaker state.
 */
enum class CircuitState {
    CLOSED,    // Normal operation
    OPEN,      // Failing, rejecting requests
    HALF_OPEN  // Testing if service recovered
}

/**
 * Circuit breaker for protecting providers from cascading failures.
 * 
 * When failures exceed threshold, circuit opens and rejects requests.
 * After timeout, circuit enters half-open state to test recovery.
 * 
 * @property failureThreshold Number of failures before opening circuit (default: 5)
 * @property successThreshold Number of successes in half-open to close circuit (default: 2)
 * @property timeoutMs Time to wait before trying half-open (default: 60000ms = 1 minute)
 * @property clock Clock for time tracking (default: SystemClock)
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val successThreshold: Int = 2,
    private val timeoutMs: Long = 60_000,
    private val clock: Clock = SystemClock
) {
    private var state: CircuitState = CircuitState.CLOSED
    private var failureCount: Int = 0
    private var successCount: Int = 0
    private var lastFailureTime: Long = 0
    private val mutex = Mutex()
    
    /**
     * Execute operation with circuit breaker protection.
     * 
     * @param operation The operation to execute
     * @return Result of operation
     * @throws CircuitBreakerOpenException if circuit is open
     */
    suspend fun <T> execute(operation: suspend () -> T): T {
        mutex.withLock {
            when (state) {
                CircuitState.OPEN -> {
                    if (shouldAttemptReset()) {
                        state = CircuitState.HALF_OPEN
                        successCount = 0
                    } else {
                        throw CircuitBreakerOpenException("Circuit breaker is OPEN")
                    }
                }
                CircuitState.HALF_OPEN -> {
                    // Already in half-open, proceed
                }
                CircuitState.CLOSED -> {
                    // Normal operation
                }
            }
        }
        
        return try {
            val result = operation()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
    
    private suspend fun onSuccess() {
        mutex.withLock {
            when (state) {
                CircuitState.HALF_OPEN -> {
                    successCount++
                    if (successCount >= successThreshold) {
                        state = CircuitState.CLOSED
                        failureCount = 0
                        successCount = 0
                    }
                }
                CircuitState.CLOSED -> {
                    failureCount = 0
                }
                CircuitState.OPEN -> {
                    // Should not happen
                }
            }
        }
    }
    
    private suspend fun onFailure() {
        mutex.withLock {
            when (state) {
                CircuitState.HALF_OPEN -> {
                    state = CircuitState.OPEN
                    lastFailureTime = clock.currentTimeMs()
                    successCount = 0
                }
                CircuitState.CLOSED -> {
                    failureCount++
                    if (failureCount >= failureThreshold) {
                        state = CircuitState.OPEN
                        lastFailureTime = clock.currentTimeMs()
                    }
                }
                CircuitState.OPEN -> {
                    lastFailureTime = clock.currentTimeMs()
                }
            }
        }
    }
    
    private fun shouldAttemptReset(): Boolean {
        return clock.currentTimeMs() - lastFailureTime >= timeoutMs
    }
    
    /**
     * Get current circuit state.
     */
    suspend fun getState(): CircuitState {
        return mutex.withLock { state }
    }
    
    /**
     * Reset circuit breaker to closed state.
     */
    suspend fun reset() {
        mutex.withLock {
            state = CircuitState.CLOSED
            failureCount = 0
            successCount = 0
            lastFailureTime = 0
        }
    }
}

/**
 * Exception thrown when circuit breaker is open.
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

