package io.maxluxs.flagship.core.config

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.analytics.FlagsAnalytics
import io.maxluxs.flagship.core.analytics.NoopAnalytics
import io.maxluxs.flagship.core.analytics.ProviderAnalyticsConfig
import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.performance.NoopPerformanceMonitor
import io.maxluxs.flagship.core.performance.PerformanceMonitor
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.retry.NoRetryPolicy
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.util.*

/**
 * DSL builder for creating [FlagsConfig] with a fluent, type-safe API.
 * 
 * This builder provides a more intuitive way to configure Flagship compared
 * to constructing [FlagsConfig] directly.
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * Flagship.configure {
 *     appKey = "my-app"
 *     environment = "production"
 *     
 *     providers {
 *         + FirebaseProviderFactory.create(application)
 *         + RestFlagsProvider(httpClient, "https://api.example.com")
 *     }
 *     
 *     cache = PersistentCache(platformContext)
 *     logger = DefaultLogger()
 * }
 * ```
 * 
 * ## Advanced Usage
 * 
 * ```kotlin
 * Flagship.configure {
 *     appKey = "my-app"
 *     environment = if (BuildConfig.DEBUG) "development" else "production"
 *     
 *     providers {
 *         + RestFlagsProvider(httpClient, "https://api.example.com/flags")
 *         + FirebaseProviderFactory.create(application)
 *     }
 *     
 *     cache = PersistentCache(FlagsSerializer())
 *     logger = DefaultLogger()
 *     defaultRefreshIntervalMs = 10 * 60_000L // 10 minutes
 *     
 *     analytics = object : FlagsAnalytics {
 *         override fun trackEvent(event: AnalyticsEvent) {
 *             // Track events
 *         }
 *     }
 *     
 *     enableRealtime = true
 * }
 * ```
 */
class FlagsConfigBuilder {
    /**
     * Application key for identifying the app.
     */
    var appKey: String = ""
    
    /**
     * Environment name (e.g., "production", "staging", "development").
     */
    var environment: String = "production"
    
    /**
     * Default refresh interval in milliseconds.
     * Default: 15 minutes (900,000 ms).
     */
    var defaultRefreshIntervalMs: Long = 15 * 60_000
    
    /**
     * List of flag providers.
     * Built using the [providers] DSL block.
     */
    private val providersList = mutableListOf<FlagsProvider>()
    
    /**
     * Cache implementation.
     * Default: [InMemoryCache] if not specified.
     */
    var cache: FlagsCache? = null
    
    /**
     * Serializer for flag snapshots.
     * Default: [JsonSerializer] if not specified.
     */
    var serializer: FlagsSerializer? = null
    
    /**
     * Logger for debug and error messages.
     * Default: [NoopLogger] if not specified.
     */
    var logger: FlagsLogger? = null
    
    /**
     * Clock abstraction for testing.
     * Default: [SystemClock] if not specified.
     */
    var clock: Clock? = null
    
    /**
     * Optional crypto implementation for signature verification.
     */
    var crypto: Crypto? = null
    
    /**
     * Analytics tracker for flag events.
     * Default: [NoopAnalytics] if not specified.
     */
    var analytics: FlagsAnalytics? = null
    
    /**
     * Performance monitoring for flag operations.
     * Default: [NoopPerformanceMonitor] if not specified.
     */
    var performanceMonitor: PerformanceMonitor? = null
    
    /**
     * Retry policy for failed provider operations.
     * Default: [NoRetryPolicy] if not specified.
     */
    var retryPolicy: RetryPolicy? = null
    
    /**
     * Enable realtime updates for [RealtimeFlagsProvider] instances.
     * Default: false.
     */
    var enableRealtime: Boolean? = null
    
    /**
     * Optional configuration for provider analytics reporting.
     */
    var providerAnalytics: ProviderAnalyticsConfig? = null
    
    /**
     * Enable performance profiling for flag operations.
     */
    var enablePerformanceProfiling: Boolean? = null
    
    /**
     * Maximum number of snapshots to keep in memory.
     */
    var maxSnapshots: Int? = null
    
    /**
     * Maximum age of snapshots before cleanup in milliseconds.
     */
    var maxSnapshotAgeMs: Long? = null
    
    /**
     * DSL block for adding providers.
     * 
     * Use the `+` operator to add providers:
     * ```kotlin
     * providers {
     *     + FirebaseProviderFactory.create(application)
     *     + RestFlagsProvider(httpClient, "https://api.example.com")
     * }
     * ```
     */
    fun providers(block: ProviderListBuilder.() -> Unit) {
        val builder = ProviderListBuilder(providersList)
        builder.block()
    }
    
    /**
     * Helper class for building provider list with `+` operator support.
     */
    inner class ProviderListBuilder(private val list: MutableList<FlagsProvider>) {
        /**
         * Add a provider using `+` operator.
         * 
         * This extension function allows using `+provider` syntax in the DSL block.
         */
        operator fun FlagsProvider.unaryPlus() {
            list.add(this)
        }
    }
    
    /**
     * Build [FlagsConfig] from the builder configuration.
     * 
     * @return Configured [FlagsConfig]
     * @throws IllegalArgumentException if appKey is empty
     */
    fun build(): FlagsConfig {
        require(appKey.isNotBlank()) {
            "appKey must be specified in FlagsConfigBuilder"
        }
        
        return FlagsConfig(
            appKey = appKey,
            environment = environment,
            defaultRefreshIntervalMs = defaultRefreshIntervalMs,
            providers = providersList.toList(),
            cache = cache ?: InMemoryCache(),
            serializer = serializer ?: JsonSerializer(),
            logger = logger ?: NoopLogger,
            clock = clock ?: SystemClock,
            crypto = crypto,
            analytics = analytics ?: NoopAnalytics,
            performanceMonitor = performanceMonitor ?: NoopPerformanceMonitor,
            retryPolicy = retryPolicy ?: NoRetryPolicy,
            enableRealtime = enableRealtime ?: false,
            providerAnalytics = providerAnalytics,
            enablePerformanceProfiling = enablePerformanceProfiling,
            maxSnapshots = maxSnapshots,
            maxSnapshotAgeMs = maxSnapshotAgeMs
        )
    }
}

