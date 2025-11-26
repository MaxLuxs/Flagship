package io.maxluxs.flagship.integration
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
import kotlin.test.assertTrue

/**
 * Tests for provider precedence - first provider should take precedence
 */
class ProviderPrecedenceTest {
    
    @BeforeTest
    fun setup() {
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testFirstProviderTakesPrecedence() = runTest {
        val provider1 = TestProvider(
            flags = mapOf(
                "flag1" to FlagValue.Bool(true),
                "shared_flag" to FlagValue.Bool(true)
            ),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf(
                "flag2" to FlagValue.Bool(false),
                "shared_flag" to FlagValue.Bool(false)
            ),
            name = "provider2"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // First provider should win for shared flag
        assertTrue(manager.isEnabled("shared_flag"))
        assertTrue(manager.isEnabled("flag1"))
        assertFalse(manager.isEnabled("flag2"))
    }
    
    @Test
    fun testProviderOrderMatters() = runTest {
        val provider1 = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(false)),
            name = "provider2"
        )
        
        // Test order 1 -> 2
        val config1 = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        Flagship.configure(config1)
        val manager1 = Flagship.manager() as DefaultFlagsManager
        manager1.setDefaultContext(TestHelpers.createTestContext())
        manager1.ensureBootstrap()
        assertTrue(manager1.isEnabled("test_flag"))
        
        Flagship.reset()
        
        // Test order 2 -> 1
        val config2 = TestFlagsConfig.createTestConfig(
            providers = listOf(provider2, provider1)
        )
        Flagship.configure(config2)
        val manager2 = Flagship.manager() as DefaultFlagsManager
        manager2.setDefaultContext(TestHelpers.createTestContext())
        manager2.ensureBootstrap()
        assertFalse(manager2.isEnabled("test_flag"))
    }
    
    @Test
    fun testMultipleProvidersWithDifferentFlags() = runTest {
        val provider1 = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf("flag2" to FlagValue.Bool(true)),
            name = "provider2"
        )
        val provider3 = TestProvider(
            flags = mapOf("flag3" to FlagValue.Bool(true)),
            name = "provider3"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2, provider3)
        )
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("flag1"))
        assertTrue(manager.isEnabled("flag2"))
        assertTrue(manager.isEnabled("flag3"))
    }
    
    @Test
    fun testProviderFallbackWhenFirstProviderMissing() = runTest {
        val provider1 = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf(
                "flag1" to FlagValue.Bool(false),
                "flag2" to FlagValue.Bool(true)
            ),
            name = "provider2"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // First provider has flag1, so it should be used
        assertTrue(manager.isEnabled("flag1"))
        // flag2 only in second provider
        assertTrue(manager.isEnabled("flag2"))
    }
}

