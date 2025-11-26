package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

/**
 * Source of a flag value.
 * 
 * Indicates where the flag value came from, which is useful for debugging
 * and understanding flag evaluation precedence.
 */
enum class FlagSource {
    /**
     * Flag value comes from a local override (set via [FlagsManager.setOverride]).
     * Overrides have the highest precedence.
     */
    OVERRIDE,
    
    /**
     * Flag value comes directly from a provider (fresh data).
     * This indicates the value was fetched recently and is up-to-date.
     */
    PROVIDER,
    
    /**
     * Flag value comes from cache.
     * This indicates the value was retrieved from cached data, which may be stale.
     */
    CACHE,
    
    /**
     * Flag value is the default value (flag not found).
     * This indicates the flag key doesn't exist in any provider or cache.
     */
    DEFAULT
}

/**
 * Status information for a feature flag.
 * 
 * Provides detailed information about a flag's current state, including:
 * - Whether the flag exists
 * - Where the value came from (source)
 * - Any errors that occurred
 * - When the flag was last updated
 * - Which provider provided the value
 * 
 * This is useful for debugging, monitoring, and understanding flag evaluation.
 * 
 * Example:
 * ```kotlin
 * lifecycleScope.launch {
 *     val status = Flagship.getFlagStatus("new_feature")
 *     when (status.source) {
 *         FlagSource.OVERRIDE -> log.debug("Using override value")
 *         FlagSource.PROVIDER -> log.debug("Using fresh provider value")
 *         FlagSource.CACHE -> log.warn("Using cached value, may be stale")
 *         FlagSource.DEFAULT -> log.warn("Flag not found, using default")
 *     }
 *     
 *     if (status.lastError != null) {
 *         log.error("Error getting flag", status.lastError)
 *     }
 * }
 * ```
 * 
 * @property exists Whether the flag exists (not using default value)
 * @property source Where the flag value came from
 * @property lastError Last error that occurred when trying to get this flag, if any
 * @property lastUpdated Timestamp (milliseconds since epoch) when the flag was last updated, if available
 * @property providerName Name of the provider that provided this flag value, if available
 */
@Serializable
data class FlagStatus(
    val exists: Boolean,
    val source: FlagSource,
    val lastError: String? = null,
    val lastUpdated: Long? = null,
    val providerName: String? = null
) {
    /**
     * Check if the flag status indicates a healthy state.
     * 
     * A flag is considered healthy if:
     * - It exists (not using default)
     * - There's no error
     * 
     * @return true if the flag is in a healthy state
     */
    fun isHealthy(): Boolean {
        return exists && lastError == null
    }
    
    /**
     * Check if the flag value is fresh (from provider, not cache).
     * 
     * @return true if the value came from a provider (fresh), false if from cache or default
     */
    fun isFresh(): Boolean {
        return source == FlagSource.PROVIDER
    }
}

