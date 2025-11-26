package io.maxluxs.flagship.provider.launchdarkly

import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.provider.ProviderErrorHandler
import io.maxluxs.flagship.core.retry.ExponentialBackoffRetry
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.util.ExperimentParser
import io.maxluxs.flagship.core.util.FlagValueParser
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock

/**
 * LaunchDarkly provider for Flagship with error handling, retry logic, and caching.
 * Integrates with LaunchDarkly SDK for enterprise feature flag management.
 * 
 * @param adapter Platform-specific LaunchDarkly adapter
 * @param name Provider name (default: "launchdarkly")
 * @param knownFlagKeys Optional list of known flag keys. If provided, these keys will be
 *                      explicitly fetched in getAllFlags(). Useful when LaunchDarkly SDK
 *                      doesn't provide a way to enumerate all flags.
 * @param retryPolicy Retry policy (default: ExponentialBackoffRetry with 3 attempts)
 * @param logger Logger for debug messages
 */
class LaunchDarklyProvider(
    private val adapter: LaunchDarklyAdapter,
    name: String = "launchdarkly",
    private val knownFlagKeys: List<String>? = null,
    private val retryPolicy: RetryPolicy = ExponentialBackoffRetry(maxAttempts = 3),
    private val logger: FlagsLogger = NoopLogger
) : BaseFlagsProvider(name) {
    
    init {
        // Initialize error handler for common error handling
        errorHandler = ProviderErrorHandler(
            providerName = name,
            retryPolicy = retryPolicy,
            logger = logger,
            snapshotCache = snapshotCache
        )
    }
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        return errorHandler?.fetchWithFallback {
            fetchWithErrorHandling()
        } ?: fetchWithErrorHandling() // Fallback if errorHandler not set
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        return errorHandler?.fetchWithFallback {
            fetchWithErrorHandling()
        } ?: fetchWithErrorHandling() // Fallback if errorHandler not set
    }
    
    private suspend fun fetchWithErrorHandling(): ProviderSnapshot {
        return try {
            adapter.initialize()
            val snapshot = parseSnapshot()
            
            // Cache successful snapshot (also cached in BaseFlagsProvider.bootstrap/refresh)
            snapshotCache.update(snapshot)
            
            logger.info(name, "Successfully fetched snapshot from LaunchDarkly")
            snapshot
        } catch (e: Exception) {
            // Use errorHandler to categorize error if available
            throw errorHandler?.categorizeError(e, "LaunchDarkly")
                ?: ProviderException(name, "LaunchDarkly error: ${e.message}", e)
        }
    }

    private fun parseSnapshot(): ProviderSnapshot {
        val allFlags = adapter.getAllFlags(knownFlagKeys)
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()

        allFlags.forEach { (key, value) ->
            when {
                key.startsWith("exp_") -> {
                    // Parse as experiment
                    parseExperiment(key, value)?.let { experiments[key] = it }
                }
                else -> {
                    // Parse as simple flag
                    parseFlag(value)?.let { flags[key] = it }
                }
            }
        }

        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = adapter.getRevision(),
            fetchedAtMs = SystemClock.currentTimeMillis(),
            ttlMs = 15 * 60_000 // 15 minutes default
        )
    }

    private fun parseFlag(value: Any?): FlagValue? {
        return FlagValueParser.parseFromAny(value)
    }

    private fun parseExperiment(key: String, value: Any?): ExperimentDefinition? {
        if (value !is String) return null
        return ExperimentParser.parseExperiment(key, value)
    }
}

/**
 * Adapter interface for LaunchDarkly SDK.
 * Platform-specific implementations wrap native LaunchDarkly SDKs.
 */
interface LaunchDarklyAdapter {
    suspend fun initialize()
    suspend fun refresh()
    
    /**
     * Get all flags from LaunchDarkly.
     * 
     * Note: LaunchDarkly SDK doesn't provide a direct way to get all flags.
     * If [knownKeys] is provided, will fetch flags using direct lookups.
     * Otherwise, returns empty map and provider will rely on direct flag lookups.
     * 
     * @param knownKeys Optional list of known flag keys to fetch explicitly
     * @return Map of flag keys to their values
     */
    fun getAllFlags(knownKeys: List<String>? = null): Map<String, Any?>
    
    fun getRevision(): String?
}

