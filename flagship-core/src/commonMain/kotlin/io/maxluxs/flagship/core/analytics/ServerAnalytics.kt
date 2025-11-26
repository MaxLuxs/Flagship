package io.maxluxs.flagship.core.analytics

import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Analytics implementation that sends events to Flagship server.
 * 
 * Automatically batches events and sends them periodically to reduce network overhead.
 * 
 * Example:
 * ```kotlin
 * val analytics = ServerAnalytics(
 *     projectId = "project-id",
 *     analyticsUrl = "https://api.flagship.io",
 *     apiKey = "your-api-key"
 * )
 * 
 * val config = FlagsConfig(
 *     appKey = "my-app",
 *     analytics = analytics,
 *     // ...
 * )
 * ```
 */
class ServerAnalytics(
    private val projectId: String,
    private val analyticsUrl: String,
    private val apiKey: String? = null,
    private val batchSize: Int = 10,
    private val flushIntervalMs: Long = 30_000L, // 30 seconds
    private val maxRetries: Int = 3,
    private val logger: FlagsLogger = NoopLogger
) : FlagsAnalytics {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val eventQueue = mutableListOf<AnalyticsEvent>()
    private val queueMutex = Mutex()
    private var flushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        startPeriodicFlush()
    }
    
    private fun startPeriodicFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flush()
            }
        }
    }
    
    override fun trackFlagEvaluated(key: FlagKey, value: Any?, source: String) {
        scope.launch {
            addEvent(
                AnalyticsEvent(
                    eventType = if (value == true || value == "true") "flag_enabled" else "flag_disabled",
                    entityType = "flag",
                    entityId = key,
                    attributes = mapOf(
                        "value" to (value?.toString() ?: "null"),
                        "source" to source
                    )
                )
            )
        }
    }
    
    override fun trackExperimentAssignment(key: ExperimentKey, variant: String) {
        scope.launch {
            addEvent(
                AnalyticsEvent(
                    eventType = "experiment_assigned",
                    entityType = "experiment",
                    entityId = key,
                    attributes = mapOf("variant" to variant)
                )
            )
        }
    }
    
    override fun trackConfigRefreshed(providerName: String, success: Boolean, durationMs: Long) {
        scope.launch {
            addEvent(
                AnalyticsEvent(
                    eventType = if (success) "config_refreshed" else "config_refresh_failed",
                    entityType = "provider",
                    entityId = providerName,
                    attributes = mapOf(
                        "duration_ms" to durationMs.toString(),
                        "success" to success.toString()
                    )
                )
            )
        }
    }
    
    override fun trackError(operation: String, error: String) {
        scope.launch {
            addEvent(
                AnalyticsEvent(
                    eventType = "error",
                    entityType = "operation",
                    entityId = operation,
                    attributes = mapOf("error" to error)
                )
            )
        }
    }
    
    private suspend fun addEvent(event: AnalyticsEvent) {
        queueMutex.withLock {
            eventQueue.add(event)
            
            // Flush if batch size reached
            if (eventQueue.size >= batchSize) {
                flush()
            }
        }
    }
    
    suspend fun flush() {
        val events = queueMutex.withLock {
            val toSend = eventQueue.toList()
            eventQueue.clear()
            toSend
        }
        
        if (events.isEmpty()) return
        
        try {
            sendEventsWithRetry(events)
            logger.debug("ServerAnalytics", "Successfully sent ${events.size} events")
        } catch (e: Exception) {
            logger.error("ServerAnalytics", "Failed to send events", e)
            // Re-add events to queue for retry
            queueMutex.withLock {
                eventQueue.addAll(0, events)
            }
        }
    }
    
    private suspend fun sendEventsWithRetry(events: List<AnalyticsEvent>) {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < maxRetries) {
            try {
                sendEventsActual(analyticsUrl, projectId, apiKey, events)
                return
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                if (attempt < maxRetries) {
                    val delayMs = (1000L * attempt * attempt) // Exponential backoff
                    logger.warn("ServerAnalytics", "Retry $attempt/$maxRetries after ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }
        
        throw lastException ?: Exception("Failed to send events")
    }
    
    /**
     * Platform-specific implementation for sending events.
     * Default implementation does nothing - override in platform-specific code.
     */
    protected open suspend fun sendEventsActual(
        analyticsUrl: String,
        projectId: String,
        apiKey: String?,
        events: List<AnalyticsEvent>
    ) {
        // Default: no-op, override in platform-specific implementations
    }
    
    companion object {
        /**
         * Create a no-op analytics that does nothing.
         */
        fun noop(): FlagsAnalytics = NoopAnalytics
    }
    
    suspend fun stop() {
        flushJob?.cancel()
        scope.cancel()
        // Flush remaining events
        flush()
    }
}

@Serializable
data class AnalyticsEvent(
    val eventType: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

