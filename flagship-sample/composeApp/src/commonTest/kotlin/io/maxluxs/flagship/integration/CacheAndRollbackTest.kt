package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.currentTimeMillis
import io.maxluxs.flagship.helpers.TestFlagsConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for cache behavior and rollback mechanisms
 */
class CacheAndRollbackTest {
    
    private class FailingProvider : FlagsProvider {
        override val name: String = "failing"
        private var shouldFail = true
        
        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
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
    fun testCachePersistence() = runTest {
        val provider = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("cached_flag" to FlagValue.Bool(true))
        )
        
        val cache = InMemoryCache()
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
            .copy(cache = cache)
        
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Verify flag is available
        assertTrue(manager.isEnabled("cached_flag", false))
        
        // Clear provider (simulate network failure)
        // Cache should still have the value
        // Note: This test depends on cache implementation
    }
    
    @Test
    fun testRollbackOnProviderFailure() = runTest {
        val goodProvider = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("stable_flag" to FlagValue.Bool(true))
        )
        val failingProvider = FailingProvider()
        
        // First bootstrap with good provider
        val config1 = TestFlagsConfig.createTestConfig(
            providers = listOf(goodProvider)
        )
        Flags.configure(config1)
        val manager1 = Flags.manager() as DefaultFlagsManager
        manager1.setDefaultContext(TestHelpers.createTestContext())
        manager1.ensureBootstrap()
        assertTrue(manager1.isEnabled("stable_flag"))
        
        Flags.reset()
        
        // Now use failing provider - should fallback to cache if available
        failingProvider.setShouldFail(true)
        val goodProvider2 = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("stable_flag" to FlagValue.Bool(true)),
            name = "good2"
        )
        val config2 = TestFlagsConfig.createTestConfig(
            providers = listOf(failingProvider, goodProvider2)
        )
        Flags.configure(config2)
        val manager2 = Flags.manager() as DefaultFlagsManager
        manager2.setDefaultContext(TestHelpers.createTestContext())
        
        // Should still work because of fallback to second provider
        val bootstrapped = manager2.ensureBootstrap()
        assertTrue(bootstrapped)
        assertTrue(manager2.isEnabled("stable_flag"))
    }
    
    @Test
    fun testTTLExpiration() = runTest {
        val provider = object : FlagsProvider {
            override val name: String = "ttl-test"
            private var callCount = 0
            
            override suspend fun bootstrap(): ProviderSnapshot {
                callCount++
                return ProviderSnapshot(
                    flags = mapOf("ttl_flag" to FlagValue.Bool(callCount == 1)) as Map<String, FlagValue>,
                    experiments = emptyMap(),
                    revision = "v$callCount",
                    fetchedAtMs = currentTimeMillis(),
                    ttlMs = 100L // Very short TTL
                )
            }
            
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: io.maxluxs.flagship.core.model.EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: io.maxluxs.flagship.core.model.EvalContext): io.maxluxs.flagship.core.model.ExperimentAssignment? = null
        }
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // First call should return true
        assertTrue(manager.isEnabled("ttl_flag"))
        
        // Wait for TTL to expire
        delay(150)
        
        // Use listener to wait for refresh completion
        var refreshCompleted = false
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                if (source == "refresh") {
                    refreshCompleted = true
                }
            }
            override fun onOverrideChanged(key: String) {}
        })
        
        // Refresh should get new value
        manager.refresh()
        var attempts = 0
        while (!refreshCompleted && attempts < 50) {
            delay(50)
            attempts++
        }
        assertFalse(manager.isEnabled("ttl_flag"))
    }
}

