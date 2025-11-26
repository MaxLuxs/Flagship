package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for edge cases and error handling
 */
class EdgeCasesTest {
    
    @BeforeTest
    fun setup() {
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testEmptyProviderList() = runTest {
        val config = TestFlagsConfig.createTestConfig(providers = emptyList())
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Should handle empty providers gracefully
        val bootstrapped = manager.ensureBootstrap()
        assertTrue(bootstrapped) // Should succeed even with no providers
        
        // Flags should return defaults
        assertFalse(manager.isEnabled("nonexistent", default = false))
    }
    
    @Test
    fun testNonExistentFlag() = runTest {
        val provider = TestProvider(flags = emptyMap())
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Should return default value
        assertFalse(manager.isEnabled("nonexistent", default = false))
        assertTrue(manager.isEnabled("nonexistent", default = true))
        assertEquals(42, manager.value("nonexistent_int", default = 42))
        assertEquals("default", manager.value("nonexistent_string", default = "default"))
    }
    
    @Test
    fun testEmptyFlagKey() = runTest {
        val provider = TestProvider(
            flags = mapOf("" to FlagValue.Bool(true))
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Empty key should be handled
        assertTrue(manager.isEnabled("", default = false))
    }
    
    @Test
    fun testSpecialCharactersInFlagKey() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "flag_with_underscore" to FlagValue.Bool(true),
                "flag-with-dash" to FlagValue.Bool(true),
                "flag.with.dot" to FlagValue.Bool(true)
            )
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("flag_with_underscore"))
        assertTrue(manager.isEnabled("flag-with-dash"))
        assertTrue(manager.isEnabled("flag.with.dot"))
    }
    
    @Test
    fun testVeryLongFlagKey() = runTest {
        val longKey = "a".repeat(1000)
        val provider = TestProvider(
            flags = mapOf(longKey to FlagValue.Bool(true))
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled(longKey))
    }
    
    @Test
    fun testNullDefaultValues() = runTest {
        val provider = TestProvider(flags = emptyMap())
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Should handle defaults gracefully
        // For non-existent flags, should return provided default
        val value = manager.value("nonexistent", default = "default_value")
        assertEquals("default_value", value)
    }
    
    @Test
    fun testMultipleBootstrapCalls() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Multiple bootstrap calls should be idempotent
        val bootstrapped1 = manager.ensureBootstrap()
        val bootstrapped2 = manager.ensureBootstrap()
        val bootstrapped3 = manager.ensureBootstrap()
        
        assertTrue(bootstrapped1)
        assertTrue(bootstrapped2)
        assertTrue(bootstrapped3)
        assertTrue(manager.isEnabled("test_flag"))
    }
    
    @Test
    fun testConcurrentFlagAccess() = runTest {
        val provider = TestProvider(
            flags = mapOf("concurrent_flag" to FlagValue.Bool(true))
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Concurrent access should be safe
        val results = coroutineScope {
            (1..100).map { i ->
                async {
                    manager.isEnabled("concurrent_flag", false)
                }
            }.map { it.await() }
        }
        
        // All should return true
        assertTrue(results.all { it })
    }
    
    @Test
    fun testExperimentWithNoVariants() = runTest {
        // This tests edge case - experiment with no variants
        // Should be handled gracefully
        val provider = TestProvider()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Non-existent experiment should return null
        val assignment = manager.assign("nonexistent_experiment")
        assertNull(assignment)
    }
    
    @Test
    fun testFlagValueTypeMismatch() = runTest {
        val provider = TestProvider(
            flags = mapOf("int_flag" to FlagValue.Int(42))
        )
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Trying to get int flag as bool should return default
        assertFalse(manager.isEnabled("int_flag", default = false))
        
        // But should work as int
        assertEquals(42, manager.value("int_flag", default = 0))
    }
}

