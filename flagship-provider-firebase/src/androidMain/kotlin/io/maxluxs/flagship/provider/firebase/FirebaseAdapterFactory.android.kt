package io.maxluxs.flagship.provider.firebase

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

actual object FirebaseAdapterFactory {
    actual fun create(): FirebaseRemoteConfigAdapter {
        throw UnsupportedOperationException(
            "FirebaseAdapterFactory.create() without Application is not supported on Android. " +
            "Use FirebaseProviderFactory.create(application) instead."
        )
    }
    
    /**
     * Android-specific method to create adapter with Application context.
     */
    fun create(application: Application): FirebaseRemoteConfigAdapter {
        if (FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.initializeApp(application)
        }
        
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        return AndroidFirebaseAdapter(remoteConfig)
    }
}

