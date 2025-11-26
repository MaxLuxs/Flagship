package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.manager.DefaultFlagsManager

/**
 * Platform-specific context initializer for automatic setup.
 * 
 * This utility automatically detects the platform and initializes:
 * - Platform-specific PersistentCache
 * - Default EvalContext with device/app information
 * - Sets default context in FlagsManager
 * 
 * Usage:
 * ```kotlin
 * // Configure Flagship
 * Flagship.configure(config)
 * 
 * // Automatically initialize platform context
 * PlatformContextInitializer.initialize()
 * ```
 * 
 * This will automatically:
 * - Create platform-specific cache (if not already set)
 * - Create default context with device info
 * - Set default context in FlagsManager
 */
object PlatformContextInitializer {
    /**
     * Initialize platform-specific context automatically.
     * 
     * This method:
     * 1. Creates platform-specific PersistentCache (if needed)
     * 2. Creates default EvalContext with device/app info
     * 3. Sets default context in FlagsManager
     * 
     * @param manager Optional FlagsManager instance (uses Flagship.manager() if not provided)
     * @return The created EvalContext
     */
    fun initialize(manager: DefaultFlagsManager? = null): EvalContext {
        val flagsManager = manager ?: try {
            io.maxluxs.flagship.core.Flagship.manager() as DefaultFlagsManager
        } catch (e: IllegalStateException) {
            throw IllegalStateException(
                "Flagship not configured. Call Flagship.configure() before PlatformContextInitializer.initialize()",
                e
            )
        }
        
        val defaultContext = createDefaultContext()
        flagsManager.setDefaultContext(defaultContext)
        
        return defaultContext
    }
    
    /**
     * Create platform-specific PersistentCache.
     * 
     * This automatically detects the platform and creates appropriate cache.
     * 
     * @return Platform-specific PersistentCache
     */
    fun createPlatformCache(): PersistentCache {
        return createPlatformCacheInternal()
    }
    
    /**
     * Create default EvalContext with platform-specific information.
     * 
     * Automatically populates:
     * - deviceId (platform-specific)
     * - appVersion (from app bundle/package)
     * - osName and osVersion
     * - locale and region
     * 
     * @return EvalContext with platform-specific defaults
     */
    fun createDefaultContext(): EvalContext {
        return createDefaultContextInternal()
    }
}

// Internal expect/actual functions
internal expect fun createPlatformCacheInternal(): PersistentCache
internal expect fun createDefaultContextInternal(): EvalContext

