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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BatchEvaluationTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf(
                    "flag1" to FlagValue.Bool(true),
                    "flag2" to FlagValue.Int(42),
                    "flag3" to FlagValue.StringV("hello"),
                    "flag4" to FlagValue.Double(3.14)
                ),
                experiments = mapOf(
                    "exp1" to io.maxluxs.flagship.core.model.ExperimentDefinition(
                        key = "exp1",
                        variants = listOf(
                            io.maxluxs.flagship.core.model.Variant("control", 0.5),
                            io.maxluxs.flagship.core.model.Variant("treatment", 0.5)
                        )
                    )
                ),
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
                "flag4" -> FlagValue.Double(3.14)
                else -> null
            }
        }
        
        override fun evaluateExperiment(
            key: io.maxluxs.flagship.core.model.ExperimentKey,
            context: io.maxluxs.flagship.core.model.EvalContext
        ): io.maxluxs.flagship.core.model.ExperimentAssignment? {
            return when (key) {
                "exp1" -> io.maxluxs.flagship.core.model.ExperimentAssignment(
                    key = "exp1",
                    variant = "control",
                    payload = emptyMap()
                )
                else -> null
            }
        }
        
        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
    
    @Test
    fun testEvaluateFlags() = runTest {
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
        
        val defaults = mapOf(
            "flag1" to FlagValue.Bool(false),
            "flag2" to FlagValue.Int(0),
            "flag3" to FlagValue.StringV(""),
            "flag4" to FlagValue.Double(0.0),
            "nonexistent" to FlagValue.Bool(false)
        )
        
        val results = Flagship.evaluateFlags(
            listOf("flag1", "flag2", "flag3", "flag4", "nonexistent"),
            defaults
        )
        
        assertEquals(5, results.size)
        assertEquals(FlagValue.Bool(true), results["flag1"])
        assertEquals(FlagValue.Int(42), results["flag2"])
        assertEquals(FlagValue.StringV("hello"), results["flag3"])
        assertEquals(FlagValue.Double(3.14), results["flag4"])
        assertEquals(FlagValue.Bool(false), results["nonexistent"])
    }
    
    @Test
    fun testEvaluateExperiments() = runTest {
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
        
        val results = Flagship.evaluateExperiments(
            listOf("exp1", "nonexistent_exp"),
            context = context
        )
        
        assertEquals(2, results.size)
        assertNotNull(results["exp1"])
        assertEquals("exp1", results["exp1"]?.key)
        assertEquals(null, results["nonexistent_exp"])
    }
    
    @Test
    fun testEvaluateFlagsWithOverrides() = runTest {
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
        Flagship.setOverride("flag1", FlagValue.Bool(false))
        
        val defaults = mapOf(
            "flag1" to FlagValue.Bool(true)
        )
        
        val results = Flagship.evaluateFlags(
            listOf("flag1"),
            defaults
        )
        
        // Override should take precedence
        assertEquals(FlagValue.Bool(false), results["flag1"])
    }
}

