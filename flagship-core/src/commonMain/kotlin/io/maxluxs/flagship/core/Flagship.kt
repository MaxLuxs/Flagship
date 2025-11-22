package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.FlagsLogger

/**
 * Unified API for Flagship feature flags and experiments.
 * 
 * This is the recommended entry point for new code. It provides a simpler,
 * more intuitive API compared to the lower-level Flags API.
 * 
 * Usage:
 * ```kotlin
 * // 1. Initialize once during app startup
 * Flagship.init(
 *     apiKey = "your-api-key",
 *     config = FlagshipConfig(
 *         providers = listOf(restProvider),
 *         cache = persistentCache
 *     )
 * )
 * 
 * // 2. Use flags anywhere in your app
 * val flag = Flagship.get("new_ui", default = false)
 * if (flag) {
 *     showNewUI()
 * }
 * 
 * // 3. Use experiments
 * val variant = Flagship.experiment("checkout_flow").variant
 * when (variant) {
 *     "control" -> showLegacyCheckout()
 *     "B" -> showNewCheckout()
 * }
 * ```
 */
object Flagship {
    /**
     * Configuration builder for Flagship initialization.
     */
    data class FlagshipConfig(
        val providers: List<FlagsProvider>,
        val cache: FlagsCache = InMemoryCache(),
        val environment: String = "production",
        val logger: FlagsLogger = DefaultLogger()
    )
    
    /**
     * Initialize Flagship with API key and configuration.
     * 
     * This must be called once during app initialization before using any flags.
     * 
     * @param apiKey Your Flagship API key (used for authentication and identification)
     * @param config Configuration including providers, cache, etc.
     * @throws IllegalStateException if already initialized (call [reset] first to reinitialize)
     */
    fun init(apiKey: String, config: FlagshipConfig) {
        Flags.configure(
            FlagsConfig(
                appKey = apiKey,
                environment = config.environment,
                providers = config.providers,
                cache = config.cache,
                logger = config.logger
            )
        )
    }
    
    /**
     * Get a flag value with a default.
     * 
     * This is the simplest way to access feature flags. The type is inferred from the default value.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val enabled: Boolean = Flagship.get("new_ui", default = false)
     *     val timeout: Int = Flagship.get("api_timeout", default = 30)
     *     val message: String = Flagship.get("welcome_msg", default = "Hello")
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found
     * @return The flag value or default
     */
    suspend fun <T> get(key: FlagKey, default: T): T {
        return Flags.manager().value(key, default)
    }
    
    /**
     * Check if a boolean flag is enabled.
     * 
     * Convenience method for boolean flags.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     if (Flagship.isEnabled("new_ui")) {
     *         showNewUI()
     *     }
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag not found (default: false)
     * @return true if flag is enabled
     */
    suspend fun isEnabled(key: FlagKey, default: Boolean = false): Boolean {
        return Flags.manager().isEnabled(key, default)
    }
    
    /**
     * Get experiment assignment result.
     * 
     * Returns an object with the variant and other experiment data.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val experiment = Flagship.experiment("checkout_flow")
     *     when (experiment?.variant) {
     *         "control" -> showLegacyCheckout()
     *         "B" -> showNewCheckout()
     *         else -> showLegacyCheckout() // fallback
     *     }
     * }
     * ```
     * 
     * @param key The experiment key
     * @return Experiment assignment with variant, or null if user doesn't qualify
     */
    suspend fun experiment(key: ExperimentKey): ExperimentAssignment? {
        return Flags.manager().assign(key)
    }
    
    /**
     * Get the underlying FlagsManager for advanced operations.
     * 
     * Use this if you need access to methods like refresh(), setOverride(), etc.
     * 
     * @return The FlagsManager instance
     */
    fun manager(): FlagsManager {
        return Flags.manager()
    }
    
    /**
     * Check if Flagship has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return Flags.isConfigured()
    }
    
    /**
     * Reset Flagship configuration.
     * 
     * This is primarily useful for testing. In production, initialize once and use throughout app lifecycle.
     * 
     * **Warning**: This clears all state including overrides and cached data.
     */
    fun reset() {
        Flags.reset()
    }
}

