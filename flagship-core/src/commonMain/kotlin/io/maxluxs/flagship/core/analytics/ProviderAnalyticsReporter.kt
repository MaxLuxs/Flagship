package io.maxluxs.flagship.core.analytics

import io.maxluxs.flagship.core.provider.ProviderMetrics
import io.maxluxs.flagship.core.provider.ProviderMetricsTracker
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.Clock
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

/**
 * Reporter for sending provider metrics to the Flagship server.
 * 
 * Features:
 * - Automatic periodic reporting (every N minutes)
 * - Batching metrics for efficiency
 * - Retry logic with exponential backoff
 * - Configurable reporting interval and batch size
 * 
 * @property projectId Project ID for metrics
 * @property analyticsUrl Base URL for analytics endpoint (e.g., "https://api.flagship.io")
 * @property apiKey API key for authentication (optional, can use project-based auth)
 * @property metricsTracker ProviderMetricsTracker instance
 * @property reportingIntervalMs Interval between reports in milliseconds (default: 5 minutes)
 * @property batchSize Maximum number of metrics to send in one batch (default: 10)
 * @property maxRetries Maximum number of retry attempts (default: 3)
 * @property logger Logger for debug messages
 * @property clock Clock for time operations
 */
class ProviderAnalyticsReporter(
    private val projectId: String,
    private val analyticsUrl: String,
    private val apiKey: String? = null,
    private val metricsTracker: ProviderMetricsTracker,
    private val reportingIntervalMs: Long = 5 * 60 * 1000L, // 5 minutes
    private val batchSize: Int = 10,
    private val maxRetries: Int = 3,
    private val logger: FlagsLogger = NoopLogger,
    private val clock: Clock = SystemClock
) {
    private var reportingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Start periodic reporting of provider metrics.
     */
    fun start() {
        if (reportingJob?.isActive == true) {
            logger.warn("ProviderAnalyticsReporter", "Reporting already started")
            return
        }
        
        logger.info("ProviderAnalyticsReporter", "Starting periodic reporting (interval: ${reportingIntervalMs}ms)")
        
        reportingJob = scope.launch {
            while (isActive) {
                try {
                    delay(reportingIntervalMs)
                    reportMetrics()
                } catch (e: CancellationException) {
                    logger.info("ProviderAnalyticsReporter", "Reporting cancelled")
                    break
                } catch (e: Exception) {
                    logger.error("ProviderAnalyticsReporter", "Error in reporting loop", e)
                    // Continue reporting even if one attempt fails
                }
            }
        }
    }
    
    /**
     * Stop periodic reporting.
     */
    fun stop() {
        reportingJob?.cancel()
        reportingJob = null
        logger.info("ProviderAnalyticsReporter", "Stopped periodic reporting")
    }
    
    /**
     * Report metrics immediately (useful for testing or manual triggers).
     */
    suspend fun reportMetrics() {
        val allMetrics = metricsTracker.getAllMetrics()
        if (allMetrics.isEmpty()) {
            logger.debug("ProviderAnalyticsReporter", "No metrics to report")
            return
        }
        
        // Batch metrics
        val batches = allMetrics.values.chunked(batchSize)
        
        for (batch in batches) {
            try {
                sendBatch(batch)
            } catch (e: Exception) {
                logger.error("ProviderAnalyticsReporter", "Failed to send batch", e)
                // Continue with next batch even if one fails
            }
        }
    }
    
    private suspend fun sendBatch(metrics: List<ProviderMetrics>) {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < maxRetries) {
            try {
                sendMetricsWithRetry(metrics)
                logger.debug("ProviderAnalyticsReporter", "Successfully sent ${metrics.size} metrics")
                return
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                if (attempt < maxRetries) {
                    val delayMs = exponentialBackoff(attempt)
                    logger.warn("ProviderAnalyticsReporter", "Retry $attempt/$maxRetries after ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }
        
        // All retries failed
        logger.error("ProviderAnalyticsReporter", "Failed to send metrics after $maxRetries attempts", lastException)
        throw lastException ?: Exception("Failed to send metrics")
    }
    
    private suspend fun sendMetricsWithRetry(metrics: List<ProviderMetrics>) {
        val requests = metrics.map { it.toRequest() }
        sendMetricsActual(analyticsUrl, projectId, apiKey, requests)
    }
    
    /**
     * Platform-specific implementation for sending metrics.
     * Default implementation does nothing - override in platform-specific code.
     */
    protected open suspend fun sendMetricsActual(
        analyticsUrl: String,
        projectId: String,
        apiKey: String?,
        metrics: List<ProviderMetricsRequest>
    ) {
        // Default: no-op, override in platform-specific implementations
    }
    
    private fun exponentialBackoff(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, ...
        return (1000L * (1 shl (attempt - 1))).coerceAtMost(30000L) // Max 30 seconds
    }
    
    /**
     * Convert ProviderMetrics to request format.
     */
    private fun ProviderMetrics.toRequest(): ProviderMetricsRequest {
        return ProviderMetricsRequest(
            providerName = providerName,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            averageResponseTimeMs = averageResponseTimeMs,
            lastRequestTimeMs = lastRequestTimeMs,
            lastSuccessTimeMs = lastSuccessTimeMs,
            lastFailureTimeMs = lastFailureTimeMs,
            consecutiveFailures = consecutiveFailures
        )
    }
}

@Serializable
data class ProviderMetricsRequest(
    val providerName: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMs: Double,
    val lastRequestTimeMs: Long?,
    val lastSuccessTimeMs: Long?,
    val lastFailureTimeMs: Long?,
    val consecutiveFailures: Int
)

