package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.sample.MockFlagsProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for experiment assignment and targeting
 */
class ExperimentTest {
    
    @BeforeTest
    fun setup() {
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testExperimentAssignment() = runTest {
        val provider = MockFlagsProvider()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        val context = EvalContext(
            osName = "Test",
            osVersion = "1.0",
            userId = "test_user",
            deviceId = "test_device",
            appVersion = "1.0.0"
        )
        
        val assignment = manager.assign("test_experiment", ctx = context)
        assertNotNull(assignment)
        assertTrue(assignment.variant in listOf("control", "treatment"))
        assertEquals("test_experiment", assignment.key)
    }
    
    @Test
    fun testDeterministicAssignment() = runTest {
        val provider = MockFlagsProvider()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        val context = EvalContext(
            userId = "same_user",
            deviceId = "same_device",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        
        // Same user should get same assignment
        val assignment1 = manager.assign("test_experiment", ctx = context)
        val assignment2 = manager.assign("test_experiment", ctx = context)
        
        assertNotNull(assignment1)
        assertNotNull(assignment2)
        assertEquals(assignment1.variant, assignment2.variant)
    }
    
    @Test
    fun testTargetingRules() = runTest {
        val provider = MockFlagsProvider()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Test with matching targeting
        val matchingContext = EvalContext(
            userId = "test_user",
            deviceId = "test_device",
            appVersion = "1.0.0", // Matches targeting rule in MockFlagsProvider
            osName = "Test",
            osVersion = "1.0"
        )
        
        val assignment = manager.assign("checkout_flow", ctx = matchingContext)
        assertNotNull(assignment)
        assertTrue(assignment.variant in listOf("control", "variant_a", "variant_b"))
        
        // Test with non-matching targeting
        val nonMatchingContext = EvalContext(
            userId = "test_user",
            deviceId = "test_device",
            appVersion = "0.9.0", // Doesn't match targeting rule
            osName = "Test",
            osVersion = "1.0"
        )
        
        val assignment2 = manager.assign("checkout_flow", ctx = nonMatchingContext)
        // Should return null or control variant depending on implementation
    }
    
    @Test
    fun testExperimentExposure() = runTest {
        val provider = MockFlagsProvider()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        val context = EvalContext(
            osName = "Test",
            osVersion = "1.0",
            userId = "test_user",
            deviceId = "test_device",
            appVersion = "1.0.0"
        )
        
        val assignment = manager.assign("test_experiment", ctx = context)
        assertNotNull(assignment)
        
        // Exposure should be tracked (depends on implementation)
        // This is a placeholder - actual exposure tracking depends on ExposureTracker
    }
}

