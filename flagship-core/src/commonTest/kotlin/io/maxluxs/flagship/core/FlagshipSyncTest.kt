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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlagshipSyncTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf(
                    "test_flag" to FlagValue.Bool(true),
                    "int_flag" to FlagValue.Int(42),
                    "string_flag" to FlagValue.StringV("hello")
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
                "string_flag" -> FlagValue.StringV("hello")
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
        override fun getLastSuccessfulFetchMs(): Long? = System.currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
    
    @Test
    fun testSyncMethodsRequireBootstrap() = runTest {
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
        
        // Should fail before bootstrap
        assertFailsWith<IllegalStateException> {
            Flagship.isEnabledSync("test_flag", false)
        }
        
        assertFailsWith<IllegalStateException> {
            Flagship.valueSync("test_flag", false)
        }
        
        assertFailsWith<IllegalStateException> {
            Flagship.getSync("test_flag", false)
        }
        
        // Bootstrap
        Flagship.manager().ensureBootstrap(5000)
        
        // Should work after bootstrap
        assertTrue(Flagship.isEnabledSync("test_flag", false))
        assertEquals(true, Flagship.valueSync("test_flag", false))
        assertEquals(true, Flagship.getSync("test_flag", false))
    }
    
    @Test
    fun testSyncMethodsWorkAfterBootstrap() = runTest {
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
        
        // Test sync methods
        assertTrue(Flagship.isEnabledSync("test_flag", false))
        assertEquals(true, Flagship.valueSync("test_flag", false))
        assertEquals(42, Flagship.valueSync("int_flag", 0))
        assertEquals("hello", Flagship.valueSync("string_flag", ""))
        
        // Test defaults
        assertEquals(false, Flagship.isEnabledSync("nonexistent", false))
        assertEquals(100, Flagship.valueSync("nonexistent", 100))
        assertEquals("default", Flagship.valueSync("nonexistent", "default"))
    }
    
    @Test
    fun testGetSyncIsAliasForValueSync() = runTest {
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
        
        // getSync should work the same as valueSync
        assertEquals(42, Flagship.getSync("int_flag", 0))
        assertEquals("hello", Flagship.getSync("string_flag", ""))
        assertEquals(true, Flagship.getSync("test_flag", false))
    }
}

