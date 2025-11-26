package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.errors.*
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FlagsManagerEdgeCasesTest {
    private val context = EvalContext(
        userId = "test_user",
        deviceId = "test_device",
        appVersion = "1.0.0",
        osName = "Test",
        osVersion = "1.0"
    )

    @Test
    fun testEmptyFlagKey() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Empty key should return default
        assertEquals(false, manager.isEnabled("", false))
        assertEquals(100, manager.value("", 100))
    }

    @Test
    fun testNullContext() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        // No default context set
        manager.ensureBootstrap(5000)

        // Should return default when context is null
        assertEquals(false, manager.isEnabled("test_flag", false, null))
        assertEquals(100, manager.value("test_flag", 100, null))
    }

    @Test
    fun testNonExistentFlag() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Non-existent flag should return default
        assertEquals(false, manager.isEnabled("nonexistent", false))
        assertEquals(true, manager.isEnabled("nonexistent", true))
        assertEquals(42, manager.value("nonexistent", 42))
        assertEquals("default", manager.value("nonexistent", "default"))
    }

    @Test
    fun testTypeMismatch() = runTest {
        val provider = TestProvider(flags = mapOf(
            "flag1" to FlagValue.Bool(true),
            "flag2" to FlagValue.Int(42),
            "flag3" to FlagValue.StringV("test")
        ))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Type mismatch should return default
        assertEquals(false, manager.isEnabled("flag2", false)) // flag2 is Int, not Bool
        assertEquals(0, manager.value("flag1", 0)) // flag1 is Bool, not Int
        assertEquals("", manager.value("flag1", "")) // flag1 is Bool, not String
    }

    @Test
    fun testEmptyProvidersList() = runTest {
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = emptyList(),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        val bootstrapped = manager.ensureBootstrap(5000)

        // Should bootstrap successfully even with no providers
        assertEquals(true, bootstrapped)

        // Should return defaults
        assertEquals(false, manager.isEnabled("any_flag", false))
        assertEquals(100, manager.value("any_flag", 100))
    }

    @Test
    fun testProviderWithEmptySnapshot() = runTest {
        val provider = TestProvider(flags = emptyMap(), experiments = emptyMap())
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should return defaults
        assertEquals(false, manager.isEnabled("any_flag", false))
        assertEquals(100, manager.value("any_flag", 100))
        assertNull(manager.assign("any_experiment"))
    }

    @Test
    fun testVeryLongFlagKey() = runTest {
        val longKey = "a".repeat(1000)
        val provider = TestProvider(flags = mapOf(longKey to FlagValue.Bool(true)))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should handle long keys
        assertEquals(true, manager.isEnabled(longKey, false))
    }

    @Test
    fun testSpecialCharactersInFlagKey() = runTest {
        val specialKey = "flag_with-special.chars_123"
        val provider = TestProvider(flags = mapOf(specialKey to FlagValue.Bool(true)))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should handle special characters
        assertEquals(true, manager.isEnabled(specialKey, false))
    }

    @Test
    fun testExtremeValues() = runTest {
        val provider = TestProvider(flags = mapOf(
            "int_max" to FlagValue.Int(Int.MAX_VALUE),
            "int_min" to FlagValue.Int(Int.MIN_VALUE),
            "double_max" to FlagValue.Double(Double.MAX_VALUE),
            "double_min" to FlagValue.Double(Double.MIN_VALUE),
            "long_string" to FlagValue.StringV("x".repeat(10000))
        ))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should handle extreme values
        assertEquals(Int.MAX_VALUE, manager.value("int_max", 0))
        assertEquals(Int.MIN_VALUE, manager.value("int_min", 0))
        assertEquals(Double.MAX_VALUE, manager.value("double_max", 0.0))
        assertEquals(Double.MIN_VALUE, manager.value("double_min", 0.0))
        assertEquals("x".repeat(10000), manager.value("long_string", ""))
    }

    @Test
    fun testBootstrapTimeout() = runTest {
        val slowProvider = object : FlagsProvider {
            override val name: String = "slow"
            override suspend fun bootstrap(): ProviderSnapshot {
                kotlinx.coroutines.delay(10000) // 10 seconds
                return ProviderSnapshot(
                    flags = mapOf("flag1" to FlagValue.Bool(true)),
                    experiments = emptyMap(),
                    revision = "rev1",
                    fetchedAtMs = currentTimeMillis()
                )
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = true
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 0
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(slowProvider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should timeout
        val bootstrapped = manager.ensureBootstrap(100) // 100ms timeout
        assertEquals(false, bootstrapped)
    }

    @Test
    fun testMultipleOverrides() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Set multiple overrides
        manager.setOverride("flag1", FlagValue.Bool(true))
        manager.setOverride("flag2", FlagValue.Int(42))
        manager.setOverride("flag3", FlagValue.StringV("test"))

        val overrides = manager.listOverrides()
        assertEquals(3, overrides.size)
        assertEquals(true, overrides["flag1"]?.asBoolean())
        assertEquals(42, overrides["flag2"]?.asInt())
        assertEquals("test", overrides["flag3"]?.asString())

        // Clear one override
        manager.clearOverride("flag1")
        val remainingOverrides = manager.listOverrides()
        assertEquals(2, remainingOverrides.size)
        assertEquals(false, remainingOverrides.containsKey("flag1"))
    }

    @Test
    fun testOverrideOverridesProvider() = runTest {
        val provider = TestProvider(flags = mapOf("flag1" to FlagValue.Bool(false)))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Provider value
        assertEquals(false, manager.isEnabled("flag1", true))

        // Override should take precedence
        manager.setOverride("flag1", FlagValue.Bool(true))
        assertEquals(true, manager.isEnabled("flag1", false))

        // Clear override, should return to provider value
        manager.clearOverride("flag1")
        assertEquals(false, manager.isEnabled("flag1", true))
    }

    @Test
    fun testExperimentWithZeroWeights() = runTest {
        val experiment = ExperimentDefinition(
            key = "test_exp",
            variants = listOf(
                Variant("control", 0.0),
                Variant("treatment", 0.0)
            )
        )

        val provider = TestProvider(experiments = mapOf("test_exp" to experiment))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should return null when all weights are zero
        assertNull(manager.assign("test_exp"))
    }

    @Test
    fun testExperimentWithSingleVariant() = runTest {
        val experiment = ExperimentDefinition(
            key = "test_exp",
            variants = listOf(
                Variant("control", 1.0)
            )
        )

        val provider = TestProvider(experiments = mapOf("test_exp" to experiment))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Should always return the single variant
        val assignment = manager.assign("test_exp")
        assertNotNull(assignment)
        assertEquals("control", assignment.variant)
    }

    private class TestProvider(
        override val name: String = "test",
        private val flags: Map<String, FlagValue> = mapOf("test_flag" to FlagValue.Bool(true)),
        private val experiments: Map<String, ExperimentDefinition> = emptyMap()
    ) : FlagsProvider {
        override suspend fun bootstrap(): ProviderSnapshot {
            return ProviderSnapshot(
                flags = flags,
                experiments = experiments,
                revision = "test_rev",
                fetchedAtMs = currentTimeMillis()
            )
        }

        override suspend fun refresh(): ProviderSnapshot {
            return bootstrap()
        }

        override fun evaluateFlag(key: String, context: EvalContext): FlagValue? {
            return flags[key]
        }

        override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? {
            return null // Handled by manager
        }

        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
}

