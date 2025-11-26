package io.maxluxs.flagship.core.performance

import io.maxluxs.flagship.core.util.currentTimeMillis

import io.maxluxs.flagship.core.util.FlagsLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Performance profiler for Flagship operations.
 * 
 * Tracks execution times and memory usage for various operations.
 */
class PerformanceProfiler(
    private val logger: FlagsLogger,
    private val enabled: Boolean = true
) {
    private data class OperationMetrics(
        val operation: String,
        var count: Int = 0,
        var totalTimeMs: Long = 0,
        var minTimeMs: Long = Long.MAX_VALUE,
        var maxTimeMs: Long = 0,
        var lastTimeMs: Long = 0
    )
    
    private val metrics = mutableMapOf<String, OperationMetrics>()
    private val mutex = Mutex()
    
    /**
     * Measure execution time of an operation.
     */
    suspend fun <T> measure(operation: String, block: suspend () -> T): T {
        if (!enabled) return block()
        
        val startTime = currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = currentTimeMillis() - startTime
            recordMetric(operation, duration)
        }
    }
    
    /**
     * Record a metric manually.
     */
    suspend fun recordMetric(operation: String, durationMs: Long) {
        if (!enabled) return
        
        mutex.withLock {
            val metric = metrics.getOrPut(operation) { OperationMetrics(operation) }
            metric.count++
            metric.totalTimeMs += durationMs
            metric.minTimeMs = minOf(metric.minTimeMs, durationMs)
            metric.maxTimeMs = maxOf(metric.maxTimeMs, durationMs)
            metric.lastTimeMs = durationMs
        }
    }
    
    /**
     * Get metrics for an operation.
     */
    suspend fun getMetrics(operation: String): OperationStats? {
        return mutex.withLock {
            metrics[operation]?.let { metric ->
                OperationStats(
                    operation = metric.operation,
                    count = metric.count,
                    averageTimeMs = metric.totalTimeMs / metric.count,
                    minTimeMs = metric.minTimeMs,
                    maxTimeMs = metric.maxTimeMs,
                    totalTimeMs = metric.totalTimeMs,
                    lastTimeMs = metric.lastTimeMs
                )
            }
        }
    }
    
    /**
     * Get all metrics.
     */
    suspend fun getAllMetrics(): Map<String, OperationStats> {
        return mutex.withLock {
            metrics.mapValues { (_, metric) ->
                OperationStats(
                    operation = metric.operation,
                    count = metric.count,
                    averageTimeMs = metric.totalTimeMs / metric.count,
                    minTimeMs = metric.minTimeMs,
                    maxTimeMs = metric.maxTimeMs,
                    totalTimeMs = metric.totalTimeMs,
                    lastTimeMs = metric.lastTimeMs
                )
            }
        }
    }
    
    /**
     * Reset metrics for an operation.
     */
    suspend fun reset(operation: String) {
        mutex.withLock {
            metrics.remove(operation)
        }
    }
    
    /**
     * Reset all metrics.
     */
    suspend fun resetAll() {
        mutex.withLock {
            metrics.clear()
        }
    }
    
    /**
     * Log performance report.
     */
    suspend fun logReport() {
        if (!enabled) return
        
        val allMetrics = getAllMetrics()
        if (allMetrics.isEmpty()) {
            logger.info("PerformanceProfiler", "No metrics recorded")
            return
        }
        
        logger.info("PerformanceProfiler", "=== Performance Report ===")
        allMetrics.forEach { (operation, stats) ->
            logger.info("PerformanceProfiler", """
                |$operation:
                |  Count: ${stats.count}
                |  Avg: ${stats.averageTimeMs}ms
                |  Min: ${stats.minTimeMs}ms
                |  Max: ${stats.maxTimeMs}ms
                |  Total: ${stats.totalTimeMs}ms
            """.trimMargin())
        }
    }
}

/**
 * Performance statistics for an operation.
 */
data class OperationStats(
    val operation: String,
    val count: Int,
    val averageTimeMs: Long,
    val minTimeMs: Long,
    val maxTimeMs: Long,
    val totalTimeMs: Long,
    val lastTimeMs: Long
)

