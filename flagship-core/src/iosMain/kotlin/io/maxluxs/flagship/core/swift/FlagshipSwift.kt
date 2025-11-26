package io.maxluxs.flagship.core.swift

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.platform.IOSFlagsInitializer
import io.maxluxs.flagship.core.platform.PlatformContextInitializer
import io.maxluxs.flagship.core.manager.DefaultFlagsManager

/**
 * Swift-friendly API wrapper for Flagship.
 * 
 * This provides a more convenient API for Swift developers with:
 * - Simplified initialization
 * - Better error handling
 * - Swift-style naming conventions
 * 
 * Usage in Swift:
 * ```swift
 * // Initialize
 * FlagshipSwift.shared.configure(
 *     appKey: "my-app",
 *     environment: "production",
 *     providers: [...]
 * )
 * 
 * // Use flags
 * let flags = FlagshipSwift.shared.manager
 * Task {
 *     let enabled = await flags.isEnabled(key: "new_feature", default: false)
 *     if enabled {
 *         showNewFeature()
 *     }
 * }
 * ```
 */
object FlagshipSwift {
    /**
     * Shared singleton instance for easy access from Swift.
     */
    val shared: FlagshipSwift
        get() = this
    
    private var isConfigured: Boolean = false
    
    /**
     * Configure Flagship with iOS-specific defaults.
     * 
     * This automatically:
     * - Creates iOS-specific PersistentCache
     * - Sets up default EvalContext with device info
     * - Configures Flags with the provided config
     * 
     * @param config FlagsConfig to use
     * @param autoInitializeContext If true, automatically initializes platform context (default: true)
     */
    fun configure(
        config: FlagsConfig,
        autoInitializeContext: Boolean = true
    ) {
        if (isConfigured) {
            throw IllegalStateException("FlagshipSwift already configured. Call reset() first to reconfigure.")
        }
        
        Flagship.configure(config)
        
        if (autoInitializeContext) {
            try {
                val manager = Flagship.manager() as DefaultFlagsManager
                PlatformContextInitializer.initialize(manager)
            } catch (e: Exception) {
                // Context initialization is optional, log but don't fail
                config.logger.warn("FlagshipSwift", "Failed to auto-initialize context: ${e.message}")
            }
        }
        
        isConfigured = true
    }
    
    /**
     * Quick configuration helper for common use cases.
     * 
     * @param appKey Application key
     * @param environment Environment name
     * @param providers List of providers
     * @param autoInitializeContext If true, automatically initializes platform context (default: true)
     */
    fun quickConfigure(
        appKey: String,
        environment: String,
        providers: List<io.maxluxs.flagship.core.provider.FlagsProvider>,
        autoInitializeContext: Boolean = true
    ) {
        val cache = IOSFlagsInitializer.createPersistentCache()
        val config = FlagsConfig(
            appKey = appKey,
            environment = environment,
            providers = providers,
            cache = cache
        )
        configure(config, autoInitializeContext)
    }
    
    /**
     * Get the FlagsManager instance.
     * 
     * @return FlagsManager instance
     * @throws IllegalStateException if not configured
     */
    val manager: FlagsManager
        get() {
            if (!isConfigured) {
                throw IllegalStateException("FlagshipSwift not configured. Call configure() first.")
            }
            return Flagship.manager()
        }
    
    /**
     * Check if Flagship is configured.
     */
    fun isConfigured(): Boolean = isConfigured
    
    /**
     * Reset configuration (useful for testing).
     */
    fun reset() {
        Flagship.reset()
        isConfigured = false
    }
}

/**
 * Swift-friendly extension methods for FlagsManager.
 * 
 * These provide convenience methods that work seamlessly with Swift async/await.
 * Kotlin suspend functions are automatically converted to Swift async functions.
 * 
 * Usage in Swift:
 * ```swift
 * let flags = FlagshipSwift.shared.manager
 * Task {
 *     let enabled = await flags.isEnabled(key: "new_feature", default: false)
 *     let value: String = await flags.value(key: "api_url", default: "https://api.example.com")
 *     let assignment = await flags.assign(key: "checkout_exp", context: nil)
 * }
 * ```
 */

