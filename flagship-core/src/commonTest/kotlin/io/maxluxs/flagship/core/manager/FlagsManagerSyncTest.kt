package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlagsManagerSyncTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf("test_flag" to FlagValue.Bool(true)),
                experiments = emptyMap(),
                revision = "rev1",
                fetchedAtMs = System.currentTimeMillis()
            )
        }
        
        override suspend fun refresh(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return bootstrap()
        }
        
        override fun evaluateFlag(
            key: io.maxluxs.flagship.core.model.FlagKey,
            context: io.maxluxs.flagship.core.model.EvalContext
        ): FlagValue? {
            return if (key == "test_flag") FlagValue.Bool(true) else null
        }
        
        override fun evaluateExperiment(
            key: io.maxluxs.flagship.core.model.ExperimentKey,
            context: io.maxluxs.flagship.core.model.EvalContext
        ): io.maxluxs.flagship.core.model.ExperimentAssignment? {
            return null
        }
        
        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = System.currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
    
    @Test
    fun testSyncMethodsRequireBootstrap() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
        
        val manager = DefaultFlagsManager(config)
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        manager.setDefaultContext(context)
        
        // Should fail before bootstrap
        assertFailsWith<IllegalStateException> {
            manager.isEnabledSync("test_flag", false)
        }
        
        assertFailsWith<IllegalStateException> {
            manager.valueSync("test_flag", false)
        }
        
        // Bootstrap
        manager.ensureBootstrap(5000)
        
        // Should work after bootstrap
        assertTrue(manager.isEnabledSync("test_flag", false))
        assertEquals(true, manager.valueSync("test_flag", false))
    }
    
    @Test
    fun testSyncMethodsWorkAfterBootstrap() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
        
        val manager = DefaultFlagsManager(config)
        val context = EvalContext(
            userId = "user1",
            deviceId = "device1",
            appVersion = "1.0.0",
            osName = "Test",
            osVersion = "1.0"
        )
        manager.setDefaultContext(context)
        
        manager.ensureBootstrap(5000)
        
        // Test sync methods
        assertTrue(manager.isEnabledSync("test_flag", false))
        assertEquals(true, manager.valueSync("test_flag", false))
        assertEquals(false, manager.isEnabledSync("nonexistent", false))
        assertEquals(100, manager.valueSync("nonexistent", 100))
    }
}

