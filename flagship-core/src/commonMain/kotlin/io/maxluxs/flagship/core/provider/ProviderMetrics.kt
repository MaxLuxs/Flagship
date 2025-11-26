package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Performance metrics for a provider.
 */
data class ProviderMetrics(
    val providerName: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMs: Double,
    val lastRequestTimeMs: Long?,
    val lastSuccessTimeMs: Long?,
    val lastFailureTimeMs: Long?,
    val consecutiveFailures: Int
) {
    val successRate: Double
        get() = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0
}

/**
 * Tracks performance metrics for providers.
 */
class ProviderMetricsTracker(
    private val clock: Clock = SystemClock
) {
    private val metrics = mutableMapOf<String, ProviderMetricsData>()
    private val mutex = Mutex()
    
    private data class ProviderMetricsData(
        var totalRequests: Long = 0,
        var successfulRequests: Long = 0,
        var failedRequests: Long = 0,
        var totalResponseTimeMs: Long = 0,
        var lastRequestTimeMs: Long? = null,
        var lastSuccessTimeMs: Long? = null,
        var lastFailureTimeMs: Long? = null,
        var consecutiveFailures: Int = 0
    )
    
    suspend fun recordRequest(providerName: String, startTimeMs: Long) {
        mutex.withLock {
            val data = metrics.getOrPut(providerName) { ProviderMetricsData() }
            data.totalRequests++
            data.lastRequestTimeMs = startTimeMs
        }
    }
    
    suspend fun recordSuccess(providerName: String, startTimeMs: Long, endTimeMs: Long) {
        mutex.withLock {
            val data = metrics.getOrPut(providerName) { ProviderMetricsData() }
            data.successfulRequests++
            data.totalResponseTimeMs += (endTimeMs - startTimeMs)
            data.lastSuccessTimeMs = endTimeMs
            data.consecutiveFailures = 0
        }
    }
    
    suspend fun recordFailure(providerName: String, startTimeMs: Long, endTimeMs: Long) {
        mutex.withLock {
            val data = metrics.getOrPut(providerName) { ProviderMetricsData() }
            data.failedRequests++
            data.lastFailureTimeMs = endTimeMs
            data.consecutiveFailures++
        }
    }
    
    suspend fun getMetrics(providerName: String): ProviderMetrics? {
        return mutex.withLock {
            val data = metrics[providerName] ?: return null
            
            val avgResponseTime = if (data.successfulRequests > 0) {
                data.totalResponseTimeMs.toDouble() / data.successfulRequests
            } else {
                0.0
            }
            
            ProviderMetrics(
                providerName = providerName,
                totalRequests = data.totalRequests,
                successfulRequests = data.successfulRequests,
                failedRequests = data.failedRequests,
                averageResponseTimeMs = avgResponseTime,
                lastRequestTimeMs = data.lastRequestTimeMs,
                lastSuccessTimeMs = data.lastSuccessTimeMs,
                lastFailureTimeMs = data.lastFailureTimeMs,
                consecutiveFailures = data.consecutiveFailures
            )
        }
    }
    
    suspend fun getAllMetrics(): Map<String, ProviderMetrics> {
        return mutex.withLock {
            metrics.keys.mapNotNull { name ->
                getMetrics(name)
            }.associateBy { it.providerName }
        }
    }
    
    suspend fun reset(providerName: String) {
        mutex.withLock {
            metrics.remove(providerName)
        }
    }
    
    suspend fun resetAll() {
        mutex.withLock {
            metrics.clear()
        }
    }
}

