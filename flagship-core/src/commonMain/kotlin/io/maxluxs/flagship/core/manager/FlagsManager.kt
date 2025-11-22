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
 * val flags = Flags.manager()
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
     * Trigger a background refresh of all providers.
     * 
     * This is non-blocking and updates will be available after completion.
     * Listeners will be notified when refresh completes.
     * 
     * Typically called:
     * - On app foreground
     * - Periodically in the background
     * - After certain user actions
     */
    fun refresh()

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
}

