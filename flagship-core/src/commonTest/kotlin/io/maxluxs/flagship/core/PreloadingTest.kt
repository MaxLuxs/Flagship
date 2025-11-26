package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreloadingTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf(
                    "flag1" to FlagValue.Bool(true),
                    "flag2" to FlagValue.Int(42),
                    "flag3" to FlagValue.StringV("hello")
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
                "flag1" -> FlagValue.Bool(true)
                "flag2" -> FlagValue.Int(42)
                "flag3" -> FlagValue.StringV("hello")
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
    fun testPreload() = runTest {
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
        
        // Preload flags
        Flagship.preload(listOf("flag1", "flag2", "flag3"))
        
        // Verify flags are accessible
        assertTrue(Flagship.isEnabledSync("flag1", false))
        assertEquals(42, Flagship.intValueSync("flag2", 0))
        assertEquals("hello", Flagship.stringValueSync("flag3", ""))
    }
    
    @Test
    fun testPreloadForUser() = runTest {
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
        
        // Preload flags for specific user
        Flagship.preloadForUser("user123", listOf("flag1", "flag2", "flag3"))
        
        // Verify flags are accessible
        assertTrue(Flagship.isEnabledSync("flag1", false))
        assertEquals(42, Flagship.intValueSync("flag2", 0))
        assertEquals("hello", Flagship.stringValueSync("flag3", ""))
    }
    
    @Test
    fun testPreloadWithNonExistentFlags() = runTest {
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
        
        // Preload should not fail with non-existent flags
        Flagship.preload(listOf("nonexistent1", "nonexistent2"))
        
        // Non-existent flags should return defaults
        assertEquals(false, Flagship.isEnabledSync("nonexistent1", false))
        assertEquals(0, Flagship.intValueSync("nonexistent2", 0))
    }
}

