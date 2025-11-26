package io.maxluxs.flagship

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.JvmFlagsInitializer
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.maxluxs.flagship.sample.MockFlagsProvider
import io.maxluxs.flagship.sample.ProviderPreferences
import io.maxluxs.flagship.sample.ProviderType
import kotlinx.serialization.json.Json

object FlagshipDesktopInitializer {
    fun initialize() {
        val selectedProvider = ProviderPreferences.getSelectedProvider()
        
        val config = when (selectedProvider) {
            ProviderType.MOCK -> {
                FlagsConfig(
                    appKey = "sample-app",
                    environment = "development",
                    providers = listOf(MockFlagsProvider()),
                    cache = JvmFlagsInitializer.createPersistentCache()
                )
            }
            ProviderType.REST -> {
                val httpClient = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        })
                    }
                }
                
                FlagsConfig(
                    appKey = "sample-app",
                    environment = "development",
                    providers = listOf(
                        RestFlagsProvider(
                            client = httpClient,
                            baseUrl = "http://localhost:8080/flags" // Default REST endpoint
                        )
                    ),
                    cache = JvmFlagsInitializer.createPersistentCache()
                )
            }
            ProviderType.FIREBASE -> {
                // Firebase not available on Desktop, fallback to Mock
                FlagsConfig(
                    appKey = "sample-app",
                    environment = "development",
                    providers = listOf(MockFlagsProvider()),
                    cache = JvmFlagsInitializer.createPersistentCache()
                )
            }
            ProviderType.LAUNCHDARKLY -> {
                // LaunchDarkly not available on Desktop, fallback to Mock
                FlagsConfig(
                    appKey = "sample-app",
                    environment = "development",
                    providers = listOf(MockFlagsProvider()),
                    cache = JvmFlagsInitializer.createPersistentCache()
                )
            }
        }
        
        Flags.configure(config)
        
        // Set default context
        val manager = Flagship.manager() as DefaultFlagsManager
        val defaultContext = JvmFlagsInitializer.createDefaultContext()
        manager.setDefaultContext(defaultContext)
    }
}

