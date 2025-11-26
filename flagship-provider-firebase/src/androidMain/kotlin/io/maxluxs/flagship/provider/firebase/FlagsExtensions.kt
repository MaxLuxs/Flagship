package io.maxluxs.flagship.provider.firebase

import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer

/**
 * Quick Firebase initialization for Android.
 * 
 * This is the simplest way to get started - just one line!
 * 
 * Example:
 * ```kotlin
 * // In Application.onCreate()
 * import io.maxluxs.flagship.provider.firebase.initFirebase
 * 
 * Flagship.initFirebase(application)
 * 
 * // Then use anywhere:
 * lifecycleScope.launch {
 *     if (Flagship.isEnabled("new_feature")) {
 *         showNewFeature()
 *     }
 * }
 * ```
 * 
 * @param application Android Application instance
 * @param defaults Optional default values map
 * @param environment Environment name (default: "production")
 */
fun Flagship.initFirebase(
    application: Application,
    defaults: Map<String, Any> = emptyMap(),
    environment: String = "production"
) {
    val provider = FirebaseProviderFactory.create(
        application = application,
        defaults = defaults
    )
    
    val config = FlagsConfig(
        appKey = application.packageName,
        environment = environment,
        providers = listOf(provider),
        cache = AndroidFlagsInitializer.createPersistentCache(application),
        logger = DefaultLogger()
    )
    
    configure(config)
    
    // Set default context
    val manager = manager() as DefaultFlagsManager
    val defaultContext = AndroidFlagsInitializer.createDefaultContext(application)
    manager.setDefaultContext(defaultContext)
}

