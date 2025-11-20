package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlagsManagerIntegrationTest {
    private val context = EvalContext(
        userId = "test_user",
        deviceId = "test_device",
        appVersion = "1.0.0",
        osName = "Test",
        osVersion = "1.0",
        locale = "en_US",
        region = "US"
    )

    @Test
    fun testBootstrapAndEvaluate() = runTest {
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

        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)

        val flagValue = manager.isEnabled("test_flag", false)
        assertTrue(flagValue)
    }

    @Test
    fun testOverrides() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Original value
        val originalValue = manager.isEnabled("test_flag", false)
        assertTrue(originalValue)

        // Set override
        manager.setOverride("test_flag", FlagValue.Bool(false))
        val overriddenValue = manager.isEnabled("test_flag", true)
        assertEquals(false, overriddenValue)

        // Clear override
        manager.clearOverride("test_flag")
        val clearedValue = manager.isEnabled("test_flag", false)
        assertTrue(clearedValue)
    }

    @Test
    fun testListeners() = runTest {
        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        var snapshotUpdated = false
        var overrideChanged = false

        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                snapshotUpdated = true
            }

            override fun onOverrideChanged(key: String) {
                overrideChanged = true
            }
        })

        manager.ensureBootstrap(5000)
        delay(100) // Give listeners time to fire
        assertTrue(snapshotUpdated)

        manager.setOverride("test_flag", FlagValue.Bool(false))
        assertTrue(overrideChanged)
    }

    @Test
    fun testMultipleProviderPrecedence() = runTest {
        val provider1 = TestProvider("provider1", mapOf("flag1" to FlagValue.Bool(true)))
        val provider2 = TestProvider("provider2", mapOf("flag1" to FlagValue.Bool(false), "flag2" to FlagValue.Bool(true)))
        
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider1, provider2),
            cache = InMemoryCache()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // flag1 should come from provider1 (first in list)
        assertTrue(manager.isEnabled("flag1", false))
        
        // flag2 should come from provider2 (provider1 doesn't have it)
        assertTrue(manager.isEnabled("flag2", false))
    }

    @Test
    fun testExperimentAssignment() = runTest {
        val experiment = ExperimentDefinition(
            key = "test_exp",
            variants = listOf(
                Variant("control", 0.5),
                Variant("treatment", 0.5)
            )
        )
        
        val provider = TestProvider(experiments = mapOf("test_exp" to experiment))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        val assignment = manager.assign("test_exp")
        assertNotNull(assignment)
        assertTrue(assignment.variant in setOf("control", "treatment"))
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
                fetchedAtMs = System.currentTimeMillis()
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
    }
}

