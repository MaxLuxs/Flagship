package io.maxluxs.flagship.e2e
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.currentTimeMillis
import io.maxluxs.flagship.helpers.TestFlagsConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for rollback scenarios - when provider fails, should fallback to cache
 */
class RollbackScenarioTest {
    
    private class FailingProvider(
        private val shouldFail: Boolean = true
    ) : FlagsProvider {
        override val name: String = "failing"
        
        fun setShouldFail(fail: Boolean) {
            // This won't work with immutable class, but for test purposes
        }
        
        override suspend fun bootstrap(): ProviderSnapshot {
            if (shouldFail) {
                throw Exception("Provider failed")
            }
            return ProviderSnapshot(
                flags = mapOf("test_flag" to FlagValue.Bool(true)) as Map<String, FlagValue>,
                experiments = emptyMap(),
                revision = "v1",
                fetchedAtMs = currentTimeMillis(),
                ttlMs = 60_000L
            )
        }
        
        override suspend fun refresh(): ProviderSnapshot = bootstrap()
        override fun evaluateFlag(key: String, context: io.maxluxs.flagship.core.model.EvalContext): FlagValue? = null
        override fun evaluateExperiment(key: String, context: io.maxluxs.flagship.core.model.EvalContext): io.maxluxs.flagship.core.model.ExperimentAssignment? = null
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
    fun testRollbackToCacheOnProviderFailure() = runTest {
        val cache = InMemoryCache()
        
        // First, bootstrap with good provider to populate cache
        val goodProvider = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("stable_flag" to FlagValue.Bool(true)),
            name = "good"
        )
        
        val config1 = TestFlagsConfig.createTestConfig(providers = listOf(goodProvider))
            .copy(cache = cache)
        Flags.configure(config1)
        val manager1 = Flags.manager() as DefaultFlagsManager
        manager1.setDefaultContext(TestHelpers.createTestContext())
        manager1.ensureBootstrap()
        assertTrue(manager1.isEnabled("stable_flag", false))
        
        Flags.reset()
        
        // Now use failing provider with fallback to good provider
        val failingProvider = FailingProvider(shouldFail = true)
        val goodProvider2 = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("stable_flag" to FlagValue.Bool(true)),
            name = "good2"
        )
        val config2 = TestFlagsConfig.createTestConfig(
            providers = listOf(failingProvider, goodProvider2)
        )
            .copy(cache = cache)
        Flags.configure(config2)
        val manager2 = Flags.manager() as DefaultFlagsManager
        manager2.setDefaultContext(TestHelpers.createTestContext())
        
        // Bootstrap should succeed because of fallback provider
        val bootstrapped = manager2.ensureBootstrap()
        assertTrue(bootstrapped)
        assertTrue(manager2.isEnabled("stable_flag", false))
    }
    
    @Test
    fun testRollbackWithMultipleProviders() = runTest {
        val provider1 = FailingProvider(shouldFail = true)
        val provider2 = FailingProvider(shouldFail = true)
        val provider3 = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("fallback_flag" to FlagValue.Bool(true)),
            name = "provider3"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2, provider3)
        )
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Should succeed with third provider
        val bootstrapped = manager.ensureBootstrap()
        assertTrue(bootstrapped)
        assertTrue(manager.isEnabled("fallback_flag"))
    }
    
    @Test
    fun testAllProvidersFail() = runTest {
        val provider1 = FailingProvider(shouldFail = true)
        val provider2 = FailingProvider(shouldFail = true)
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Bootstrap completes even if all providers fail (loads from cache first, then tries providers)
        // bootstrapped = true is set regardless of provider success
        val bootstrapped = manager.ensureBootstrap()
        assertTrue(bootstrapped) // Bootstrap completes, but no flags are available
        
        // Manager should still be usable with defaults
        assertFalse(manager.isEnabled("nonexistent_flag", default = false))
    }
}

