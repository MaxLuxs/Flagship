package io.maxluxs.flagship

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import java.util.concurrent.Future
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.provider.firebase.AndroidFirebaseAdapter
import io.maxluxs.flagship.provider.firebase.FirebaseRemoteConfigProvider
import io.maxluxs.flagship.provider.launchdarkly.AndroidLaunchDarklyAdapter
import io.maxluxs.flagship.provider.launchdarkly.LaunchDarklyProvider
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.maxluxs.flagship.sample.MockFlagsProvider
import io.maxluxs.flagship.sample.ProviderType
import kotlinx.serialization.json.Json

/**
 * Factory for creating provider instances based on selected type
 */
object ProviderFactory {
    
    // Configuration keys - replace with your actual keys
    private const val LAUNCHDARKLY_MOBILE_KEY = "mob-YOUR-KEY-HERE"
    private const val REST_API_URL = "https://api.example.com/flags"
    
    fun createProvider(type: ProviderType, application: Application): FlagsProvider {
        return when (type) {
            ProviderType.MOCK -> {
                MockFlagsProvider()
            }
            
            ProviderType.REST -> {
                val httpClient = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        })
                    }
                }
                RestFlagsProvider(
                    client = httpClient,
                    baseUrl = REST_API_URL
                )
            }
            
            ProviderType.FIREBASE -> {
                initializeFirebase(application)
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                val adapter = AndroidFirebaseAdapter(remoteConfig)
                FirebaseRemoteConfigProvider(adapter, name = "firebase")
            }
            
            ProviderType.LAUNCHDARKLY -> {
                val ldClient = initializeLaunchDarkly(application)
                val adapter = AndroidLaunchDarklyAdapter(ldClient)
                LaunchDarklyProvider(adapter, name = "launchdarkly")
            }
        }
    }
    
    private fun initializeFirebase(application: Application) {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 10 // Low interval for testing
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values
            remoteConfig.setDefaultsAsync(mapOf(
                "new_feature" to false,
                "dark_mode" to false,
                "payment_enabled" to false,
                "max_retries" to 3,
                "api_timeout" to 30.0,
                "welcome_message" to "Welcome!"
            ))
        } catch (e: Exception) {
            println("Firebase initialization failed: ${e.message}")
        }
    }
    
    private fun initializeLaunchDarkly(application: Application): LDClient {
        val config = LDConfig.Builder(LDConfig.Builder.AutoEnvAttributes.Disabled)
            .mobileKey(LAUNCHDARKLY_MOBILE_KEY)
            .build()
        
        val context = LDContext.builder("user-${System.currentTimeMillis()}")
            .kind("user")
            .name("Test User")
            .build()
        
        val future: Future<LDClient> = LDClient.init(application, config, context)
        return future.get() // Block until client is initialized
    }
}

