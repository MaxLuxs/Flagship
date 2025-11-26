package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.model.*

/**
 * Central manager for feature flags and experiments.
 * 
 * The FlagsManager is the main interface for:
 * - Checking if features are enabled
 * - Getting typed flag values
 * - Assigning users to experiment variants
 * - Managing local overrides for testing
 * - Refreshing configuration from providers
 * 
 * Usage example:
 * ```kotlin
 * val flags = Flagship.manager()
 * 
 * // Check boolean flag
 * if (flags.isEnabled("new_feature")) {
 *     showNewFeature()
 * }
 * 
 * // Get typed value
 * val maxAmount = flags.value("max_transfer_amount", default = 1000)
 * 
 * // Get experiment assignment
 * val variant = flags.assign("onboarding_exp")?.variant
 * ```
 */
interface FlagsManager {
    /**
     * Check if a boolean feature flag is enabled.
     * 
     * This is the most common method for feature gating. Evaluation follows this precedence:
     * 1. Local overrides (for testing)
     * 2. Provider values (in order of precedence)
     * 3. Default value
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     if (flags.isEnabled("new_feature")) {
     *         showNewFeature()
     *     }
     * }
     * ```
     * 
     * @param key The flag key to check
     * @param default Default value if flag is not found (defaults to false for safety)
     * @param ctx Optional evaluation context (uses default context if not provided)
     * @return true if the flag is enabled, false otherwise
     */
    suspend fun isEnabled(key: FlagKey, default: Boolean = false, ctx: EvalContext? = null): Boolean

    /**
     * Get a typed flag value.
     * 
     * Supports Int, Double, String, and Boolean types. The type is inferred from the default value.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val maxRetries: Int = flags.value("max_retries", default = 3)
     *     val apiUrl: String = flags.value("api_url", default = "https://api.prod.com")
     *     val threshold: Double = flags.value("threshold", default = 0.75)
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value (type is inferred from this)
     * @param ctx Optional evaluation context
     * @return The flag value, or default if not found or type mismatch
     */
    suspend fun <T> value(key: FlagKey, default: T, ctx: EvalContext? = null): T

    /**
     * Assign user to an experiment variant.
     * 
     * Uses deterministic bucketing based on user ID to ensure consistent assignment.
     * Users are only assigned if they match the experiment's targeting rules.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val assignment = flags.assign("checkout_exp")
     *     when (assignment?.variant) {
     *         "control" -> showLegacyCheckout()
     *         "treatment_a" -> showNewCheckout()
     *         "treatment_b" -> showAlternativeCheckout()
     *         else -> showLegacyCheckout() // fallback
     *     }
     * }
     * ```
     * 
     * @param key The experiment key
     * @param ctx Optional evaluation context (required for targeting and bucketing)
     * @return ExperimentAssignment with variant and payload, or null if user doesn't qualify
     */
    suspend fun assign(key: ExperimentKey, ctx: EvalContext? = null): ExperimentAssignment?

    /**
     * Synchronous version of [assign] for use when bootstrap is already complete.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * Use this in non-suspending contexts where you're certain bootstrap has finished:
     * ```kotlin
     * // After ensureBootstrap() has completed
     * val assignment = flags.assignSync("checkout_exp")
     * when (assignment?.variant) {
     *     "control" -> showLegacyCheckout()
     *     "treatment_a" -> showNewCheckout()
     *     else -> showLegacyCheckout()
     * }
     * ```
     * 
     * @param key The experiment key
     * @param ctx Optional evaluation context
     * @return Experiment assignment with variant, or null if user doesn't qualify
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun assignSync(key: ExperimentKey, ctx: EvalContext? = null): ExperimentAssignment?

    /**
     * Ensure flags are bootstrapped before first use.
     * 
     * This suspending function blocks until:
     * - All providers have completed their initial bootstrap, OR
     * - The timeout is reached
     * 
     * Call this during app initialization before rendering UI that depends on flags.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val success = flags.ensureBootstrap(timeoutMs = 3000)
     *     if (!success) {
     *         // Bootstrap timed out, using cached/default values
     *     }
     * }
     * ```
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 2000ms)
     * @return true if bootstrap completed successfully, false if timed out
     */
    suspend fun ensureBootstrap(timeoutMs: Long = 2000): Boolean


    /**
     * Add a listener for flag and snapshot updates.
     * 
     * Listeners are notified when:
     * - Configuration is refreshed from providers
     * - Local overrides are changed
     * 
     * @param listener The listener to add
     */
    fun addListener(listener: FlagsListener)

    /**
     * Remove a previously added listener.
     * 
     * @param listener The listener to remove
     */
    fun removeListener(listener: FlagsListener)

    /**
     * Set a local override for testing.
     * 
     * Overrides have highest precedence and persist until cleared or app restart.
     * Use this for:
     * - QA testing specific scenarios
     * - Developer debugging
     * - Demo mode
     * 
     * **Warning**: Never use overrides in production code for business logic.
     * 
     * @param key The flag key to override
     * @param value The override value
     */
    fun setOverride(key: FlagKey, value: FlagValue)

    /**
     * Clear a specific override.
     * 
     * @param key The flag key to clear
     */
    fun clearOverride(key: FlagKey)

    /**
     * List all currently active overrides.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val overrides = flags.listOverrides()
     *     // Use overrides
     * }
     * ```
     * 
     * @return Map of flag keys to their override values
     */
    suspend fun listOverrides(): Map<FlagKey, FlagValue>
    
    /**
     * List all available flags from all providers.
     * 
     * Returns a map of all flag keys to their current values.
     * Useful for debugging and displaying all available flags in a dashboard.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val allFlags = flags.listAllFlags()
     *     // Use allFlags
     * }
     * ```
     * 
     * @return Map of all flag keys to their values
     */
    suspend fun listAllFlags(): Map<FlagKey, FlagValue>
    
    /**
     * Synchronous version of [isEnabled] for use when bootstrap is already complete.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * Use this in non-suspending contexts where you're certain bootstrap has finished:
     * ```kotlin
     * // After ensureBootstrap() has completed
     * if (flags.isEnabledSync("new_feature")) {
     *     showNewFeature()
     * }
     * ```
     * 
     * @param key The flag key to check
     * @param default Default value if flag is not found (defaults to false for safety)
     * @param ctx Optional evaluation context (uses default context if not provided)
     * @return true if the flag is enabled, false otherwise
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun isEnabledSync(key: FlagKey, default: Boolean = false, ctx: EvalContext? = null): Boolean
    
    /**
     * Synchronous version of [value] for use when bootstrap is already complete.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * @param key The flag key
     * @param default Default value (type is inferred from this)
     * @param ctx Optional evaluation context
     * @return The flag value, or default if not found or type mismatch
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun <T> valueSync(key: FlagKey, default: T, ctx: EvalContext? = null): T
    
    /**
     * Refresh configuration from all providers.
     * 
     * @param force If true, forces refresh even if recent data is available (default: false)
     * @param onProgress Optional callback for progress updates (provider name, success/failure)
     */
    fun refresh(force: Boolean = false, onProgress: ((String, Boolean) -> Unit)? = null)
    
    /**
     * Get detailed status information for a flag.
     * 
     * Returns information about where the flag value came from, whether it exists,
     * any errors that occurred, and when it was last updated.
     * 
     * This is useful for debugging, monitoring, and understanding flag evaluation.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val status = flags.getFlagStatus("new_feature")
     *     when (status.source) {
     *         FlagSource.OVERRIDE -> log.debug("Using override")
     *         FlagSource.PROVIDER -> log.debug("Using fresh value")
     *         FlagSource.CACHE -> log.warn("Using cached value")
     *         FlagSource.DEFAULT -> log.warn("Flag not found")
     *     }
     * }
     * ```
     * 
     * @param key The flag key to get status for
     * @return FlagStatus with detailed information about the flag
     */
    suspend fun getFlagStatus(key: FlagKey): FlagStatus
    
    /**
     * Evaluate multiple flags in a single operation.
     * 
     * More efficient than calling [value] multiple times, as it:
     * - Acquires the lock only once
     * - Iterates through snapshots only once
     * - Reduces overhead for batch operations
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val defaults = mapOf(
     *         "flag1" to FlagValue.Bool(false),
     *         "flag2" to FlagValue.Int(100),
     *         "flag3" to FlagValue.StringV("default")
     *     )
     *     val results = flags.evaluateFlags(listOf("flag1", "flag2", "flag3"), defaults)
     *     // Use results
     * }
     * ```
     * 
     * @param keys List of flag keys to evaluate
     * @param defaults Map of flag keys to their default values
     * @param ctx Optional evaluation context
     * @return Map of flag keys to their evaluated values
     */
    suspend fun evaluateFlags(
        keys: List<FlagKey>,
        defaults: Map<FlagKey, FlagValue>,
        ctx: EvalContext? = null
    ): Map<FlagKey, FlagValue>
    
    /**
     * Evaluate multiple experiments in a single operation.
     * 
     * More efficient than calling [assign] multiple times, as it:
     * - Acquires the lock only once
     * - Iterates through snapshots only once
     * - Reduces overhead for batch operations
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val results = flags.evaluateExperiments(
     *         listOf("exp1", "exp2", "exp3"),
     *         ctx = context
     *     )
     *     // Use results
     * }
     * ```
     * 
     * @param keys List of experiment keys to evaluate
     * @param ctx Optional evaluation context
     * @return Map of experiment keys to their assignments (null if user doesn't qualify)
     */
    suspend fun evaluateExperiments(
        keys: List<ExperimentKey>,
        ctx: EvalContext? = null
    ): Map<ExperimentKey, ExperimentAssignment?>
    
    /**
     * Preload flags to ensure they are available in cache.
     * 
     * This method evaluates the specified flags to ensure they are loaded
     * and cached. Useful for warming up cache before user interactions.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     // Preload flags before showing UI
     *     flags.preload(listOf("new_feature", "api_timeout", "welcome_msg"))
     *     // Now flags are ready for fast access
     * }
     * ```
     * 
     * @param keys List of flag keys to preload
     */
    suspend fun preload(keys: List<FlagKey>)
    
    /**
     * Preload flags for a specific user.
     * 
     * This method evaluates flags with a user-specific context to ensure
     * they are loaded and cached for that user. Useful for warming up cache
     * before user interactions.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val userContext = EvalContext(userId = "user123", ...)
     *     flags.preloadForUser("user123", listOf("personalized_feature", "user_settings"))
     * }
     * ```
     * 
     * @param userId User ID for context
     * @param keys List of flag keys to preload
     */
    suspend fun preloadForUser(userId: String, keys: List<FlagKey>)
}

