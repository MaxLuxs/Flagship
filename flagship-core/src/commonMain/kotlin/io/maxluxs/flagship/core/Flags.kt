package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.Flags.configure
import io.maxluxs.flagship.core.Flags.reset
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger

/**
 * Main entry point for the Flagship feature flags library.
 *
 * This singleton object provides access to the configured flags manager.
 *
 * Usage:
 * ```kotlin
 * // 1. Configure during app initialization
 * Flags.configure(FlagsConfig(
 *     appKey = "my-app",
 *     environment = "production",
 *     providers = listOf(restProvider),
 *     cache = persistentCache
 * ))
 *
 * // 2. Get manager instance anywhere in your app
 * val flags = Flags.manager()
 *
 * // 3. Use flags
 * if (flags.isEnabled("new_feature")) {
 *     showNewFeature()
 * }
 * ```
 */
object Flags {
    private var managerInstance: DefaultFlagsManager? = null

    /**
     * Configure the flags system with the provided configuration.
     *
     * This must be called once during app initialization before using any flags.
     * Typically called in Application.onCreate() on Android or during app startup on iOS.
     *
     * @param config The flags configuration
     * @throws IllegalStateException if already configured (call [reset] first to reconfigure)
     */
    fun configure(config: FlagsConfig) {
        managerInstance = DefaultFlagsManager(config)
    }

    /**
     * Get the configured flags manager.
     *
     * @return The flags manager instance
     * @throws IllegalStateException if [configure] has not been called yet
     */
    fun manager(): FlagsManager {
        return managerInstance ?: throw IllegalStateException(
            "Flags not configured. Call Flags.configure() first."
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
     * Flags.quickInit(
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
}

