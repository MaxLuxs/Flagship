package io.maxluxs.flagship.core.analytics

import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey

/**
 * Analytics interface for tracking flag and experiment events.
 * 
 * Implement this interface to send flag evaluation and experiment assignment
 * events to your analytics backend (e.g., Firebase Analytics, Amplitude, Mixpanel).
 * 
 * Example implementation:
 * ```kotlin
 * class FirebaseAnalyticsAdapter : FlagsAnalytics {
 *     override fun trackFlagEvaluated(key: FlagKey, value: Any?, source: String) {
 *         Firebase.analytics.logEvent("flag_evaluated") {
 *             param("flag_key", key)
 *             param("flag_value", value.toString())
 *             param("source", source)
 *         }
 *     }
 *     
 *     override fun trackExperimentAssignment(key: ExperimentKey, variant: String) {
 *         Firebase.analytics.logEvent("experiment_assigned") {
 *             param("experiment_key", key)
 *             param("variant", variant)
 *         }
 *     }
 * }
 * ```
 */
interface FlagsAnalytics {
    /**
     * Track when a flag is evaluated.
     * 
     * Called every time a flag value is accessed. Consider debouncing or
     * sampling to avoid excessive events.
     * 
     * @param key The flag key that was evaluated
     * @param value The evaluated value
     * @param source Source of the value ("override", "provider", "default")
     */
    fun trackFlagEvaluated(key: FlagKey, value: Any?, source: String)

    /**
     * Track when a user is assigned to an experiment variant.
     * 
     * This should be called only once per experiment per user (exposure tracking).
     * 
     * @param key The experiment key
     * @param variant The assigned variant name
     */
    fun trackExperimentAssignment(key: ExperimentKey, variant: String)

    /**
     * Track when a flag configuration is refreshed.
     * 
     * @param providerName The provider that was refreshed
     * @param success Whether the refresh was successful
     * @param durationMs Time taken for the refresh in milliseconds
     */
    fun trackConfigRefreshed(providerName: String, success: Boolean, durationMs: Long)

    /**
     * Track when an error occurs during flag operations.
     * 
     * @param operation The operation that failed (e.g., "bootstrap", "refresh", "evaluate")
     * @param error The error message or exception
     */
    fun trackError(operation: String, error: String)
}

/**
 * No-op implementation of FlagsAnalytics that does nothing.
 * Use this as default when analytics is not configured.
 */
object NoopAnalytics : FlagsAnalytics {
    override fun trackFlagEvaluated(key: FlagKey, value: Any?, source: String) {}
    override fun trackExperimentAssignment(key: ExperimentKey, variant: String) {}
    override fun trackConfigRefreshed(providerName: String, success: Boolean, durationMs: Long) {}
    override fun trackError(operation: String, error: String) {}
}

