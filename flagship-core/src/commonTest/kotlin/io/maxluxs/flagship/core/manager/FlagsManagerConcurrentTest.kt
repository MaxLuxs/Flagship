package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlagsManagerConcurrentTest {
    private val context = EvalContext(
        userId = "test_user",
        deviceId = "test_device",
        appVersion = "1.0.0",
        osName = "Test",
        osVersion = "1.0"
    )

    @Test
    fun testConcurrentFlagAccess() = runTest {
        val provider = TestProvider(flags = mapOf(
            "flag1" to FlagValue.Bool(true),
            "flag2" to FlagValue.Bool(false),
            "flag3" to FlagValue.Int(42)
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

        // Concurrent reads
        val results = coroutineScope {
            (1..100).map { i ->
                async {
                    when (i % 3) {
                        0 -> manager.isEnabled("flag1", false)
                        1 -> manager.isEnabled("flag2", false)
                        else -> manager.value("flag3", 0)
                    }
                }
            }.awaitAll()
        }

        // All reads should succeed
        assertEquals(100, results.size)
        assertTrue(results.all { it is Boolean || it is Int })
    }

    @Test
    fun testConcurrentBootstrap() = runTest {
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

        // Multiple concurrent bootstrap calls
        val results = coroutineScope {
            (1..10).map {
                async {
                    manager.ensureBootstrap(5000)
                }
            }.awaitAll()
        }

        // All should succeed
        assertTrue(results.all { it })
        
        // Manager should be bootstrapped
        assertTrue(manager.isEnabled("test_flag", false))
    }

    @Test
    fun testConcurrentRefresh() = runTest {
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

        // Multiple concurrent refresh calls
        coroutineScope {
            (1..10).forEach {
                launch {
                    manager.refresh(force = true)
                }
            }
        }

        // Wait for all refreshes to complete
        delay(1000)

        // Manager should still work
        assertTrue(manager.isEnabled("test_flag", false))
    }

    @Test
    fun testConcurrentOverrides() = runTest {
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

        // Concurrent set/clear overrides
        coroutineScope {
            (1..50).forEach { i ->
                launch {
                    manager.setOverride("flag$i", FlagValue.Bool(true))
                }
            }
            (1..50).forEach { i ->
                launch {
                    manager.clearOverride("flag$i")
                }
            }
        }

        // Wait for all operations to complete
        delay(500)

        // Manager should still work
        assertTrue(manager.isEnabled("test_flag", false))
    }

    @Test
    fun testConcurrentListeners() = runTest {
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

        var listenerCount = 0
        val listeners = mutableListOf<FlagsListener>()

        // Add listeners concurrently
        coroutineScope {
            (1..20).forEach {
                launch {
                    val listener = object : FlagsListener {
                        override fun onSnapshotUpdated(source: String) {
                            listenerCount++
                        }
                        override fun onOverrideChanged(key: String) {}
                    }
                    listeners.add(listener)
                    manager.addListener(listener)
                }
            }
        }

        // Bootstrap should notify all listeners
        manager.ensureBootstrap(5000)
        delay(100)

        // All listeners should be notified
        assertTrue(listenerCount >= 20)

        // Remove listeners concurrently
        coroutineScope {
            listeners.forEach { listener ->
                launch {
                    manager.removeListener(listener)
                }
            }
        }

        delay(100)
    }

    @Test
    fun testConcurrentExperimentAssignment() = runTest {
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
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        manager.ensureBootstrap(5000)

        // Concurrent experiment assignments
        val assignments = coroutineScope {
            (1..100).map {
                async {
                    manager.assign("test_exp")
                }
            }.awaitAll()
        }

        // All assignments should succeed
        assertEquals(100, assignments.size)
        assertTrue(assignments.all { it != null })
        
        // Should have both variants
        val variants = assignments.mapNotNull { it?.variant }.toSet()
        assertTrue(variants.contains("control"))
        assertTrue(variants.contains("treatment"))
    }

    @Test
    fun testConcurrentBootstrapAndAccess() = runTest {
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

        // Concurrent bootstrap and flag access
        coroutineScope {
            launch {
                manager.ensureBootstrap(5000)
            }
            
            // Try to access flags while bootstrapping
            (1..50).map { i ->
                async {
                    try {
                        manager.isEnabled("test_flag", false)
                    } catch (e: IllegalStateException) {
                        // Expected before bootstrap completes
                        null
                    }
                }
            }.awaitAll()
        }

        // After bootstrap, should work
        assertTrue(manager.isEnabled("test_flag", false))
    }

    @Test
    fun testConcurrentRefreshAndAccess() = runTest {
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

        // Concurrent refresh and flag access
        coroutineScope {
            launch {
                manager.refresh(force = true)
            }
            
            // Access flags while refreshing
            val results = (1..100).map {
                async {
                    manager.isEnabled("test_flag", false)
                }
            }.awaitAll()
            
            assertTrue(results.all { it is Boolean })
        }
    }

    @Test
    fun testConcurrentSyncAndAsyncAccess() = runTest {
        val provider = TestProvider(flags = mapOf("flag1" to FlagValue.Bool(true)))
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

        // Concurrent sync and async access
        coroutineScope {
            // Async access
            val asyncResults = (1..50).map {
                async {
                    manager.isEnabled("flag1", false)
                }
            }
            
            // Sync access (on different thread)
            val syncResults = (1..50).map {
                async {
                    manager.isEnabledSync("flag1", false)
                }
            }
            
            val allResults = asyncResults.awaitAll() + syncResults.awaitAll()
            assertTrue(allResults.all { it == true })
        }
    }

    @Test
    fun testRaceConditionOnBootstrap() = runTest {
        val slowProvider = object : FlagsProvider {
            override val name: String = "slow"
            private var callCount = 0
            
            override suspend fun bootstrap(): ProviderSnapshot {
                callCount++
                delay(100) // Simulate slow bootstrap
                return ProviderSnapshot(
                    flags = mapOf("flag1" to FlagValue.Bool(true)),
                    experiments = emptyMap(),
                    revision = "rev$callCount",
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

        // Multiple concurrent bootstrap calls
        val results = coroutineScope {
            (1..10).map {
                async {
                    manager.ensureBootstrap(5000)
                }
            }.awaitAll()
        }

        // All should succeed
        assertTrue(results.all { it })
        
        // Manager should be in consistent state
        assertTrue(manager.isEnabled("flag1", false))
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

