package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for bootstrap flow and initialization
 */
class BootstrapFlowTest {
    
    @BeforeTest
    fun setup() {
        Flags.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testBootstrapSucceeds() = runTest {
        val provider = TestProvider(
            flags = mapOf("bootstrap_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        assertTrue(bootstrapped)
        assertTrue(manager.isEnabled("bootstrap_flag", false))
    }
    
    @Test
    fun testBootstrapTimeout() = runTest {
        val slowProvider = object : io.maxluxs.flagship.core.provider.FlagsProvider {
            override val name: String = "slow"
            
            override suspend fun bootstrap(): ProviderSnapshot {
                delay(2000) // Simulate slow network
                return ProviderSnapshot(
                    flags = emptyMap(),
                    experiments = emptyMap(),
                    revision = "v1",
                    fetchedAtMs = io.maxluxs.flagship.core.util.currentTimeMillis(),
                    ttlMs = 60_000L
                )
            }
            
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: io.maxluxs.flagship.core.model.EvalContext): io.maxluxs.flagship.core.model.FlagValue? = null
            override fun evaluateExperiment(key: String, context: io.maxluxs.flagship.core.model.EvalContext): io.maxluxs.flagship.core.model.ExperimentAssignment? = null
        }
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(slowProvider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Should timeout with short timeout
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 500)
        assertFalse(bootstrapped)
    }
    
    @Test
    fun testBootstrapWithMultipleProviders() = runTest {
        val provider1 = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf("flag2" to FlagValue.Bool(true)),
            name = "provider2"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        assertTrue(bootstrapped)
        assertTrue(manager.isEnabled("flag1", false))
        assertTrue(manager.isEnabled("flag2", false))
    }
    
    @Test
    fun testBootstrapIdempotent() = runTest {
        val provider = TestProvider(
            flags = mapOf("idempotent_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Call bootstrap multiple times
        val bootstrapped1 = manager.ensureBootstrap(timeoutMs = 5000)
        val bootstrapped2 = manager.ensureBootstrap(timeoutMs = 5000)
        val bootstrapped3 = manager.ensureBootstrap(timeoutMs = 5000)
        
        assertTrue(bootstrapped1)
        assertTrue(bootstrapped2)
        assertTrue(bootstrapped3)
        assertTrue(manager.isEnabled("idempotent_flag", false))
    }
    
    @Test
    fun testFlagsAvailableAfterBootstrap() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "bool_flag" to FlagValue.Bool(true),
                "int_flag" to FlagValue.Int(42),
                "string_flag" to FlagValue.StringV("test")
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        manager.ensureBootstrap(timeoutMs = 5000)
        
        assertTrue(manager.isEnabled("bool_flag", false))
        assertEquals(42, manager.value("int_flag", default = 0))
        assertEquals("test", manager.value("string_flag", default = ""))
    }
}

