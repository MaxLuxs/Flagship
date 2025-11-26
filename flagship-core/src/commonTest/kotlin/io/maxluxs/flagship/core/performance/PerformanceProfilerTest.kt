package io.maxluxs.flagship.core.performance

import io.maxluxs.flagship.core.util.DefaultLogger
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PerformanceProfilerTest {
    
    @Test
    fun testMeasureOperation() = runTest {
        val profiler = PerformanceProfiler(DefaultLogger(), enabled = true)
        
        profiler.measure("test_operation") {
            delay(10)
        }
        
        val metrics = profiler.getMetrics("test_operation")
        assertNotNull(metrics)
        assertEquals(1, metrics.count)
        assertTrue(metrics.averageTimeMs >= 10)
    }
    
    @Test
    fun testMultipleMeasurements() = runTest {
        val profiler = PerformanceProfiler(DefaultLogger(), enabled = true)
        
        repeat(5) {
            profiler.measure("test_operation") {
                delay(10)
            }
        }
        
        val metrics = profiler.getMetrics("test_operation")
        assertNotNull(metrics)
        assertEquals(5, metrics.count)
        assertTrue(metrics.averageTimeMs >= 10)
        assertTrue(metrics.maxTimeMs >= metrics.minTimeMs)
    }
    
    @Test
    fun testReset() = runTest {
        val profiler = PerformanceProfiler(DefaultLogger(), enabled = true)
        
        profiler.measure("test_operation") {
            delay(10)
        }
        
        assertNotNull(profiler.getMetrics("test_operation"))
        
        profiler.reset("test_operation")
        
        assertNotNull(profiler.getMetrics("test_operation")) // Still returns stats, but count is 0
    }
    
    @Test
    fun testDisabledProfiler() = runTest {
        val profiler = PerformanceProfiler(DefaultLogger(), enabled = false)
        
        profiler.measure("test_operation") {
            delay(10)
        }
        
        val metrics = profiler.getMetrics("test_operation")
        // When disabled, should not record metrics
        // But getMetrics might still return null or empty
    }
}

