package io.maxluxs.flagship.provider.firebase

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * Factory for creating Firebase Remote Config provider instances on Android.
 * Handles Firebase initialization and configuration.
 */
object FirebaseProviderFactory {
    
    /**
     * Creates a Firebase Remote Config provider with default configuration.
     * 
     * @param application Android Application instance
     * @param defaults Default flag values to use before remote config is fetched
     * @param name Provider name (default: "firebase")
     * @return Configured FirebaseRemoteConfigProvider instance
     */
    fun create(
        application: Application,
        defaults: Map<String, Any> = emptyMap(),
        name: String = "firebase"
    ): FirebaseRemoteConfigProvider {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            
            // Configure settings
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 10 // Low interval for testing
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values if provided
            if (defaults.isNotEmpty()) {
                remoteConfig.setDefaultsAsync(defaults)
            }
            
            val adapter = AndroidFirebaseAdapter(remoteConfig)
            return FirebaseRemoteConfigProvider(adapter, name)
        } catch (e: Exception) {
            println("Firebase initialization failed: ${e.message}")
            throw IllegalStateException("Failed to initialize Firebase Remote Config", e)
        }
    }
}

