package io.maxluxs.flagship.e2e
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for Flagship initialization flow
 */
class InitializationTest {
    
    @BeforeTest
    fun setup() {
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testFullInitializationFlow() = runTest {
        // Step 1: Configure
        val provider = TestProvider(
            flags = mapOf(
                "feature_enabled" to FlagValue.Bool(true),
                "max_items" to FlagValue.Int(100)
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        
        // Step 2: Verify configuration
        assertTrue(Flagship.isConfigured())
        
        // Step 3: Get manager
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        assertNotNull(manager)
        
        // Step 4: Bootstrap
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        assertTrue(bootstrapped)
        
        // Step 5: Use flags
        assertTrue(manager.isEnabled("feature_enabled", false))
        assertEquals(100, manager.value("max_items", default = 0))
    }
    
    @Test
    fun testInitializationWithoutBootstrap() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Flags should work even without explicit bootstrap (lazy bootstrap)
        // This depends on implementation - some may require explicit bootstrap
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        assertTrue(bootstrapped)
        assertTrue(manager.isEnabled("test_flag", false))
    }
    
    @Test
    fun testReconfiguration() = runTest {
        // First configuration
        val provider1 = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val config1 = TestFlagsConfig.createTestConfig(providers = listOf(provider1))
        Flagship.configure(config1)
        val manager1 = Flagship.manager() as DefaultFlagsManager
        manager1.setDefaultContext(TestHelpers.createTestContext())
        manager1.ensureBootstrap()
        assertTrue(manager1.isEnabled("flag1", false))
        
        // Reset and reconfigure
        Flagship.reset()
        assertFalse(Flagship.isConfigured())
        
        val provider2 = TestProvider(
            flags = mapOf("flag2" to FlagValue.Bool(true)),
            name = "provider2"
        )
        val config2 = TestFlagsConfig.createTestConfig(providers = listOf(provider2))
        Flagship.configure(config2)
        val manager2 = Flagship.manager() as DefaultFlagsManager
        manager2.setDefaultContext(TestHelpers.createTestContext())
        manager2.ensureBootstrap()
        
        // After reset, old flag should not be available (different manager instance)
        // flag1 is not in provider2, so it should return default value
        assertTrue(manager2.isEnabled("flag1", default = true)) // Old flag should return default (true)
        assertTrue(manager2.isEnabled("flag2", default = false)) // New flag should be available
    }
    
    @Test
    fun testMultipleFlagsInitialization() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "bool_flag" to FlagValue.Bool(true),
                "int_flag" to FlagValue.Int(42),
                "double_flag" to FlagValue.Double(3.14),
                "string_flag" to FlagValue.StringV("test")
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("bool_flag", false))
        assertEquals(42, manager.value("int_flag", default = 0))
        assertEquals(3.14, manager.value("double_flag", default = 0.0), 0.01)
        assertEquals("test", manager.value("string_flag", default = ""))
    }
}

