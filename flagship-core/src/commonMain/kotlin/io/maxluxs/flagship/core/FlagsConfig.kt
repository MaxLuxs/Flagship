package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.analytics.FlagsAnalytics
import io.maxluxs.flagship.core.analytics.NoopAnalytics
import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.performance.NoopPerformanceMonitor
import io.maxluxs.flagship.core.performance.PerformanceMonitor
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.retry.NoRetryPolicy
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.util.*
import io.maxluxs.flagship.core.analytics.ProviderAnalyticsConfig

/**
 * Configuration for the Flagship flags system.
 * 
 * This configures all aspects of the flags library including:
 * - Which providers to use and in what order
 * - Caching strategy
 * - Refresh intervals
 * - Analytics and monitoring
 * - Retry policies
 * - Logging
 * 
 * ## Basic Example
 * 
 * ```kotlin
 * val config = FlagsConfig(
 *     appKey = "my-app",
 *     environment = "production",
 *     providers = listOf(
 *         RestFlagsProvider(httpClient, "https://api.example.com/flags"),
 *         FirebaseRemoteConfigProvider(firebase)
 *     ),
 *     cache = PersistentCache(serializer)
 * )
 * 
 * Flagship.configure(config)
 * ```
 * 
 * ## Advanced Example with Analytics
 * 
 * ```kotlin
 * val config = FlagsConfig(
 *     appKey = "my-app",
 *     environment = if (BuildConfig.DEBUG) "development" else "production",
 *     providers = listOf(
 *         FirebaseProviderFactory.create(application),
 *         RestFlagsProvider(httpClient, "https://api.example.com/flags")
 *     ),
 *     cache = PersistentCache(FlagsSerializer()),
 *     analytics = object : FlagsAnalytics {
 *         override fun trackEvent(event: AnalyticsEvent) {
 *             when (event) {
 *                 is AnalyticsEvent.ExperimentExposure -> {
 *                     Firebase.analytics.logEvent("experiment_exposure") {
 *                         param("experiment", event.experimentKey)
 *                         param("variant", event.variant)
 *                     }
 *                 }
 *             }
 *         }
 *     },
 *     providerAnalytics = ProviderAnalyticsConfig(
 *         projectId = "my-project",
 *         analyticsUrl = "https://api.example.com"
 *     ),
 *     defaultRefreshIntervalMs = 10 * 60_000L, // 10 minutes
 *     enableRealtime = true
 * )
 * ```
 * 
 * @property appKey Unique application identifier. Used for analytics and provider identification.
 * @property environment Environment name (e.g., "production", "staging", "development"). 
 *   Providers can return different values based on environment.
 * @property defaultRefreshIntervalMs How often to automatically refresh configuration in milliseconds. 
 *   Default: 15 minutes (900,000 ms). Set to 0 to disable automatic refresh.
 * @property providers List of flag providers in order of precedence. First provider has highest priority.
 *   If first provider fails or doesn't have a flag, subsequent providers are tried.
 * @property cache Cache implementation for persisting flag snapshots. Use [PersistentCache] for 
 *   offline support, or [InMemoryCache] for faster but non-persistent caching.
 * @property serializer Serializer for flag snapshots. Default: [JsonSerializer]. Only needed if 
 *   using custom cache implementation.
 * @property logger Logger for debug and error messages. Default: [NoopLogger]. Use [DefaultLogger] 
 *   for console logging or implement custom logger for your logging framework.
 * @property clock Clock abstraction for testing. Default: [SystemClock]. Override for time-based 
 *   testing scenarios.
 * @property crypto Optional crypto implementation for signature verification. If provided, snapshots 
 *   will be verified against their signatures to prevent tampering.
 * @property analytics Analytics tracker for flag events (experiment exposures, conversions). 
 *   Default: [NoopAnalytics]. Implement to track events in Firebase Analytics, Amplitude, etc.
 * @property performanceMonitor Performance monitoring for flag operations. Default: [NoopPerformanceMonitor].
 *   Use to track bootstrap times, refresh durations, etc.
 * @property retryPolicy Retry policy for failed provider operations. Default: [NoRetryPolicy]. 
 *   Use [ExponentialBackoffRetry] for automatic retries with exponential backoff.
 * @property enableRealtime Enable realtime updates for [RealtimeFlagsProvider] instances. 
 *   Default: false. When enabled, flags update instantly via WebSocket/SSE without polling.
 * @property providerAnalytics Optional configuration for provider analytics reporting. 
 *   Default: null (disabled). When enabled, SDK automatically reports provider health metrics 
 *   (success rate, response time, failures) to your backend.
 * @property enablePerformanceProfiling Enable performance profiling for flag operations. 
 *   Default: null (auto-detected). When enabled, tracks detailed performance metrics.
 * @property maxSnapshots Maximum number of snapshots to keep in memory for rollback purposes. 
 *   Default: null (unlimited). Set to limit memory usage.
 * @property maxSnapshotAgeMs Maximum age of snapshots before cleanup in milliseconds. 
 *   Default: null (24 hours). Older snapshots are automatically removed.
 */
data class FlagsConfig(
    val appKey: String,
    val environment: String,
    val defaultRefreshIntervalMs: Long = 15 * 60_000,
    val providers: List<FlagsProvider>,
    val cache: FlagsCache,
    val serializer: FlagsSerializer = JsonSerializer(),
    val logger: FlagsLogger = NoopLogger,
    val clock: Clock = SystemClock,
    val crypto: Crypto? = null,
    val analytics: FlagsAnalytics = NoopAnalytics,
    val performanceMonitor: PerformanceMonitor = NoopPerformanceMonitor,
    val retryPolicy: RetryPolicy = NoRetryPolicy,
    val enableRealtime: Boolean = false,
    val providerAnalytics: ProviderAnalyticsConfig? = null,
    val enablePerformanceProfiling: Boolean? = null,
    val maxSnapshots: Int? = null,
    val maxSnapshotAgeMs: Long? = null
)

