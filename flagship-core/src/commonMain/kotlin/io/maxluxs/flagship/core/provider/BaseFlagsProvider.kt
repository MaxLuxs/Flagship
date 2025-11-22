package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.*

/**
 * Base implementation for FlagsProvider that handles common patterns.
 * 
 * Most providers follow the same structure:
 * - Store a snapshot internally
 * - Evaluate flags/experiments from the snapshot
 * - Use BucketingEngine for experiment assignment
 * 
 * Subclasses only need to implement snapshot fetching logic.
 */
abstract class BaseFlagsProvider(
    override val name: String
) : FlagsProvider {
    
    protected var snapshot: ProviderSnapshot = ProviderSnapshot(
        flags = emptyMap(),
        experiments = emptyMap(),
        revision = null,
        fetchedAtMs = 0L
    )
    
    /**
     * Fetch and update the snapshot.
     * Subclasses should implement this to fetch from their specific backend.
     * 
     * @param currentRevision Optional current revision for incremental updates
     * @return New ProviderSnapshot
     */
    protected abstract suspend fun fetchSnapshot(currentRevision: String? = null): ProviderSnapshot
    
    override suspend fun bootstrap(): ProviderSnapshot {
        snapshot = fetchSnapshot()
        return snapshot
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        snapshot = fetchSnapshot(snapshot.revision)
        return snapshot
    }
    
    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return snapshot.flags[key]
    }
    
    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = snapshot.experiments[key] ?: return null
        return BucketingEngine.assign(experiment, context)
    }
}

