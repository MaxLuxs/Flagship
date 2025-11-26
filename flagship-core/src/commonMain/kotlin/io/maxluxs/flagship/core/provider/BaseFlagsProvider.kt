package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.ProviderMetricsTracker
import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base implementation for FlagsProvider that handles common patterns.
 * 
 * Most providers follow the same structure:
 * - Store a snapshot internally
 * - Evaluate flags/experiments from the snapshot
 * - Use BucketingEngine for experiment assignment
 * - Track health status
 * 
 * Subclasses only need to implement snapshot fetching logic.
 */
abstract class BaseFlagsProvider(
    override val name: String,
    private val clock: Clock = SystemClock,
    private var metricsTracker: ProviderMetricsTracker? = null
) : FlagsProvider {
    
    protected var snapshot: ProviderSnapshot = ProviderSnapshot(
        flags = emptyMap(),
        experiments = emptyMap(),
        revision = null,
        fetchedAtMs = 0L
    )
    
    /**
     * Thread-safe cache for last successful snapshot.
     * Used by subclasses for fallback on errors.
     */
    protected val snapshotCache = SnapshotCache()
    
    /**
     * Optional error handler for common error handling patterns.
     * Subclasses can create and use this for retry and fallback logic.
     */
    protected var errorHandler: ProviderErrorHandler? = null
    
    private var lastSuccessfulFetchMs: Long? = null
    private var consecutiveFailures: Int = 0
    private val healthMutex = Mutex()
    
    /**
     * Set metrics tracker for this provider.
     * Can be called after provider creation to enable metrics tracking.
     */
    fun setMetricsTracker(tracker: ProviderMetricsTracker?) {
        metricsTracker = tracker
    }
    
    /**
     * Fetch and update the snapshot.
     * Subclasses should implement this to fetch from their specific backend.
     * 
     * @param currentRevision Optional current revision for incremental updates
     * @return New ProviderSnapshot
     */
    protected abstract suspend fun fetchSnapshot(currentRevision: String? = null): ProviderSnapshot
    
    override suspend fun bootstrap(): ProviderSnapshot {
        val startTime = clock.currentTimeMs()
        metricsTracker?.recordRequest(name, startTime)
        
        return try {
            snapshot = fetchSnapshot()
            snapshotCache.update(snapshot) // Cache successful snapshot
            val endTime = clock.currentTimeMs()
            metricsTracker?.recordSuccess(name, startTime, endTime)
            recordSuccess()
            snapshot
        } catch (e: Exception) {
            val endTime = clock.currentTimeMs()
            metricsTracker?.recordFailure(name, startTime, endTime)
            recordFailure()
            throw e
        }
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        val startTime = clock.currentTimeMs()
        metricsTracker?.recordRequest(name, startTime)
        
        return try {
            snapshot = fetchSnapshot(snapshot.revision)
            snapshotCache.update(snapshot) // Cache successful snapshot
            val endTime = clock.currentTimeMs()
            metricsTracker?.recordSuccess(name, startTime, endTime)
            recordSuccess()
            snapshot
        } catch (e: Exception) {
            val endTime = clock.currentTimeMs()
            metricsTracker?.recordFailure(name, startTime, endTime)
            recordFailure()
            throw e
        }
    }
    
    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return snapshot.flags[key]
    }
    
    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = snapshot.experiments[key] ?: return null
        return BucketingEngine.assign(experiment, context)
    }
    
    override fun isHealthy(): Boolean {
        // Note: This is a simplified non-suspend version
        // For thread-safety, consider making this suspend or using atomic operations
        return try {
            // Access health status without lock for non-suspend function
            // This is safe for read-only access in most cases
            if (lastSuccessfulFetchMs == null) {
                return false // Never fetched successfully
            }
            
            // Check if snapshot is expired
            val ttlMs = snapshot.ttlMs
            if (ttlMs != null) {
                val age = clock.currentTimeMs() - snapshot.fetchedAtMs
                if (age > ttlMs) {
                    return false // Expired
                }
            }
            
            // Check consecutive failures
            if (consecutiveFailures >= 5) {
                return false // Too many failures
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getLastSuccessfulFetchMs(): Long? {
        // Note: Non-suspend version - read-only access is generally safe
        return lastSuccessfulFetchMs
    }
    
    override fun getConsecutiveFailures(): Int {
        // Note: Non-suspend version - read-only access is generally safe
        return consecutiveFailures
    }
    
    private suspend fun recordSuccess() {
        healthMutex.withLock {
            lastSuccessfulFetchMs = clock.currentTimeMs()
            consecutiveFailures = 0
        }
    }
    
    private suspend fun recordFailure() {
        healthMutex.withLock {
            consecutiveFailures++
        }
    }
}

