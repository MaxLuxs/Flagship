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
 * Example:
 * ```kotlin
 * val config = FlagsConfig(
 *     appKey = "my-app",
 *     environment = "production",
 *     providers = listOf(
 *         RestFlagsProvider(httpClient, "https://api.example.com/flags"),
 *         FirebaseRemoteConfigProvider(firebase)
 *     ),
 *     cache = PersistentCache(serializer),
 *     analytics = FirebaseAnalyticsAdapter(),
 *     retryPolicy = ExponentialBackoffRetry(maxAttempts = 3)
 * )
 * 
 * Flags.configure(config)
 * ```
 * 
 * @property appKey Unique application identifier
 * @property environment Environment name (e.g., "production", "staging", "development")
 * @property defaultRefreshIntervalMs How often to refresh configuration in milliseconds (default: 15 minutes)
 * @property providers List of flag providers in order of precedence (first has highest priority)
 * @property cache Cache implementation for persisting flag snapshots
 * @property serializer Serializer for flag snapshots (default: JSON)
 * @property logger Logger for debug and error messages
 * @property clock Clock abstraction for testing (default: system clock)
 * @property crypto Optional crypto for signature verification
 * @property analytics Analytics tracker for flag events (default: no-op)
 * @property performanceMonitor Performance monitoring (default: no-op)
 * @property retryPolicy Retry policy for failed provider operations (default: no retry)
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
    val retryPolicy: RetryPolicy = NoRetryPolicy
)

