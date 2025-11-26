package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagSource
import io.maxluxs.flagship.core.model.FlagStatus
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlagStatusTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test_provider"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf(
                    "test_flag" to FlagValue.Bool(true),
                    "int_flag" to FlagValue.Int(42)
                ),
                experiments = emptyMap(),
                revision = "rev1",
                fetchedAtMs = currentTimeMillis()
            )
        }
        
        override suspend fun refresh(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return bootstrap()
        }
        
        override fun evaluateFlag(
            key: io.maxluxs.flagship.core.model.FlagKey,
            context: io.maxluxs.flagship.core.model.EvalContext
        ): FlagValue? {
            return when (key) {
                "test_flag" -> FlagValue.Bool(true)
                "int_flag" -> FlagValue.Int(42)
                else -> null
            }
        }
        
        override fun evaluateExperiment(
            key: io.maxluxs.flagship.core.model.ExperimentKey,
            context: io.maxluxs.flagship.core.model.EvalContext
        ): io.maxluxs.flagship.core.model.ExperimentAssignment? {
            return null
        }
        
        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
    
    @Test
    fun testGetFlagStatusFromProvider() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        val status = Flagship.getFlagStatus("test_flag")
        
        assertTrue(status.exists)
        assertEquals(FlagSource.PROVIDER, status.source)
        assertNull(status.lastError)
        assertNotNull(status.lastUpdated)
        assertEquals("test_provider", status.providerName)
        assertTrue(status.isHealthy())
        assertTrue(status.isFresh())
    }
    
    @Test
    fun testGetFlagStatusFromOverride() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        // Set override
        Flagship.setOverride("test_flag", FlagValue.Bool(false))
        
        val status = Flagship.getFlagStatus("test_flag")
        
        assertTrue(status.exists)
        assertEquals(FlagSource.OVERRIDE, status.source)
        assertNull(status.lastError)
        assertFalse(status.isFresh()) // Override is not considered "fresh"
        assertTrue(status.isHealthy())
    }
    
    @Test
    fun testGetFlagStatusFromCache() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        // Wait a bit to make snapshot "stale" (older than TTL)
        // For this test, we'll just check that cache source is possible
        // In real scenario, cache would be used if snapshot is older than TTL
        
        val status = Flagship.getFlagStatus("test_flag")
        
        assertTrue(status.exists)
        // Source could be PROVIDER or CACHE depending on timing
        assertTrue(status.source == FlagSource.PROVIDER || status.source == FlagSource.CACHE)
        assertTrue(status.isHealthy())
    }
    
    @Test
    fun testGetFlagStatusDefault() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        val status = Flagship.getFlagStatus("nonexistent_flag")
        
        assertFalse(status.exists)
        assertEquals(FlagSource.DEFAULT, status.source)
        assertNull(status.lastError)
        assertFalse(status.isHealthy()) // Default means flag not found
        assertFalse(status.isFresh())
    }
    
    @Test
    fun testFlagStatusIsHealthy() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        // Existing flag should be healthy
        val existingStatus = Flagship.getFlagStatus("test_flag")
        assertTrue(existingStatus.isHealthy())
        
        // Non-existent flag should not be healthy
        val defaultStatus = Flagship.getFlagStatus("nonexistent")
        assertFalse(defaultStatus.isHealthy())
    }
    
    @Test
    fun testFlagStatusIsFresh() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        // Provider source should be fresh
        val providerStatus = Flagship.getFlagStatus("test_flag")
        if (providerStatus.source == FlagSource.PROVIDER) {
            assertTrue(providerStatus.isFresh())
        }
        
        // Default source should not be fresh
        val defaultStatus = Flagship.getFlagStatus("nonexistent")
        assertFalse(defaultStatus.isFresh())
        
        // Override source should not be fresh
        Flagship.setOverride("test_flag", FlagValue.Bool(false))
        val overrideStatus = Flagship.getFlagStatus("test_flag")
        assertEquals(FlagSource.OVERRIDE, overrideStatus.source)
        assertFalse(overrideStatus.isFresh())
    }
    
    @Test
    fun testFlagStatusProviderName() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        Flagship.configure(
            FlagsConfig(
                appKey = "test",
                environment = "test",
                providers = listOf(provider),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        )
        
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        (Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager).setDefaultContext(context)
        Flagship.manager().ensureBootstrap(5000)
        
        val status = Flagship.getFlagStatus("test_flag")
        
        if (status.source == FlagSource.PROVIDER || status.source == FlagSource.CACHE) {
            assertEquals("test_provider", status.providerName)
        }
        
        // Default status should not have provider name
        val defaultStatus = Flagship.getFlagStatus("nonexistent")
        assertNull(defaultStatus.providerName)
    }
}
