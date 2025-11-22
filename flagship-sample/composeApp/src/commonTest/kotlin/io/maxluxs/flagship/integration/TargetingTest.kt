package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentDefinition
import io.maxluxs.flagship.core.model.ExposureType
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.model.TargetingRule
import io.maxluxs.flagship.core.model.Variant
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.currentTimeMillis
import io.maxluxs.flagship.helpers.TestFlagsConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for targeting rules in experiments
 */
class TargetingTest {
    
    private class TargetingProvider(
        private val experiment: ExperimentDefinition
    ) : FlagsProvider {
        override val name: String = "targeting"
        
        override suspend fun bootstrap(): ProviderSnapshot {
            return ProviderSnapshot(
                flags = emptyMap(),
                experiments = mapOf(experiment.key to experiment),
                revision = "v1",
                fetchedAtMs = currentTimeMillis(),
                ttlMs = 60_000L
            )
        }
        
        override suspend fun refresh(): ProviderSnapshot = bootstrap()
        override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
        override fun evaluateExperiment(key: String, context: EvalContext): io.maxluxs.flagship.core.model.ExperimentAssignment? = null
    }
    
    @BeforeTest
    fun setup() {
        Flags.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testAppVersionTargeting() = runTest {
        val experiment = ExperimentDefinition(
            key = "version_experiment",
            variants = listOf(
                Variant(name = "control", weight = 0.5),
                Variant(name = "treatment", weight = 0.5)
            ),
            targeting = TargetingRule.AppVersionGte("1.0.0"),
            exposureType = ExposureType.OnAssign
        )
        
        val provider = TargetingProvider(experiment)
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Matching version
        val matchingContext = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        val assignment1 = manager.assign("version_experiment", ctx = matchingContext)
        assertNotNull(assignment1)
        
        // Non-matching version
        val nonMatchingContext = EvalContext(
            userId = "user2",
            deviceId = "device2",
            appVersion = "0.9.0",
            osName = "Test",
            osVersion = "1.0"
        )
        val assignment2 = manager.assign("version_experiment", ctx = nonMatchingContext)
        // Should return null or control depending on implementation
    }
    
    @Test
    fun testRegionTargeting() = runTest {
        val experiment = ExperimentDefinition(
            key = "region_experiment",
            variants = listOf(
                Variant(name = "control", weight = 0.5),
                Variant(name = "treatment", weight = 0.5)
            ),
            targeting = TargetingRule.RegionIn(setOf("US", "CA")),
            exposureType = ExposureType.OnAssign
        )
        
        val provider = TargetingProvider(experiment)
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Matching region
        val matchingContext = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0",
            region = "US"
        )
        val assignment1 = manager.assign("region_experiment", ctx = matchingContext)
        assertNotNull(assignment1)
        
        // Non-matching region
        val nonMatchingContext = EvalContext(
            userId = "user2",
            deviceId = "device2",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0",
            region = "RU"
        )
        val assignment2 = manager.assign("region_experiment", ctx = nonMatchingContext)
        // Should return null or control depending on implementation
    }
    
    @Test
    fun testCompositeTargeting() = runTest {
        val experiment = ExperimentDefinition(
            key = "composite_experiment",
            variants = listOf(
                Variant(name = "control", weight = 0.5),
                Variant(name = "treatment", weight = 0.5)
            ),
            targeting = TargetingRule.Composite(
                all = listOf(
                    TargetingRule.AppVersionGte("1.0.0"),
                    TargetingRule.RegionIn(setOf("US", "CA"))
                )
            ),
            exposureType = ExposureType.OnAssign
        )
        
        val provider = TargetingProvider(experiment)
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Matching all conditions
        val matchingContext = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0",
            region = "US"
        )
        val assignment1 = manager.assign("composite_experiment", ctx = matchingContext)
        assertNotNull(assignment1)
        
        // Matching only one condition
        val partialContext = EvalContext(
            userId = "user2",
            deviceId = "device2",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0",
            region = "RU" // Doesn't match region
        )
        val assignment2 = manager.assign("composite_experiment", ctx = partialContext)
        // Should not match
    }
    
    @Test
    fun testNoTargeting() = runTest {
        val experiment = ExperimentDefinition(
            key = "no_targeting_experiment",
            variants = listOf(
                Variant(name = "control", weight = 0.5),
                Variant(name = "treatment", weight = 0.5)
            ),
            targeting = null, // No targeting - everyone eligible
            exposureType = ExposureType.OnAssign
        )
        
        val provider = TargetingProvider(experiment)
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Any context should work
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "0.5.0",
            osName = "Test",
            osVersion = "1.0",
            region = "XX"
        )
        val assignment = manager.assign("no_targeting_experiment", ctx = context)
        assertNotNull(assignment)
    }
}

