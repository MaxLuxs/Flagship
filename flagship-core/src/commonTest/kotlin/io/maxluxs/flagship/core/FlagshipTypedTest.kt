package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlagshipTypedTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf(
                    "bool_flag" to FlagValue.Bool(true),
                    "int_flag" to FlagValue.Int(42),
                    "double_flag" to FlagValue.Double(3.14),
                    "string_flag" to FlagValue.StringV("hello"),
                    "json_flag" to FlagValue.Json(JsonPrimitive("test"))
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
                "bool_flag" -> FlagValue.Bool(true)
                "int_flag" -> FlagValue.Int(42)
                "double_flag" -> FlagValue.Double(3.14)
                "string_flag" -> FlagValue.StringV("hello")
                "json_flag" -> FlagValue.Json(JsonPrimitive("test"))
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
    fun testBoolValue() = runTest {
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
        
        assertEquals(true, Flagship.boolValue("bool_flag", default = false))
        assertEquals(false, Flagship.boolValue("nonexistent", default = false))
    }
    
    @Test
    fun testBoolValueSync() = runTest {
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
        
        assertEquals(true, Flagship.boolValueSync("bool_flag", default = false))
        assertEquals(false, Flagship.boolValueSync("nonexistent", default = false))
    }
    
    @Test
    fun testIntValue() = runTest {
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
        
        assertEquals(42, Flagship.intValue("int_flag", default = 0))
        assertEquals(100, Flagship.intValue("nonexistent", default = 100))
    }
    
    @Test
    fun testIntValueSync() = runTest {
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
        
        assertEquals(42, Flagship.intValueSync("int_flag", default = 0))
        assertEquals(100, Flagship.intValueSync("nonexistent", default = 100))
    }
    
    @Test
    fun testDoubleValue() = runTest {
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
        
        assertEquals(3.14, Flagship.doubleValue("double_flag", default = 0.0))
        assertEquals(0.75, Flagship.doubleValue("nonexistent", default = 0.75))
    }
    
    @Test
    fun testDoubleValueSync() = runTest {
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
        
        assertEquals(3.14, Flagship.doubleValueSync("double_flag", default = 0.0))
        assertEquals(0.75, Flagship.doubleValueSync("nonexistent", default = 0.75))
    }
    
    @Test
    fun testStringValue() = runTest {
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
        
        assertEquals("hello", Flagship.stringValue("string_flag", default = ""))
        assertEquals("default", Flagship.stringValue("nonexistent", default = "default"))
    }
    
    @Test
    fun testStringValueSync() = runTest {
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
        
        assertEquals("hello", Flagship.stringValueSync("string_flag", default = ""))
        assertEquals("default", Flagship.stringValueSync("nonexistent", default = "default"))
    }
    
    @Test
    fun testJsonValue() = runTest {
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
        
        val jsonValue = Flagship.jsonValue("json_flag", default = JsonNull)
        assertTrue(jsonValue is JsonPrimitive)
        assertEquals("test", (jsonValue as JsonPrimitive).content)
        
        val defaultJson = Flagship.jsonValue("nonexistent", default = JsonNull)
        assertTrue(defaultJson is JsonNull)
    }
    
    @Test
    fun testJsonValueSync() = runTest {
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
        
        val jsonValue = Flagship.jsonValueSync("json_flag", default = JsonNull)
        assertTrue(jsonValue is JsonPrimitive)
        assertEquals("test", (jsonValue as JsonPrimitive).content)
        
        val defaultJson = Flagship.jsonValueSync("nonexistent", default = JsonNull)
        assertTrue(defaultJson is JsonNull)
    }
}
