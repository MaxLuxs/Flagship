package io.maxluxs.flagship.sample

import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.delay

/**
 * Mock flags provider for demo purposes.
 * Returns predefined flags and experiments without requiring a real backend.
 */
class MockFlagsProvider : FlagsProvider {
    override val name: String = "mock"

    private val mockSnapshot = ProviderSnapshot(
        flags = mapOf(
            "new_feature" to FlagValue.Bool(true),
            "dark_mode" to FlagValue.Bool(false),
            "max_retries" to FlagValue.Int(3),
            "api_timeout" to FlagValue.Double(30.0),
            "welcome_message" to FlagValue.StringV("Welcome to Flagship Demo!"),
            "payment_enabled" to FlagValue.Bool(true)
        ),
        experiments = mapOf(
            "test_experiment" to ExperimentDefinition(
                key = "test_experiment",
                variants = listOf(
                    Variant(name = "control", weight = 0.5),
                    Variant(name = "treatment", weight = 0.5)
                ),
                targeting = null,
                exposureType = ExposureType.OnAssign
            ),
            "checkout_flow" to ExperimentDefinition(
                key = "checkout_flow",
                variants = listOf(
                    Variant(name = "control", weight = 0.33),
                    Variant(name = "variant_a", weight = 0.33),
                    Variant(name = "variant_b", weight = 0.34)
                ),
                targeting = TargetingRule.Composite(
                    all = listOf(
                        TargetingRule.AppVersionGte("1.0.0")
                    )
                ),
                exposureType = ExposureType.OnAssign
            )
        ),
        revision = "mock-v1",
        fetchedAtMs = currentTimeMillis(),
        ttlMs = 15 * 60_000L
    )

    override suspend fun bootstrap(): ProviderSnapshot {
        // Simulate network delay
        delay(500)
        return mockSnapshot
    }

    override suspend fun refresh(): ProviderSnapshot {
        // Simulate network delay
        delay(300)
        return mockSnapshot.copy(
            fetchedAtMs = currentTimeMillis()
        )
    }

    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return mockSnapshot.flags[key]
    }

    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = mockSnapshot.experiments[key] ?: return null
        
        // Simple bucketing based on userId hash
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

    override fun isHealthy(): Boolean = true

    override fun getLastSuccessfulFetchMs(): Long? = mockSnapshot.fetchedAtMs

    override fun getConsecutiveFailures(): Int = 0
}

