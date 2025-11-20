package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.*

class FlagsEvaluator {
    fun evaluateFlag(
        key: FlagKey,
        overrides: Map<FlagKey, FlagValue>,
        snapshots: List<ProviderSnapshot>,
        default: FlagValue?
    ): FlagValue? {
        // Priority: overrides > providers (in order) > default
        overrides[key]?.let { return it }

        for (snapshot in snapshots) {
            snapshot.flags[key]?.let { return it }
        }

        return default
    }

    fun evaluateExperiment(
        key: ExperimentKey,
        context: EvalContext,
        snapshots: List<ProviderSnapshot>
    ): ExperimentAssignment? {
        // Find experiment definition from first available provider
        for (snapshot in snapshots) {
            snapshot.experiments[key]?.let { experiment ->
                return BucketingEngine.assign(experiment, context)
            }
        }

        return null
    }

    fun isSnapshotExpired(snapshot: ProviderSnapshot, currentTimeMs: Long): Boolean {
        snapshot.ttlMs?.let { ttl ->
            return (currentTimeMs - snapshot.fetchedAtMs) > ttl
        }
        return false
    }
}

