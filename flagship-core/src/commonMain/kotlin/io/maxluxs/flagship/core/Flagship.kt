package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.config.ConfigValidator
import io.maxluxs.flagship.core.config.FlagsConfigBuilder
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagStatus
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.FlagsLogger
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Unified API for Flagship feature flags and experiments.
 * 
 * This is the main entry point for the Flagship library. It provides a simple,
 * intuitive API for working with feature flags and experiments.
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * // 1. Configure during app initialization
 * Flagship.configure(FlagsConfig(
 *     appKey = "my-app",
 *     environment = "production",
 *     providers = listOf(restProvider),
 *     cache = persistentCache
 * ))
 * 
 * // 2. Use flags anywhere in your app
 * lifecycleScope.launch {
 *     val enabled = Flagship.isEnabled("new_ui")
 *     if (enabled) {
 *         showNewUI()
 *     }
 *     
 *     val timeout: Int = Flagship.get("api_timeout", default = 30)
 *     val message: String = Flagship.get("welcome_msg", default = "Hello")
 * }
 * 
 * // 3. Use experiments
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
 * ## Advanced Usage
 * 
 * ```kotlin
 * // Get manager for advanced operations
 * val manager = Flagship.manager()
 * manager.refresh()
 * manager.setOverride("new_ui", FlagValue.Bool(true))
 * ```
 */
object Flagship {
    private var managerInstance: DefaultFlagsManager? = null

    /**
     * Configuration builder for Flagship initialization (legacy API).
     * 
     * @deprecated Use [configure] with [FlagsConfig] instead
     */
    @Deprecated("Use configure(FlagsConfig) instead", ReplaceWith("Flagship.configure(FlagsConfig(...))"))
    data class FlagshipConfig(
        val providers: List<FlagsProvider>,
        val cache: FlagsCache = InMemoryCache(),
        val environment: String = "production",
        val logger: FlagsLogger = DefaultLogger()
    )
    
    /**
     * Configure the flags system with the provided configuration.
     *
     * This must be called once during app initialization before using any flags.
     * Typically called in Application.onCreate() on Android or during app startup on iOS.
     *
     * @param config The flags configuration
     * @param validate Whether to validate configuration (default: true)
     * @throws IllegalStateException if already configured (call [reset] first to reconfigure)
     * @throws io.maxluxs.flagship.core.errors.ConfigurationException if validation fails
     */
    fun configure(config: FlagsConfig, validate: Boolean = true) {
        if (validate) {
            ConfigValidator.validateOrThrow(config)
            val warnings = ConfigValidator.getWarnings(config)
            if (warnings.isNotEmpty()) {
                config.logger.warn("Flagship", "Configuration warnings:\n" + warnings.joinToString("\n"))
            }
        }
        managerInstance = DefaultFlagsManager(config)
    }
    
    /**
     * Configure the flags system using DSL builder.
     * 
     * This provides a more intuitive way to configure Flagship with a fluent API.
     * 
     * Example:
     * ```kotlin
     * Flagship.configure {
     *     appKey = "my-app"
     *     environment = "production"
     *     
     *     providers {
     *         + FirebaseProviderFactory.create(application)
     *         + RestFlagsProvider(httpClient, "https://api.example.com")
     *     }
     *     
     *     cache = PersistentCache(platformContext)
     *     logger = DefaultLogger()
     * }
     * ```
     * 
     * @param block DSL block for configuring Flagship
     * @param validate Whether to validate configuration (default: true)
     * @throws IllegalStateException if already configured (call [reset] first to reconfigure)
     * @throws io.maxluxs.flagship.core.errors.ConfigurationException if validation fails
     * @throws IllegalArgumentException if appKey is not specified in builder
     */
    fun configure(validate: Boolean = true, block: FlagsConfigBuilder.() -> Unit) {
        val builder = FlagsConfigBuilder()
        builder.block()
        configure(builder.build(), validate)
    }

    /**
     * Get the configured flags manager.
     *
     * @return The flags manager instance
     * @throws IllegalStateException if [configure] has not been called yet
     */
    fun manager(): FlagsManager {
        return managerInstance ?: throw IllegalStateException(
            "Flagship not configured. Call Flagship.configure() first."
        )
    }

    /**
     * Check if flags system has been configured.
     *
     * @return true if configured, false otherwise
     */
    fun isConfigured(): Boolean = managerInstance != null

    /**
     * Reset the flags configuration.
     *
     * This is primarily useful for testing. In production, configure once and use throughout app lifecycle.
     *
     * **Warning**: This clears all state including overrides and cached data.
     */
    fun reset() {
        managerInstance = null
    }

    internal fun managerOrNull(): DefaultFlagsManager? = managerInstance

    /**
     * Quick initialization helper for common use cases.
     *
     * This creates a minimal configuration with sensible defaults.
     * Perfect for simple remote config scenarios.
     *
     * Example:
     * ```kotlin
     * Flagship.quickInit(
     *     appKey = "my-app",
     *     providers = listOf(FirebaseProviderFactory.create(application))
     * )
     * ```
     */
    fun quickInit(
        appKey: String,
        providers: List<FlagsProvider>,
        environment: String = "production"
    ) {
        configure(
            FlagsConfig(
                appKey = appKey,
                environment = environment,
                providers = providers,
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
    }
    
    /**
     * Initialize Flagship with API key and configuration (legacy API).
     * 
     * @deprecated Use [configure] with [FlagsConfig] instead
     */
    @Deprecated("Use configure(FlagsConfig) instead", ReplaceWith("Flagship.configure(FlagsConfig(...))"))
    fun init(apiKey: String, config: FlagshipConfig) {
        configure(
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
        return manager().value(key, default)
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
        return manager().isEnabled(key, default)
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
        return manager().assign(key)
    }
    
    /**
     * Assign experiment variant with optional context.
     * 
     * @param key The experiment key
     * @param context Optional evaluation context
     * @return Experiment assignment with variant, or null if user doesn't qualify
     */
    suspend fun assign(key: ExperimentKey, context: EvalContext? = null): ExperimentAssignment? {
        return manager().assign(key, context)
    }
    
    /**
     * Refresh flags from all providers.
     * 
     * This triggers a refresh of all configured providers.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     Flagship.refresh()
     * }
     * ```
     */
    fun refresh() {
        manager().refresh()
    }
    
    /**
     * Set override for a flag (debug only).
     * 
     * This allows overriding flag values for testing and debugging.
     * 
     * @param key Flag key
     * @param value Override value
     */
    fun setOverride(key: FlagKey, value: FlagValue) {
        manager().setOverride(key, value)
    }
    
    /**
     * Clear override for a flag.
     * 
     * @param key Flag key
     */
    fun clearOverride(key: FlagKey) {
        manager().clearOverride(key)
    }
    
    /**
     * Check if Flagship has been initialized (legacy method name).
     * 
     * @deprecated Use [isConfigured] instead
     */
    @Deprecated("Use isConfigured() instead", ReplaceWith("Flagship.isConfigured()"))
    fun isInitialized(): Boolean {
        return isConfigured()
    }
    
    /**
     * Synchronous version of [isEnabled] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * Use this in non-suspending contexts where you're certain bootstrap has finished:
     * ```kotlin
     * // After ensureBootstrap() has completed
     * if (Flagship.isEnabledSync("new_ui")) {
     *     showNewUI()
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag not found (default: false)
     * @return true if flag is enabled
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun isEnabledSync(key: FlagKey, default: Boolean = false): Boolean {
        return manager().isEnabledSync(key, default)
    }
    
    /**
     * Synchronous version of [get] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * Use this in non-suspending contexts where you're certain bootstrap has finished:
     * ```kotlin
     * // After ensureBootstrap() has completed
     * val timeout: Int = Flagship.getSync("api_timeout", default = 30)
     * val message: String = Flagship.getSync("welcome_msg", default = "Hello")
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found
     * @return The flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun <T> getSync(key: FlagKey, default: T): T {
        return manager().valueSync(key, default)
    }
    
    /**
     * Synchronous version of [value] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * This is an alias for [getSync] that matches the [FlagsManager] API.
     * 
     * @param key The flag key
     * @param default Default value (type is inferred from this)
     * @return The flag value, or default if not found or type mismatch
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun <T> valueSync(key: FlagKey, default: T): T {
        return manager().valueSync(key, default)
    }
    
    /**
     * Synchronous version of [assign] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * It does not suspend and returns immediately using cached values.
     * 
     * Use this in non-suspending contexts where you're certain bootstrap has finished:
     * ```kotlin
     * // After ensureBootstrap() has completed
     * val assignment = Flagship.assignSync("checkout_flow")
     * when (assignment?.variant) {
     *     "control" -> showLegacyCheckout()
     *     "B" -> showNewCheckout()
     *     else -> showLegacyCheckout()
     * }
     * ```
     * 
     * @param key The experiment key
     * @param context Optional evaluation context
     * @return Experiment assignment with variant, or null if user doesn't qualify
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun assignSync(key: ExperimentKey, context: EvalContext? = null): ExperimentAssignment? {
        return manager().assignSync(key, context)
    }
    
    /**
     * Get flag value with explicit error handling.
     * 
     * Returns [Result] that can be used to handle errors explicitly.
     * This is useful when you need to distinguish between different error conditions
     * or want to handle errors in a specific way.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val result = Flagship.valueOrError("api_timeout", default = 100)
     *     result.onSuccess { value -> 
     *         println("Value: $value") 
     *     }.onFailure { error -> 
     *         log.error("Failed to get flag", error) 
     *     }
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found
     * @return Result containing the flag value or an error
     */
    suspend fun <T> valueOrError(key: FlagKey, default: T): Result<T> {
        return runCatching {
            manager().value(key, default)
        }
    }
    
    /**
     * Check if flag is enabled with explicit error handling.
     * 
     * Returns [Result] that can be used to handle errors explicitly.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val result = Flagship.isEnabledOrError("new_ui", default = false)
     *     result.onSuccess { enabled -> 
     *         if (enabled) showNewUI() 
     *     }.onFailure { error -> 
     *         log.error("Failed to check flag", error) 
     *     }
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag not found (default: false)
     * @return Result containing true if flag is enabled, or an error
     */
    suspend fun isEnabledOrError(key: FlagKey, default: Boolean = false): Result<Boolean> {
        return runCatching {
            manager().isEnabled(key, default)
        }
    }
    
    /**
     * Assign experiment variant with explicit error handling.
     * 
     * Returns [Result] that can be used to handle errors explicitly.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val result = Flagship.assignOrError("checkout_flow")
     *     result.onSuccess { assignment -> 
     *         when (assignment?.variant) {
     *             "control" -> showLegacyCheckout()
     *             "B" -> showNewCheckout()
     *             else -> showLegacyCheckout()
     *         }
     *     }.onFailure { error -> 
     *         log.error("Failed to assign experiment", error) 
     *     }
     * }
     * ```
     * 
     * @param key The experiment key
     * @param context Optional evaluation context
     * @return Result containing experiment assignment or an error
     */
    suspend fun assignOrError(
        key: ExperimentKey, 
        context: EvalContext? = null
    ): Result<ExperimentAssignment?> {
        return runCatching {
            manager().assign(key, context)
        }
    }
    
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
     *     val status = Flagship.getFlagStatus("new_feature")
     *     when (status.source) {
     *         FlagSource.OVERRIDE -> log.debug("Using override")
     *         FlagSource.PROVIDER -> log.debug("Using fresh value")
     *         FlagSource.CACHE -> log.warn("Using cached value")
     *         FlagSource.DEFAULT -> log.warn("Flag not found")
     *     }
     *     
     *     if (status.lastError != null) {
     *         log.error("Error getting flag", status.lastError)
     *     }
     * }
     * ```
     * 
     * @param key The flag key to get status for
     * @return FlagStatus with detailed information about the flag
     */
    suspend fun getFlagStatus(key: FlagKey): FlagStatus {
        return manager().getFlagStatus(key)
    }
    
    /**
     * Get a boolean flag value with explicit type.
     * 
     * This is a type-safe alternative to [value] for boolean flags.
     * The type is explicit, making the code more readable and less error-prone.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val enabled = Flagship.boolValue("new_feature", default = false)
     *     if (enabled) {
     *         showNewFeature()
     *     }
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: false)
     * @return The boolean flag value or default
     */
    suspend fun boolValue(key: FlagKey, default: Boolean = false): Boolean {
        return manager().value(key, default)
    }
    
    /**
     * Synchronous version of [boolValue] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: false)
     * @return The boolean flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun boolValueSync(key: FlagKey, default: Boolean = false): Boolean {
        return manager().valueSync(key, default)
    }
    
    /**
     * Get an integer flag value with explicit type.
     * 
     * This is a type-safe alternative to [value] for integer flags.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val timeout = Flagship.intValue("api_timeout", default = 30)
     *     val maxRetries = Flagship.intValue("max_retries", default = 3)
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: 0)
     * @return The integer flag value or default
     */
    suspend fun intValue(key: FlagKey, default: Int = 0): Int {
        return manager().value(key, default)
    }
    
    /**
     * Synchronous version of [intValue] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: 0)
     * @return The integer flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun intValueSync(key: FlagKey, default: Int = 0): Int {
        return manager().valueSync(key, default)
    }
    
    /**
     * Get a double flag value with explicit type.
     * 
     * This is a type-safe alternative to [value] for double flags.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val threshold = Flagship.doubleValue("confidence_threshold", default = 0.75)
     *     val discount = Flagship.doubleValue("discount_rate", default = 0.1)
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: 0.0)
     * @return The double flag value or default
     */
    suspend fun doubleValue(key: FlagKey, default: Double = 0.0): Double {
        return manager().value(key, default)
    }
    
    /**
     * Synchronous version of [doubleValue] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: 0.0)
     * @return The double flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun doubleValueSync(key: FlagKey, default: Double = 0.0): Double {
        return manager().valueSync(key, default)
    }
    
    /**
     * Get a string flag value with explicit type.
     * 
     * This is a type-safe alternative to [value] for string flags.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val message = Flagship.stringValue("welcome_msg", default = "Hello")
     *     val apiUrl = Flagship.stringValue("api_url", default = "https://api.example.com")
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: "")
     * @return The string flag value or default
     */
    suspend fun stringValue(key: FlagKey, default: String = ""): String {
        return manager().value(key, default)
    }
    
    /**
     * Synchronous version of [stringValue] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: "")
     * @return The string flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun stringValueSync(key: FlagKey, default: String = ""): String {
        return manager().valueSync(key, default)
    }
    
    /**
     * Get a JSON flag value with explicit type.
     * 
     * This is a type-safe alternative to [value] for JSON flags.
     * Useful for complex structured data stored as JSON.
     * 
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val config = Flagship.jsonValue("feature_config", default = JsonNull)
     *     if (config is JsonObject) {
     *         val enabled = config["enabled"]?.jsonPrimitive?.booleanOrNull
     *     }
     * }
     * ```
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: JsonNull)
     * @return The JSON element flag value or default
     */
    suspend fun jsonValue(key: FlagKey, default: JsonElement = JsonNull): JsonElement {
        return manager().value(key, default)
    }
    
    /**
     * Synchronous version of [jsonValue] for use after bootstrap.
     * 
     * **Important**: This method only works after bootstrap has completed.
     * 
     * @param key The flag key
     * @param default Default value if flag is not found (default: JsonNull)
     * @return The JSON element flag value or default
     * @throws IllegalStateException if bootstrap has not completed yet
     */
    fun jsonValueSync(key: FlagKey, default: JsonElement = JsonNull): JsonElement {
        return manager().valueSync(key, default)
    }
    
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
     *     val results = Flagship.evaluateFlags(listOf("flag1", "flag2", "flag3"), defaults)
     *     // Use results
     * }
     * ```
     * 
     * @param keys List of flag keys to evaluate
     * @param defaults Map of flag keys to their default values
     * @return Map of flag keys to their evaluated values
     */
    suspend fun evaluateFlags(
        keys: List<FlagKey>,
        defaults: Map<FlagKey, FlagValue>
    ): Map<FlagKey, FlagValue> {
        return manager().evaluateFlags(keys, defaults)
    }
    
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
     *     val results = Flagship.evaluateExperiments(
     *         listOf("exp1", "exp2", "exp3"),
     *         context = context
     *     )
     *     // Use results
     * }
     * ```
     * 
     * @param keys List of experiment keys to evaluate
     * @param context Optional evaluation context
     * @return Map of experiment keys to their assignments (null if user doesn't qualify)
     */
    suspend fun evaluateExperiments(
        keys: List<ExperimentKey>,
        context: EvalContext? = null
    ): Map<ExperimentKey, ExperimentAssignment?> {
        return manager().evaluateExperiments(keys, context)
    }
    
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
     *     Flagship.preload(listOf("new_feature", "api_timeout", "welcome_msg"))
     *     // Now flags are ready for fast access
     * }
     * ```
     * 
     * @param keys List of flag keys to preload
     */
    suspend fun preload(keys: List<FlagKey>) {
        manager().preload(keys)
    }
    
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
     *     Flagship.preloadForUser("user123", listOf("personalized_feature", "user_settings"))
     * }
     * ```
     * 
     * @param userId User ID for context
     * @param keys List of flag keys to preload
     */
    suspend fun preloadForUser(userId: String, keys: List<FlagKey>) {
        manager().preloadForUser(userId, keys)
    }
}

