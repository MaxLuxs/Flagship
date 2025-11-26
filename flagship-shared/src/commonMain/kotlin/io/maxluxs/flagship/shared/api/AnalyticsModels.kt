package io.maxluxs.flagship.shared.api

import kotlinx.serialization.Serializable

/**
 * Shared analytics models for provider metrics and event analytics.
 * 
 * These models are used for:
 * - Server API endpoints (flagship-server)
 * - Admin UI API client (flagship-admin-ui-compose)
 * - Analytics and monitoring features
 */

// ============================================================================
// Provider Analytics Models
// ============================================================================

/**
 * Metrics data for a specific provider.
 * 
 * Contains information about request counts, success rates, response times,
 * and health status indicators.
 * 
 * @property providerName Name of the provider (e.g., "firebase", "rest")
 * @property totalRequests Total number of requests made to this provider
 * @property successfulRequests Number of successful requests
 * @property failedRequests Number of failed requests
 * @property averageResponseTimeMs Average response time in milliseconds
 * @property lastRequestTimeMs Timestamp of the last request (milliseconds since epoch)
 * @property lastSuccessTimeMs Timestamp of the last successful request
 * @property lastFailureTimeMs Timestamp of the last failed request
 * @property consecutiveFailures Number of consecutive failures
 * @property timestamp Timestamp when these metrics were recorded
 */
@Serializable
data class ProviderMetricsData(
    val providerName: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMs: Double,
    val lastRequestTimeMs: Long?,
    val lastSuccessTimeMs: Long?,
    val lastFailureTimeMs: Long?,
    val consecutiveFailures: Int,
    val timestamp: Long
) {
    /**
     * Calculated success rate as a ratio of successful requests to total requests.
     * Returns 0.0 if no requests have been made.
     */
    val successRate: Double
        get() = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0

    /**
     * Health status indicator.
     * Provider is considered healthy if:
     * - Consecutive failures < 5
     * - Success rate > 0.8 (80%)
     */
    val isHealthy: Boolean
        get() = consecutiveFailures < 5 && successRate > 0.8
}

/**
 * Health status summary for a provider.
 * 
 * Provides a quick overview of provider health without full metrics data.
 * 
 * @property providerName Name of the provider
 * @property isHealthy Whether the provider is currently healthy
 * @property successRate Success rate as a decimal (0.0 to 1.0)
 * @property consecutiveFailures Number of consecutive failures
 * @property lastSuccessTimeMs Timestamp of the last successful request
 * @property averageResponseTimeMs Average response time in milliseconds
 */
@Serializable
data class ProviderHealthStatus(
    val providerName: String,
    val isHealthy: Boolean,
    val successRate: Double,
    val consecutiveFailures: Int,
    val lastSuccessTimeMs: Long?,
    val averageResponseTimeMs: Double
)

/**
 * Request model for recording provider metrics.
 * 
 * This is an internal model used by the server to accept metrics data
 * from providers. It should not be exposed in public API documentation.
 * 
 * @property providerName Name of the provider
 * @property totalRequests Total number of requests
 * @property successfulRequests Number of successful requests
 * @property failedRequests Number of failed requests
 * @property averageResponseTimeMs Average response time in milliseconds
 * @property lastRequestTimeMs Timestamp of the last request
 * @property lastSuccessTimeMs Timestamp of the last successful request
 * @property lastFailureTimeMs Timestamp of the last failed request
 * @property consecutiveFailures Number of consecutive failures
 */
@Serializable
data class ProviderMetricsRequest(
    val providerName: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMs: Double,
    val lastRequestTimeMs: Long?,
    val lastSuccessTimeMs: Long?,
    val lastFailureTimeMs: Long?,
    val consecutiveFailures: Int
)

/**
 * Analytics event representing an action or occurrence in the system.
 * 
 * Events are used to track usage, conversions, and other important metrics.
 * 
 * @property projectId ID of the project this event belongs to
 * @property eventType Type of event (e.g., "flag_enabled", "experiment_assigned", "conversion")
 * @property entityType Type of entity involved (e.g., "flag", "experiment")
 * @property entityId ID of the entity (e.g., flag key, experiment key)
 * @property userId ID of the user who triggered the event (optional)
 * @property attributes Additional attributes as key-value pairs
 * @property timestamp Timestamp when the event occurred (milliseconds since epoch)
 * 
 * Note: The timestamp is typically set by the server when the event is recorded.
 * If not provided, the server will use the current time.
 */
@Serializable
data class AnalyticsEvent(
    val projectId: String,
    val eventType: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val userId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val timestamp: Long
)

// ============================================================================
// Event Analytics Models
// ============================================================================

/**
 * Statistics for a specific flag.
 * 
 * Tracks usage metrics for individual flags.
 * 
 * @property flagKey Key of the flag
 * @property enabledCount Number of times the flag was enabled/used
 * @property disabledCount Number of times the flag was disabled/not used
 * @property totalRequests Total number of requests for this flag
 */
@Serializable
data class FlagStats(
    val flagKey: String,
    val enabledCount: Long,
    val disabledCount: Long,
    val totalRequests: Long
)

/**
 * Statistics for a specific experiment.
 * 
 * Tracks variant distribution and assignment counts for experiments.
 * 
 * @property experimentKey Key of the experiment
 * @property variantDistribution Map of variant names to assignment counts
 * @property totalAssignments Total number of assignments for this experiment
 */
@Serializable
data class ExperimentStats(
    val experimentKey: String,
    val variantDistribution: Map<String, Long>,
    val totalAssignments: Long
)

/**
 * Overview of analytics data for a project.
 * 
 * Provides aggregated statistics for flags, experiments, and events
 * over a specific time period.
 * 
 * @property projectId ID of the project
 * @property totalFlags Total number of flags in the project
 * @property totalExperiments Total number of experiments in the project
 * @property totalEvents Total number of analytics events recorded
 * @property flagStats List of statistics for each flag
 * @property experimentStats List of statistics for each experiment
 * @property period Time period for the statistics (e.g., "1h", "24h", "7d", "30d")
 */
@Serializable
data class AnalyticsOverview(
    val projectId: String,
    val totalFlags: Int,
    val totalExperiments: Int,
    val totalEvents: Long,
    val flagStats: List<FlagStats>,
    val experimentStats: List<ExperimentStats>,
    val period: String
)
