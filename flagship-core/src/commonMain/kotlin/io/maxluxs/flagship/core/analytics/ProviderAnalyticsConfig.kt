package io.maxluxs.flagship.core.analytics

/**
 * Configuration for provider analytics reporting.
 * 
 * When configured, the SDK will automatically track provider metrics
 * (success rate, response time, failures) and send them to the Flagship server.
 * 
 * @property projectId Project ID for metrics (required)
 * @property analyticsUrl Base URL for analytics endpoint (default: same as REST provider baseUrl)
 * @property apiKey API key for authentication (optional, can use project-based auth)
 * @property reportingIntervalMs Interval between reports in milliseconds (default: 5 minutes)
 * @property batchSize Maximum number of metrics to send in one batch (default: 10)
 * @property maxRetries Maximum number of retry attempts (default: 3)
 * @property enabled Whether provider analytics is enabled (default: true)
 */
data class ProviderAnalyticsConfig(
    val projectId: String,
    val analyticsUrl: String? = null,
    val apiKey: String? = null,
    val reportingIntervalMs: Long = 5 * 60 * 1000L, // 5 minutes
    val batchSize: Int = 10,
    val maxRetries: Int = 3,
    val enabled: Boolean = true
)

