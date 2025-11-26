package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.model.*

/**
 * Provider interface for fetching and evaluating feature flags and experiments.
 * 
 * Providers fetch configuration from various backends (REST API, Firebase Remote Config,
 * custom sources) and provide flag/experiment values.
 * 
 * ## Provider Precedence
 * 
 * Multiple providers can be composed with precedence:
 * - First provider in the list has highest priority
 * - Subsequent providers fill gaps for missing flags
 * - If a provider fails, the next provider is tried
 * 
 * Example:
 * ```kotlin
 * val config = FlagsConfig(
 *     providers = listOf(
 *         FirebaseProvider(),  // Priority 1: Try Firebase first
 *         RestProvider(),      // Priority 2: Fallback to REST
 *         FileProvider()       // Priority 3: Local file as last resort
 *     )
 * )
 * ```
 * 
 * ## Custom Provider Implementation
 * 
 * Example implementation:
 * ```kotlin
 * class RestFlagsProvider(
 *     private val client: HttpClient,
 *     private val baseUrl: String,
 *     override val name: String = "rest"
 * ) : BaseFlagsProvider(name) {
 *     override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
 *         val response = client.get("$baseUrl/config") {
 *             if (currentRevision != null) {
 *                 header("If-None-Match", currentRevision)
 *             }
 *         }.body<ConfigResponse>()
 *         
 *         return ProviderSnapshot(
 *             revision = response.revision,
 *             fetchedAt = Clock.System.now().toEpochMilliseconds(),
 *             ttlMs = response.ttlMs,
 *             flags = response.flags.mapValues { it.value.toFlagValue() },
 *             experiments = response.experiments
 *         )
 *     }
 * }
 * ```
 * 
 * ## Health Monitoring
 * 
 * Providers should implement health checks:
 * ```kotlin
 * override fun isHealthy(): Boolean {
 *     return getConsecutiveFailures() < 5 && 
 *            getLastSuccessfulFetchMs() != null &&
 *            (Clock.System.now().toEpochMilliseconds() - getLastSuccessfulFetchMs()!!) < 3600_000
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
    
    /**
     * Check if the provider is healthy.
     * 
     * A provider is considered healthy if:
     * - It has successfully fetched a snapshot at least once
     * - The last fetch was successful
     * - The snapshot is not expired (if TTL is set)
     * 
     * @return true if provider is healthy, false otherwise
     */
    fun isHealthy(): Boolean
    
    /**
     * Get timestamp of last successful fetch.
     * 
     * @return Timestamp in milliseconds, or null if never fetched successfully
     */
    fun getLastSuccessfulFetchMs(): Long?
    
    /**
     * Get number of consecutive failures.
     * 
     * @return Number of consecutive failures, 0 if healthy
     */
    fun getConsecutiveFailures(): Int
}

