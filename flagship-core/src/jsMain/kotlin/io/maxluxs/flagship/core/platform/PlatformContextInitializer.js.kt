package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext

/**
 * JS implementation of PlatformContextInitializer.
 * 
 * For JS, uses JsFlagsInitializer for context creation.
 */

internal actual fun createPlatformCacheInternal(): PersistentCache {
    return JsFlagsInitializer.createPersistentCache()
}

internal actual fun createDefaultContextInternal(): EvalContext {
    return JsFlagsInitializer.createDefaultContext()
}

