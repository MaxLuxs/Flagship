package io.maxluxs.flagship.core.analytics

import io.maxluxs.flagship.core.model.ExperimentKey

/**
 * Tracks experiment exposure to prevent duplicate tracking.
 * 
 * Keeps a record of which experiments a user has been exposed to
 * in the current session to ensure each exposure is tracked only once.
 * 
 * This is critical for accurate A/B test analysis.
 */
class ExposureTracker {
    private val exposedExperiments = mutableSetOf<String>()

    /**
     * Check if an experiment has been tracked for this session.
     * 
     * @param experimentKey The experiment key
     * @return true if already tracked, false otherwise
     */
    fun isExposed(experimentKey: ExperimentKey): Boolean {
        return experimentKey in exposedExperiments
    }

    /**
     * Mark an experiment as exposed.
     * 
     * @param experimentKey The experiment key to mark
     */
    fun markExposed(experimentKey: ExperimentKey) {
        exposedExperiments.add(experimentKey)
    }

    /**
     * Track exposure if not already tracked.
     * 
     * @param experimentKey The experiment key
     * @param variant The assigned variant
     * @param analytics The analytics instance to track with
     * @return true if tracking was performed, false if already exposed
     */
    fun trackIfNeeded(
        experimentKey: ExperimentKey,
        variant: String,
        analytics: FlagsAnalytics
    ): Boolean {
        if (isExposed(experimentKey)) {
            return false
        }
        
        markExposed(experimentKey)
        analytics.trackExperimentAssignment(experimentKey, variant)
        return true
    }

    /**
     * Clear all exposure tracking.
     * Call this when starting a new session or after user logout.
     */
    fun clear() {
        exposedExperiments.clear()
    }

    /**
     * Get count of exposed experiments in current session.
     */
    fun getExposureCount(): Int = exposedExperiments.size
}

