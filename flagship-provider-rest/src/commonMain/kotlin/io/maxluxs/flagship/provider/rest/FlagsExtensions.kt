package io.maxluxs.flagship.provider.rest

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.util.DefaultLogger
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

/**
 * Quick REST initialization.
 * 
 * Example:
 * ```kotlin
 * import io.maxluxs.flagship.provider.rest.initRest
 * 
 * Flagship.initRest("https://api.example.com/flags")
 * 
 * lifecycleScope.launch {
 *     if (Flagship.isEnabled("feature")) { ... }
 * }
 * ```
 * 
 * @param baseUrl Base URL for REST API
 * @param environment Environment name (default: "production")
 * @param httpClient Optional HTTP client (creates default if not provided)
 */
fun Flagship.initRest(
    baseUrl: String,
    environment: String = "production",
    httpClient: HttpClient? = null,
    metricsTracker: io.maxluxs.flagship.core.provider.ProviderMetricsTracker? = null
) {
    val client = httpClient ?: createDefaultHttpClient()
    
    val provider = RestFlagsProvider(
        client = client,
        baseUrl = baseUrl,
        metricsTracker = metricsTracker
    )
    
    val config = FlagsConfig(
        appKey = "app",
        environment = environment,
        providers = listOf(provider),
        cache = InMemoryCache(),
        logger = DefaultLogger()
    )
    
    configure(config)
}

/**
 * Creates a default HTTP client for REST provider.
 * Platform-specific implementations are provided in androidMain/iosMain.
 */
internal expect fun createDefaultHttpClient(): HttpClient

