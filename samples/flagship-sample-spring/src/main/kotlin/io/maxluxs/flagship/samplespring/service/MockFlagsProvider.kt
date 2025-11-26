package io.maxluxs.flagship.samplespring.service

import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.delay

/**
 * Mock flags provider for demo purposes.
 * 
 * Returns predefined flags and experiments without requiring a real backend.
 * This implementation extends BaseFlagsProvider for common functionality.
 */
class MockFlagsProvider : BaseFlagsProvider(name = "mock") {
    
    private fun createMockSnapshot(): ProviderSnapshot {
        return ProviderSnapshot(
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
    }
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        // Simulate network delay
        delay(300)
        return createMockSnapshot().copy(
            fetchedAtMs = currentTimeMillis(),
            revision = "mock-v${System.currentTimeMillis()}"
        )
    }
}

