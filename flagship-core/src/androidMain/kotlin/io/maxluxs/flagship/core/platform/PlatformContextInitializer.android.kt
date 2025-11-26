package io.maxluxs.flagship.core.platform

import android.content.Context
import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext

/**
 * Android implementation of PlatformContextInitializer.
 * 
 * Note: For Android, you need to provide Context. Use AndroidFlagsInitializer directly
 * or call PlatformContextInitializer.setContext() before initialize().
 * 
 * For automatic initialization with Context, use:
 * ```kotlin
 * val context: Context = ... // from Application
 * PlatformContextInitializer.setContext(context)
 * val cache = PlatformContextInitializer.createPlatformCache()
 * val config = FlagsConfig(..., cache = cache)
 * Flagship.configure(config)
 * PlatformContextInitializer.initialize()
 * ```
 */
private var androidContext: Context? = null

/**
 * Set Android Context for automatic initialization.
 * 
 * This must be called before initialize() on Android platform.
 * 
 * @param context Android Context (usually Application instance)
 */
fun PlatformContextInitializer.setContext(context: Context) {
    androidContext = context.applicationContext
}

internal fun getAndroidContext(): Context? = androidContext

internal actual fun createPlatformCacheInternal(): PersistentCache {
    val context = getAndroidContext()
        ?: throw IllegalStateException(
            "Android Context not set. Call PlatformContextInitializer.setContext(context) first, " +
            "or use AndroidFlagsInitializer.createPersistentCache(context) directly."
        )
    return AndroidFlagsInitializer.createPersistentCache(context)
}

internal actual fun createDefaultContextInternal(): EvalContext {
    val context = getAndroidContext()
        ?: throw IllegalStateException(
            "Android Context not set. Call PlatformContextInitializer.setContext(context) first, " +
            "or use AndroidFlagsInitializer.createDefaultContext(context) directly."
        )
    return AndroidFlagsInitializer.createDefaultContext(context)
}

