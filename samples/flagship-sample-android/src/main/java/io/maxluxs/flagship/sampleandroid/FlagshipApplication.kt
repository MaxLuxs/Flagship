package io.maxluxs.flagship.sampleandroid

import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class FlagshipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFlagship()
    }

    private fun initializeFlagship() {
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val provider = RestFlagsProvider(
            client = httpClient,
            baseUrl = "http://10.0.2.2:8080/api/flags" // Android emulator localhost
        )
        
        val config = FlagsConfig(
            appKey = "sample-android-app",
            environment = "production",
            providers = listOf(provider),
            cache = AndroidFlagsInitializer.createPersistentCache(this)
        )

        Flagship.configure(config)
        
        val manager = Flagship.manager() as DefaultFlagsManager
        val defaultContext = AndroidFlagsInitializer.createDefaultContext(this)
        manager.setDefaultContext(defaultContext)
    }
}

