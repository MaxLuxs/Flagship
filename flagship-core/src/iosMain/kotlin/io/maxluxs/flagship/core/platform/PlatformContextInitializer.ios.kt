package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext

/**
 * iOS implementation of PlatformContextInitializer.
 * 
 * Automatically initializes iOS-specific cache and context.
 * 
 * Usage:
 * ```kotlin
 * Flagship.configure(config)
 * PlatformContextInitializer.initialize()
 * ```
 */

internal actual fun createPlatformCacheInternal(): PersistentCache {
    return IOSFlagsInitializer.createPersistentCache()
}

internal actual fun createDefaultContextInternal(): EvalContext {
    return IOSFlagsInitializer.createDefaultContext()
}

