package io.maxluxs.flagship

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.IOSFlagsInitializer
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import kotlinx.serialization.json.Json

object FlagshipIOSInitializer {
    fun initialize() {
        // Option 1: Use mock provider for demo (no backend needed)
        val useMockProvider = true // Set to false to use real REST backend
        
        val config = if (useMockProvider) {
            FlagsConfig(
                appKey = "sample-app",
                environment = "development",
                providers = listOf(
                    io.maxluxs.flagship.sample.MockFlagsProvider()
                ),
                cache = IOSFlagsInitializer.createPersistentCache()
            )
        } else {
            // Option 2: Use real REST backend
            val httpClient = HttpClient(Darwin) {
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
                cache = IOSFlagsInitializer.createPersistentCache()
            )
        }

        Flagship.configure(config)
        
        // Set default context
        val manager = Flagship.manager() as DefaultFlagsManager
        val defaultContext = IOSFlagsInitializer.createDefaultContext()
        manager.setDefaultContext(defaultContext)
    }
}

