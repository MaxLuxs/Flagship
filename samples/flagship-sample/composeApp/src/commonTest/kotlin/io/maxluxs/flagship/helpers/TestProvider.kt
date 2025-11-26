package io.maxluxs.flagship.helpers

import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.currentTimeMillis

/**
 * Test provider that accepts custom flags and experiments
 */
class TestProvider(
    private val flags: Map<FlagKey, FlagValue> = emptyMap(),
    private val experiments: Map<ExperimentKey, ExperimentDefinition> = emptyMap(),
    override val name: String = "test"
) : FlagsProvider {
    
    private val snapshot = ProviderSnapshot(
        flags = flags,
        experiments = experiments,
        revision = "test-v1",
        fetchedAtMs = currentTimeMillis(),
        ttlMs = 15 * 60_000L
    )
    
    override suspend fun bootstrap(): ProviderSnapshot {
        return snapshot
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        return snapshot.copy(fetchedAtMs = currentTimeMillis())
    }
    
    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return snapshot.flags[key]
    }
    
    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = snapshot.experiments[key] ?: return null
        
        // Simple bucketing
        val userId = context.userId ?: context.deviceId ?: "anonymous"
        val hash = userId.hashCode().let { if (it < 0) -it else it }
        val bucket = hash % 100
        
        var cumulative = 0.0
        for (variant in experiment.variants) {
            cumulative += variant.weight * 100
            if (bucket < cumulative) {
                return ExperimentAssignment(
                    key = key,
                    variant = variant.name,
                    payload = variant.payload
                )
            }
        }
        
        return ExperimentAssignment(
            key = key,
            variant = experiment.variants.first().name,
            payload = experiment.variants.first().payload
        )
    }
}

