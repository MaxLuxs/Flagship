package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FlagsManagerPerformanceTest {
    private val context = EvalContext(
        userId = "test_user",
        deviceId = "test_device",
        appVersion = "1.0.0",
        osName = "Test",
        osVersion = "1.0"
    )

    @Test
    fun testBootstrapPerformance() = runTest {
        val flags = (1..1000).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        val startTime = currentTimeMillis()
        val bootstrapped = manager.ensureBootstrap(10000)
        val endTime = currentTimeMillis()
        val duration = endTime - startTime

        assertTrue(bootstrapped)
        // Bootstrap should complete in reasonable time (< 5 seconds for 1000 flags)
        assertTrue(duration < 5000, "Bootstrap took ${duration}ms, expected < 5000ms")
    }

    @Test
    fun testFlagEvaluationPerformance() = runTest {
        val flags = (1..1000).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
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

        // Measure evaluation performance
        val iterations = 10000
        val startTime = currentTimeMillis()
        
        repeat(iterations) { i ->
            manager.isEnabled("flag${i % 1000}", false)
        }
        
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations

        // Average evaluation should be very fast (< 1ms per evaluation)
        assertTrue(avgTime < 1.0, "Average evaluation took ${avgTime}ms, expected < 1ms")
    }

    @Test
    fun testSyncEvaluationPerformance() = runTest {
        val flags = (1..100).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
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

        // Measure sync evaluation performance
        val iterations = 10000
        val startTime = currentTimeMillis()
        
        repeat(iterations) { i ->
            manager.isEnabledSync("flag${i % 100}", false)
        }
        
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations

        // Sync evaluation should be very fast (< 0.1ms per evaluation)
        assertTrue(avgTime < 0.1, "Average sync evaluation took ${avgTime}ms, expected < 0.1ms")
    }

    @Test
    fun testRefreshPerformance() = runTest {
        val flags = (1..500).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
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

        // Measure refresh performance
        val iterations = 10
        val startTime = currentTimeMillis()
        
        repeat(iterations) {
            manager.refresh(force = true)
            kotlinx.coroutines.delay(100) // Wait for refresh to complete
        }
        
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations

        // Average refresh should complete in reasonable time (< 500ms per refresh)
        assertTrue(avgTime < 500, "Average refresh took ${avgTime}ms, expected < 500ms")
    }

    @Test
    fun testMultipleProvidersPerformance() = runTest {
        val providers = (1..10).map { i ->
            TestProvider(
                name = "provider$i",
                flags = (1..100).associate { "flag${i}_$it" to FlagValue.Bool(true) }
            )
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = providers,
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        val startTime = currentTimeMillis()
        val bootstrapped = manager.ensureBootstrap(10000)
        val endTime = currentTimeMillis()
        val duration = endTime - startTime

        assertTrue(bootstrapped)
        // Bootstrap with 10 providers should complete in reasonable time (< 5 seconds)
        assertTrue(duration < 5000, "Bootstrap with 10 providers took ${duration}ms, expected < 5000ms")
    }

    @Test
    fun testExperimentAssignmentPerformance() = runTest {
        val experiments = (1..100).associate { i ->
            "exp$i" to ExperimentDefinition(
                key = "exp$i",
                variants = listOf(
                    Variant("control", 0.5),
                    Variant("treatment", 0.5)
                )
            )
        }

        val provider = TestProvider(experiments = experiments)
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

        // Measure experiment assignment performance
        val iterations = 10000
        val startTime = currentTimeMillis()
        
        repeat(iterations) { i ->
            manager.assign("exp${i % 100}")
        }
        
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations

        // Average assignment should be fast (< 1ms per assignment)
        assertTrue(avgTime < 1.0, "Average assignment took ${avgTime}ms, expected < 1ms")
    }

    @Test
    fun testCachePerformance() = runTest {
        val flags = (1..1000).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
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

        // First access (cache miss)
        val startTime1 = currentTimeMillis()
        manager.isEnabled("flag1", false)
        val endTime1 = currentTimeMillis()
        val firstAccess = endTime1 - startTime1

        // Second access (should be faster due to caching)
        val startTime2 = currentTimeMillis()
        manager.isEnabled("flag1", false)
        val endTime2 = currentTimeMillis()
        val secondAccess = endTime2 - startTime2

        // Second access should be at least as fast as first
        assertTrue(secondAccess <= firstAccess, "Cached access should be faster")
    }

    @Test
    fun testOverridePerformance() = runTest {
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

        // Measure override performance
        val iterations = 1000
        val startTime = currentTimeMillis()
        
        repeat(iterations) { i ->
            manager.setOverride("flag$i", FlagValue.Bool(true))
        }
        
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        val avgTime = duration.toDouble() / iterations

        // Average override should be fast (< 1ms per override)
        assertTrue(avgTime < 1.0, "Average override took ${avgTime}ms, expected < 1ms")
    }

    @Test
    fun testListenerPerformance() = runTest {
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

        // Add many listeners
        val listeners = (1..100).map {
            object : FlagsListener {
                override fun onSnapshotUpdated(source: String) {}
                override fun onOverrideChanged(key: String) {}
            }
        }

        val startTime = currentTimeMillis()
        listeners.forEach { manager.addListener(it) }
        val endTime = currentTimeMillis()
        val duration = endTime - startTime

        // Adding listeners should be fast (< 100ms for 100 listeners)
        assertTrue(duration < 100, "Adding 100 listeners took ${duration}ms, expected < 100ms")

        // Bootstrap should notify all listeners
        manager.ensureBootstrap(5000)
        kotlinx.coroutines.delay(100)
    }

    @Test
    fun testMemoryUsage() = runTest {
        val flags = (1..10000).associate { "flag$it" to FlagValue.Bool(true) }
        val provider = TestProvider(flags = flags)
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Bootstrap with large dataset
        val bootstrapped = manager.ensureBootstrap(10000)
        assertTrue(bootstrapped)

        // Should still be able to evaluate flags
        assertTrue(manager.isEnabled("flag1", false))
        assertTrue(manager.isEnabled("flag5000", false))
        assertTrue(manager.isEnabled("flag10000", false))
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
            return null
        }

        override fun isHealthy(): Boolean = true
        override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
        override fun getConsecutiveFailures(): Int = 0
    }
}

