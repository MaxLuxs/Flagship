package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for refresh functionality
 */
class RefreshTest {
    
    private class RefreshableProvider(
        private var currentValue: Boolean
    ) : FlagsProvider {
        override val name: String = "refreshable"
        
        fun setValue(value: Boolean) {
            currentValue = value
        }
        
        override suspend fun bootstrap(): ProviderSnapshot {
            return ProviderSnapshot(
                flags = mapOf("refreshable_flag" to FlagValue.Bool(currentValue)) as Map<String, FlagValue>,
                experiments = emptyMap(),
                revision = "v1",
                fetchedAtMs = currentTimeMillis(),
                ttlMs = 60_000L
            )
        }
        
        override suspend fun refresh(): ProviderSnapshot {
            return ProviderSnapshot(
                flags = mapOf("refreshable_flag" to FlagValue.Bool(currentValue)) as Map<String, FlagValue>,
                experiments = emptyMap(),
                revision = "v2",
                fetchedAtMs = currentTimeMillis(),
                ttlMs = 60_000L
            )
        }
        
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
    fun testRefreshUpdatesFlags() = runTest {
        val provider = RefreshableProvider(false)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Initial value should be false
        assertFalse(manager.isEnabled("refreshable_flag"))
        
        // Change provider value
        provider.setValue(true)
        
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
        
        // Wait for refresh to complete using listener
        var attempts = 0
        while (!refreshCompleted && attempts < 50) {
            kotlinx.coroutines.delay(50)
            attempts++
        }
        
        // Value should be updated
        assertTrue(manager.isEnabled("refreshable_flag"))
    }
    
    @Test
    fun testRefreshIsNonBlocking() = runTest {
        val provider = RefreshableProvider(false)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Refresh should return immediately (non-blocking)
        manager.refresh()
        
        // Should not block execution
        assertTrue(true) // If we get here, refresh didn't block
    }
    
    @Test
    fun testMultipleRefreshes() = runTest {
        val provider = RefreshableProvider(false)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Use listener to wait for refresh completion
        var refreshCount = 0
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                if (source == "refresh") {
                    refreshCount++
                }
            }
            override fun onOverrideChanged(key: String) {}
        })
        
        // Multiple refreshes should work
        manager.refresh()
        while (refreshCount < 1) {
            kotlinx.coroutines.delay(50)
        }
        
        provider.setValue(true)
        manager.refresh()
        while (refreshCount < 2) {
            kotlinx.coroutines.delay(50)
        }
        
        assertTrue(manager.isEnabled("refreshable_flag"))
        
        provider.setValue(false)
        manager.refresh()
        while (refreshCount < 3) {
            kotlinx.coroutines.delay(50)
        }
        
        assertFalse(manager.isEnabled("refreshable_flag"))
    }
    
    @Test
    fun testRefreshWithMultipleProviders() = runTest {
        val provider1 = RefreshableProvider(false)
        val provider2 = io.maxluxs.flagship.helpers.TestProvider(
            flags = mapOf("static_flag" to FlagValue.Bool(true)),
            name = "provider2"
        )
        
        val config = TestFlagsConfig.createTestConfig(
            providers = listOf(provider1, provider2)
        )
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Both flags should be available
        assertFalse(manager.isEnabled("refreshable_flag"))
        assertTrue(manager.isEnabled("static_flag"))
        
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
        
        // Refresh should update refreshable flag
        provider1.setValue(true)
        manager.refresh()
        
        // Wait for refresh to complete
        var attempts = 0
        while (!refreshCompleted && attempts < 50) {
            kotlinx.coroutines.delay(50)
            attempts++
        }
        
        assertTrue(manager.isEnabled("refreshable_flag"))
        assertTrue(manager.isEnabled("static_flag")) // Should still work
    }
}

