package io.maxluxs.flagship

import android.app.Application
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.provider.firebase.FirebaseProviderFactory
import io.maxluxs.flagship.provider.launchdarkly.LaunchDarklyProviderFactory
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.maxluxs.flagship.sample.MockFlagsProvider
import io.maxluxs.flagship.sample.ProviderType
import kotlinx.serialization.json.Json

/**
 * Factory for creating provider instances based on selected type.
 * Delegates actual initialization to provider-specific factories.
 */
object ProviderFactory {
    
    // Configuration keys - replace with your actual keys
    private const val LAUNCHDARKLY_MOBILE_KEY = "mob-YOUR-KEY-HERE"
    private const val REST_API_URL = "https://api.example.com/flags"
    
    // Default flag values for Firebase
    private val firebaseDefaults = mapOf(
        "new_feature" to false,
        "dark_mode" to false,
        "payment_enabled" to false,
        "max_retries" to 3,
        "api_timeout" to 30.0,
        "welcome_message" to "Welcome!"
    )
    
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
                FirebaseProviderFactory.create(
                    application = application,
                    defaults = firebaseDefaults,
                    name = "firebase"
                )
            }
            
            ProviderType.LAUNCHDARKLY -> {
                LaunchDarklyProviderFactory.create(
                    application = application,
                    mobileKey = LAUNCHDARKLY_MOBILE_KEY,
                    userName = "Test User",
                    name = "launchdarkly"
                )
            }
        }
    }
}

