package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext

/**
 * JVM implementation of PlatformContextInitializer.
 * 
 * For JVM, uses JvmFlagsInitializer for context creation.
 */

internal actual fun createPlatformCacheInternal(): PersistentCache {
    return JvmFlagsInitializer.createPersistentCache()
}

internal actual fun createDefaultContextInternal(): EvalContext {
    return JvmFlagsInitializer.createDefaultContext()
}

