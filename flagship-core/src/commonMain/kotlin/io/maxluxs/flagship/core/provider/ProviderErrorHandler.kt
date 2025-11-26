package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.retry.retryWithPolicy
import io.maxluxs.flagship.core.util.FlagsLogger

/**
 * Common error handling for providers.
 * 
 * Handles retry logic, fallback to cached snapshot, and error categorization.
 * 
 * Example usage:
 * ```kotlin
 * val errorHandler = ProviderErrorHandler(
 *     providerName = "rest",
 *     retryPolicy = ExponentialBackoffRetry(maxAttempts = 3),
 *     logger = logger,
 *     snapshotCache = snapshotCache
 * )
 * 
 * val snapshot = errorHandler.fetchWithFallback {
 *     fetchFromBackend()
 * }
 * ```
 */
class ProviderErrorHandler(
    private val providerName: String,
    private val retryPolicy: RetryPolicy,
    private val logger: FlagsLogger,
    private val snapshotCache: SnapshotCache
) {
    /**
     * Execute fetch with retry and fallback to cache.
     * 
     * This method:
     * 1. Tries to fetch using retry policy
     * 2. On failure, falls back to cached snapshot
     * 3. Throws ProviderException if both fail
     * 
     * @param fetch The fetch operation to execute
     * @return ProviderSnapshot from fetch or cache
     * @throws ProviderException if fetch fails and no cached snapshot is available
     */
    suspend fun fetchWithFallback(
        fetch: suspend () -> ProviderSnapshot
    ): ProviderSnapshot {
        return try {
            retryWithPolicy(retryPolicy) {
                val snapshot = fetch()
                // Update cache on successful fetch
                snapshotCache.update(snapshot)
                snapshot
            }
        } catch (e: Exception) {
            logger.error(providerName, "Fetch failed, using cached snapshot", e)
            snapshotCache.get() ?: throw ProviderException(
                providerName,
                "Fetch failed and no cached snapshot available: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Categorize error for better error messages.
     * 
     * Analyzes the error and creates a ProviderException with appropriate message.
     * 
     * @param e The exception to categorize
     * @param providerType The type of provider (e.g., "REST", "Firebase", "LaunchDarkly")
     * @return ProviderException with categorized error message
     */
    fun categorizeError(e: Exception, providerType: String): ProviderException {
        val errorMessage = e.message ?: "Unknown error"
        
        return when {
            errorMessage.contains("network", ignoreCase = true) -> {
                ProviderException(providerName, "Network error: $errorMessage", e)
            }
            errorMessage.contains("timeout", ignoreCase = true) -> {
                ProviderException(providerName, "Timeout error: $errorMessage", e)
            }
            errorMessage.contains("quota", ignoreCase = true) -> {
                ProviderException(providerName, "Quota exceeded: $errorMessage", e)
            }
            errorMessage.contains("authentication", ignoreCase = true) -> {
                ProviderException(providerName, "Authentication error: $errorMessage", e)
            }
            else -> {
                ProviderException(providerName, "$providerType error: $errorMessage", e)
            }
        }
    }
}

