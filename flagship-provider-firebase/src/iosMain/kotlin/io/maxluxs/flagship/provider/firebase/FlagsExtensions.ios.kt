package io.maxluxs.flagship.provider.firebase

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.IOSFlagsInitializer
import io.maxluxs.flagship.core.util.DefaultLogger
import platform.Foundation.NSBundle

/**
 * Quick Firebase initialization for iOS.
 * 
 * This is the simplest way to get started - just one line!
 * 
 * Example:
 * ```kotlin
 * // In AppDelegate or App.swift
 * import io.maxluxs.flagship.provider.firebase.initFirebase
 * 
 * // First, initialize Firebase (usually done in AppDelegate)
 * // FirebaseApp.configure() // Call this first if not already done
 * 
 * Flags.initFirebase(defaults = mapOf("feature" to false))
 * 
 * // Then use anywhere:
 * if (Flags.isEnabled("new_feature")) {
 *     showNewFeature()
 * }
 * ```
 * 
 * @param defaults Optional default values map
 * @param environment Environment name (default: "production")
 * @param remoteConfig Optional FIRRemoteConfig instance (creates default if not provided)
 */
fun Flags.initFirebase(
    defaults: Map<String, Any> = emptyMap(),
    environment: String = "production",
    remoteConfig: FIRRemoteConfig? = null
) {
    val config = remoteConfig ?: FIRRemoteConfig.remoteConfig()
    
    // Set default values if provided
    if (defaults.isNotEmpty()) {
        val defaultsDict = platform.Foundation.NSMutableDictionary()
        defaults.forEach { (key, value) ->
            defaultsDict.setObject(value, forKey = key)
        }
        config.setDefaults(defaultsDict)
    }
    
    val adapter = IOSFirebaseAdapter(config)
    val provider = FirebaseRemoteConfigProvider(adapter, "firebase")
    
    val appKey = NSBundle.mainBundle.bundleIdentifier ?: "ios-app"
    
    val flagsConfig = FlagsConfig(
        appKey = appKey,
        environment = environment,
        providers = listOf(provider),
        cache = IOSFlagsInitializer.createPersistentCache(),
        logger = DefaultLogger()
    )
    
    configure(flagsConfig)
    
    // Set default context
    val manager = manager() as DefaultFlagsManager
    val defaultContext = IOSFlagsInitializer.createDefaultContext()
    manager.setDefaultContext(defaultContext)
}

