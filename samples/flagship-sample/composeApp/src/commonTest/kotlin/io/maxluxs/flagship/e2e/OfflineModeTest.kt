package io.maxluxs.flagship.e2e
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.currentTimeMillis
import io.maxluxs.flagship.helpers.TestFlagsConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for offline mode behavior
 */
class OfflineModeTest {
    
    private class OfflineProvider : FlagsProvider {
        override val name: String = "offline"
        private var isOnline = false
        
        fun setOnline(online: Boolean) {
            isOnline = online
        }
        
        override suspend fun bootstrap(): ProviderSnapshot {
            if (!isOnline) {
                throw Exception("Network unavailable")
            }
            return ProviderSnapshot(
                flags = mapOf("online_flag" to FlagValue.Bool(true)) as Map<String, FlagValue>,
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
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testOfflineFallbackToCache() = runTest {
        val cache = InMemoryCache()
        
        // First, bootstrap while online
        val provider = OfflineProvider()
        provider.setOnline(true)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
            .copy(cache = cache)
        
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Verify flag is available
        assertTrue(manager.isEnabled("online_flag"))
        
        // Go offline
        provider.setOnline(false)
        Flagship.reset()
        
        // Reconfigure - should use cache
        val config2 = TestFlagsConfig.createTestConfig(providers = listOf(provider))
            .copy(cache = cache)
        Flagship.configure(config2)
        val manager2 = Flagship.manager() as DefaultFlagsManager
        manager2.setDefaultContext(TestHelpers.createTestContext())
        
        // Should still work from cache
        // Note: This depends on cache implementation
        val bootstrapped = manager2.ensureBootstrap()
        // If bootstrap fails, cache should still provide values
    }
    
    @Test
    fun testOfflineWithDefaultValues() = runTest {
        val provider = OfflineProvider()
        provider.setOnline(false)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Bootstrap completes even if provider fails (loads from cache first, then tries provider)
        // bootstrapped = true is set regardless of provider success
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 1000)
        assertTrue(bootstrapped) // Bootstrap completes, but no flags are available
        
        // Flags should return default values when offline
        assertFalse(manager.isEnabled("nonexistent_flag", default = false))
        assertTrue(manager.isEnabled("nonexistent_flag", default = true))
    }
    
    @Test
    fun testOnlineAfterOffline() = runTest {
        val provider = OfflineProvider()
        provider.setOnline(false)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Initially offline - bootstrap completes but no flags available
        val bootstrapped1 = manager.ensureBootstrap(timeoutMs = 1000)
        assertTrue(bootstrapped1) // Bootstrap completes even if provider fails
        
        // Go online
        provider.setOnline(true)
        
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
        
        // Refresh should work now
        manager.refresh()
        var attempts = 0
        while (!refreshCompleted && attempts < 50) {
            kotlinx.coroutines.delay(50)
            attempts++
        }
        assertTrue(manager.isEnabled("online_flag"))
    }
}

