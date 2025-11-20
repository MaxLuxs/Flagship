package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.model.*

/**
 * Provider interface for fetching and evaluating feature flags and experiments.
 * 
 * Providers fetch configuration from various backends (REST API, Firebase Remote Config,
 * custom sources) and provide flag/experiment values.
 * 
 * Multiple providers can be composed with precedence:
 * - First provider in the list has highest priority
 * - Subsequent providers fill gaps for missing flags
 * 
 * Example implementation:
 * ```kotlin
 * class RestFlagsProvider(
 *     private val client: HttpClient,
 *     private val baseUrl: String,
 *     override val name: String = "rest"
 * ) : FlagsProvider {
 *     override suspend fun bootstrap(): ProviderSnapshot {
 *         val response = client.get("$baseUrl/config").body<ConfigResponse>()
 *         return response.toSnapshot()
 *     }
 *     // ... other methods
 * }
 * ```
 */
interface FlagsProvider {
    /**
     * Unique name for this provider (e.g., "rest", "firebase", "custom").
     * Used for logging and cache keys.
     */
    val name: String

    /**
     * Initial configuration load before first evaluation.
     * 
     * This is called once during app initialization and should:
     * - Fetch the latest configuration from the backend
     * - Return a complete snapshot of flags and experiments
     * - Block until data is available or timeout occurs
     * 
     * @return Provider snapshot with flags and experiments
     * @throws Exception if bootstrap fails (will fall back to cached data)
     */
    suspend fun bootstrap(): ProviderSnapshot

    /**
     * Background refresh of configuration.
     * 
     * Called periodically to update flags/experiments. Should:
     * - Fetch updated configuration from the backend
     * - Return a new snapshot with any changes
     * - Be non-blocking to the UI
     * 
     * @return Updated provider snapshot
     * @throws Exception if refresh fails (keeps using current snapshot)
     */
    suspend fun refresh(): ProviderSnapshot

    /**
     * Evaluate a specific flag for the given context.
     * 
     * This method can be used for provider-specific evaluation logic,
     * but most implementations simply return the cached value from the snapshot.
     * 
     * @param key The flag key to evaluate
     * @param context The evaluation context with user/device information
     * @return The flag value, or null if not found in this provider
     */
    fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue?

    /**
     * Evaluate an experiment and assign a variant for the given context.
     * 
     * This method can implement custom bucketing logic, but most implementations
     * delegate to the standard bucketing engine.
     * 
     * @param key The experiment key to evaluate
     * @param context The evaluation context for targeting and bucketing
     * @return The experiment assignment with variant, or null if user doesn't qualify
     */
    fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment?
}

