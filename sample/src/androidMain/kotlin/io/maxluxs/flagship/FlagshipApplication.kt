package io.maxluxs.flagship

import android.app.Application
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.platform.android.AndroidFlagsInitializer
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import kotlinx.serialization.json.Json

class FlagshipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFlagship()
    }

    private fun initializeFlagship() {
        // Option 1: Use mock provider for demo (no backend needed)
        val useMockProvider = true // Set to false to use real REST backend
        
        val config = if (useMockProvider) {
            FlagsConfig(
                appKey = "sample-app",
                environment = "development",
                providers = listOf(
                    io.maxluxs.flagship.sample.MockFlagsProvider()
                ),
                cache = AndroidFlagsInitializer.createPersistentCache(this)
            )
        } else {
            // Option 2: Use real REST backend
            val httpClient = HttpClient(Android) {
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
                        baseUrl = "https://api.example.com/flags" // Replace with your server
                    )
                ),
                cache = AndroidFlagsInitializer.createPersistentCache(this)
            )
        }

        Flags.configure(config)
        
        // Set default context
        val manager = Flags.manager() as DefaultFlagsManager
        val defaultContext = AndroidFlagsInitializer.createDefaultContext(this)
        manager.setDefaultContext(defaultContext)
    }
}

