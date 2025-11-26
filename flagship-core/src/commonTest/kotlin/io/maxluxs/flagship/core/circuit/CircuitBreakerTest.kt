package io.maxluxs.flagship.core.circuit

import io.maxluxs.flagship.core.util.SystemClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CircuitBreakerTest {
    
    @Test
    fun testCircuitOpensAfterFailures() = runTest {
        val breaker = CircuitBreaker(failureThreshold = 3, timeoutMs = 1000)
        
        // Fail 3 times
        repeat(3) {
            try {
                breaker.execute { throw Exception("Error") }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        // Circuit should be open now
        assertEquals(CircuitState.OPEN, breaker.getState())
        
        // Should throw CircuitBreakerOpenException
        assertFailsWith<CircuitBreakerOpenException> {
            breaker.execute { "success" }
        }
    }
    
    @Test
    fun testCircuitClosesAfterSuccess() = runTest {
        val breaker = CircuitBreaker(failureThreshold = 2, successThreshold = 2, timeoutMs = 100)
        
        // Open circuit
        repeat(2) {
            try {
                breaker.execute { throw Exception("Error") }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        assertEquals(CircuitState.OPEN, breaker.getState())
        
        // Wait for timeout
        kotlinx.coroutines.delay(150)
        
        // Should be in half-open now
        val result1 = breaker.execute { "success1" }
        assertEquals("success1", result1)
        
        val result2 = breaker.execute { "success2" }
        assertEquals("success2", result2)
        
        // Should be closed now
        assertEquals(CircuitState.CLOSED, breaker.getState())
    }
    
    @Test
    fun testReset() = runTest {
        val breaker = CircuitBreaker(failureThreshold = 2, timeoutMs = 1000)
        
        // Open circuit
        repeat(2) {
            try {
                breaker.execute { throw Exception("Error") }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        assertEquals(CircuitState.OPEN, breaker.getState())
        
        // Reset
        breaker.reset()
        
        assertEquals(CircuitState.CLOSED, breaker.getState())
        
        // Should work now
        val result = breaker.execute { "success" }
        assertEquals("success", result)
    }
}

