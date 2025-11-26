package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
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

class FlagshipResultTest {
    
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
            key: FlagKey,
            context: EvalContext
        ): FlagValue? {
            return when (key) {
                "test_flag" -> FlagValue.Bool(true)
                "int_flag" -> FlagValue.Int(42)
                "string_flag" -> FlagValue.StringV("hello")
                else -> null
            }
        }
        
        override fun evaluateExperiment(
            key: ExperimentKey,
            context: EvalContext
        ): ExperimentAssignment? {
            return null
        }
        
        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
    
    @Test
    fun testValueOrErrorSuccess() = runTest {
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
        
        val result = Flagship.valueOrError("test_flag", default = false)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        
        val intResult = Flagship.valueOrError("int_flag", default = 0)
        assertTrue(intResult.isSuccess)
        assertEquals(42, intResult.getOrNull())
        
        val stringResult = Flagship.valueOrError("string_flag", default = "")
        assertTrue(stringResult.isSuccess)
        assertEquals("hello", stringResult.getOrNull())
    }
    
    @Test
    fun testValueOrErrorWithDefault() = runTest {
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
        
        // Non-existent flag should return default value successfully
        val result = Flagship.valueOrError("nonexistent", default = 100)
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
    }
    
    @Test
    fun testIsEnabledOrErrorSuccess() = runTest {
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
        
        val result = Flagship.isEnabledOrError("test_flag", default = false)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        
        // Non-existent flag should return default
        val defaultResult = Flagship.isEnabledOrError("nonexistent", default = false)
        assertTrue(defaultResult.isSuccess)
        assertEquals(false, defaultResult.getOrNull())
    }
    
    @Test
    fun testAssignOrErrorSuccess() = runTest {
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
        
        // Experiment doesn't exist, should return null successfully
        val result = Flagship.assignOrError("test_experiment", context = context)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun testAssignOrErrorWithNonExistentExperiment() = runTest {
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
        
        // Non-existent experiment should return null successfully
        val result = Flagship.assignOrError("nonexistent_experiment", context = context)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun testResultOnSuccessAndOnFailure() = runTest {
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
        
        var successCalled = false
        var failureCalled = false
        
        val result = Flagship.valueOrError("test_flag", default = false)
        result.onSuccess { value ->
            successCalled = true
            assertEquals(true, value)
        }.onFailure { error ->
            failureCalled = true
        }
        
        assertTrue(successCalled)
        assertFalse(failureCalled)
    }
    
    @Test
    fun testResultGetOrElse() = runTest {
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
        
        val result = Flagship.valueOrError("test_flag", default = false)
        val value = result.getOrElse { 
            throw AssertionError("Should not fail")
        }
        assertEquals(true, value)
    }
}
