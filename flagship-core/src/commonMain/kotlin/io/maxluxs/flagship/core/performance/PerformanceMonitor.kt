package io.maxluxs.flagship.core.performance

import io.maxluxs.flagship.core.util.currentTimeMillis

/**
 * Performance monitoring for flag operations.
 * 
 * Tracks timing and performance metrics for flag evaluations,
 * provider operations, and other critical paths.
 */
interface PerformanceMonitor {
    /**
     * Start timing an operation.
     * 
     * @param operation Name of the operation (e.g., "bootstrap", "evaluate_flag")
     * @return A timer handle to stop timing
     */
    fun startTimer(operation: String): Timer

    /**
     * Record a custom metric.
     * 
     * @param metric Name of the metric
     * @param value Value to record
     * @param unit Unit of measurement (e.g., "ms", "count", "bytes")
     */
    fun recordMetric(metric: String, value: Double, unit: String)

    /**
     * Record a count metric.
     * 
     * @param metric Name of the metric
     * @param count Count to record
     */
    fun recordCount(metric: String, count: Long)
}

/**
 * Timer handle for measuring operation duration.
 */
interface Timer {
    /**
     * Stop the timer and record the duration.
     * 
     * @return Duration in milliseconds
     */
    fun stop(): Long
}

/**
 * No-op performance monitor that does nothing.
 */
object NoopPerformanceMonitor : PerformanceMonitor {
    override fun startTimer(operation: String): Timer = NoopTimer
    
    override fun recordMetric(metric: String, value: Double, unit: String) {}
    
    override fun recordCount(metric: String, count: Long) {}

    private object NoopTimer : Timer {
        override fun stop(): Long = 0
    }
}

/**
 * Simple in-memory performance monitor for development and debugging.
 */
class InMemoryPerformanceMonitor : PerformanceMonitor {
    private val metrics = mutableMapOf<String, MutableList<Double>>()
    private val counts = mutableMapOf<String, Long>()

    override fun startTimer(operation: String): Timer {
        val startTime = currentTimeMillis()
        return object : Timer {
            override fun stop(): Long {
                val duration = currentTimeMillis() - startTime
                recordMetric(operation, duration.toDouble(), "ms")
                return duration
            }
        }
    }

    override fun recordMetric(metric: String, value: Double, unit: String) {
        metrics.getOrPut(metric) { mutableListOf() }.add(value)
    }

    override fun recordCount(metric: String, count: Long) {
        counts[metric] = (counts[metric] ?: 0) + count
    }

    /**
     * Get statistics for a metric.
     * 
     * @param metric Name of the metric
     * @return Statistics (avg, min, max, count) or null if no data
     */
    fun getStats(metric: String): MetricStats? {
        val values = metrics[metric] ?: return null
        if (values.isEmpty()) return null

        return MetricStats(
            count = values.size,
            sum = values.sum(),
            avg = values.average(),
            min = values.minOrNull() ?: 0.0,
            max = values.maxOrNull() ?: 0.0
        )
    }

    /**
     * Get a count metric value.
     */
    fun getCount(metric: String): Long = counts[metric] ?: 0

    /**
     * Clear all metrics.
     */
    fun clear() {
        metrics.clear()
        counts.clear()
    }
}

/**
 * Statistics for a performance metric.
 */
data class MetricStats(
    val count: Int,
    val sum: Double,
    val avg: Double,
    val min: Double,
    val max: Double
)

