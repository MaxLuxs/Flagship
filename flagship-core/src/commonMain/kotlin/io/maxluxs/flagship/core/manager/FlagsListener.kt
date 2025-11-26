package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.model.*

/**
 * Listener for flag and experiment events.
 * 
 * Extend this interface to receive notifications about:
 * - Snapshot updates from providers
 * - Flag value changes
 * - Experiment assignments
 * - Provider status changes
 * - Override changes
 */
interface FlagsListener {
    /**
     * Called when a snapshot is updated from a provider.
     * 
     * @param source Provider name or update source (e.g., "bootstrap", "refresh", "realtime")
     */
    fun onSnapshotUpdated(source: String) {}
    
    /**
     * Called when a local override is changed.
     * 
     * @param key The flag key that was overridden
     */
    fun onOverrideChanged(key: FlagKey) {}
    
    /**
     * Called when a flag value changes.
     * 
     * @param key The flag key
     * @param oldValue Previous value (null if flag was just created)
     * @param newValue New value (null if flag was deleted)
     */
    fun onFlagChanged(key: FlagKey, oldValue: FlagValue?, newValue: FlagValue?) {}
    
    /**
     * Called when a user is assigned to an experiment variant.
     * 
     * @param experimentKey The experiment key
     * @param assignment The assignment result
     */
    fun onExperimentAssigned(experimentKey: ExperimentKey, assignment: ExperimentAssignment) {}
    
    /**
     * Called when a provider status changes.
     * 
     * @param providerName The provider name
     * @param isHealthy Whether the provider is now healthy
     */
    fun onProviderUpdated(providerName: String, isHealthy: Boolean) {}
}

