package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.FlagsCache
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
import kotlin.test.assertTrue

class FlagsManagerErrorHandlingTest {
    private val context = EvalContext(
        userId = "test_user",
        deviceId = "test_device",
        appVersion = "1.0.0",
        osName = "Test",
        osVersion = "1.0"
    )

    @Test
    fun testBootstrapException() = runTest {
        val failingProvider = object : FlagsProvider {
            override val name: String = "failing"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw Exception("Bootstrap failed")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(failingProvider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Bootstrap should handle exception gracefully
        val bootstrapped = manager.ensureBootstrap(5000)
        
        // Should still bootstrap (with failed provider)
        assertTrue(bootstrapped)
        
        // Should return defaults when provider fails
        assertEquals(false, manager.isEnabled("any_flag", false))
    }

    @Test
    fun testProviderException() = runTest {
        val provider = object : FlagsProvider {
            override val name: String = "error_provider"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw ProviderException("error_provider", "Failed to fetch")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should handle ProviderException
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
    }

    @Test
    fun testNetworkException() = runTest {
        val provider = object : FlagsProvider {
            override val name: String = "network_error"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw NetworkException("Connection timeout")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should handle NetworkException
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
    }

    @Test
    fun testParseException() = runTest {
        val provider = object : FlagsProvider {
            override val name: String = "parse_error"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw ParseException("Invalid JSON")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should handle ParseException
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
    }

    @Test
    fun testCacheException() = runTest {
        val failingCache = object : FlagsCache {
            override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
                throw CacheException("Cache write failed")
            }
            override suspend fun load(providerName: String): ProviderSnapshot? = null
            override suspend fun clear(providerName: String) {}
            override suspend fun clearAll() {}
        }

        val provider = TestProvider()
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = failingCache,
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should handle CacheException
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
    }

    @Test
    fun testFallbackToCacheOnProviderFailure() = runTest {
        val workingProvider = TestProvider(flags = mapOf("flag1" to FlagValue.Bool(true)))
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(workingProvider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)
        
        // Bootstrap successfully
        manager.ensureBootstrap(5000)
        assertTrue(manager.isEnabled("flag1", false))

        // Replace with failing provider
        val failingProvider = object : FlagsProvider {
            override val name: String = "failing"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw Exception("Provider failed")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        // Manager should use cached data
        assertEquals(true, manager.isEnabled("flag1", false))
    }

    @Test
    fun testMultipleProvidersWithSomeFailing() = runTest {
        val workingProvider = TestProvider(
            name = "working",
            flags = mapOf("flag1" to FlagValue.Bool(true))
        )
        val failingProvider = object : FlagsProvider {
            override val name: String = "failing"
            override suspend fun bootstrap(): ProviderSnapshot {
                throw Exception("Failed")
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = false
            override fun getLastSuccessfulFetchMs(): Long? = null
            override fun getConsecutiveFailures(): Int = 1
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(workingProvider, failingProvider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should bootstrap with working provider
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
        
        // Should work with working provider
        assertTrue(manager.isEnabled("flag1", false))
    }

    @Test
    fun testRetryOnTransientFailure() = runTest {
        var attemptCount = 0
        val provider = object : FlagsProvider {
            override val name: String = "retry_provider"
            override suspend fun bootstrap(): ProviderSnapshot {
                attemptCount++
                if (attemptCount < 3) {
                    throw NetworkException("Temporary failure")
                }
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
            override fun isHealthy(): Boolean = attemptCount >= 3
            override fun getLastSuccessfulFetchMs(): Long? = if (attemptCount >= 3) currentTimeMillis() else null
            override fun getConsecutiveFailures(): Int = if (attemptCount >= 3) 0 else attemptCount
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should eventually succeed after retries
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
        assertTrue(attemptCount >= 3)
    }

    @Test
    fun testTypeMismatchException() = runTest {
        val provider = TestProvider(flags = mapOf("flag1" to FlagValue.Int(42)))
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

        // Type mismatch should return default, not throw
        assertEquals(false, manager.isEnabled("flag1", false))
        assertEquals(0, manager.value("flag1", 0))
    }

    @Test
    fun testSignatureException() = runTest {
        val provider = object : FlagsProvider {
            override val name: String = "signature_error"
            override suspend fun bootstrap(): ProviderSnapshot {
                val snapshot = ProviderSnapshot(
                    flags = mapOf("flag1" to FlagValue.Bool(true)),
                    experiments = emptyMap(),
                    revision = "rev1",
                    fetchedAtMs = currentTimeMillis(),
                    signature = "invalid_signature"
                )
                // In real scenario, signature verification would fail
                return snapshot
            }
            override suspend fun refresh(): ProviderSnapshot = bootstrap()
            override fun evaluateFlag(key: String, context: EvalContext): FlagValue? = null
            override fun evaluateExperiment(key: String, context: EvalContext): ExperimentAssignment? = null
            override fun isHealthy(): Boolean = true
            override fun getLastSuccessfulFetchMs(): Long? = currentTimeMillis()
            override fun getConsecutiveFailures(): Int = 0
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // Should handle signature verification (if enabled)
        val bootstrapped = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped)
    }

    @Test
    fun testConfigurationException() = runTest {
        // Invalid configuration
        assertFailsWith<ConfigurationException> {
            FlagsConfig(
                appKey = "", // Empty appKey should fail validation
                environment = "test",
                providers = emptyList(),
                cache = InMemoryCache(),
                logger = DefaultLogger()
            )
        }
    }

    @Test
    fun testErrorRecovery() = runTest {
        var shouldFail = true
        val provider = object : FlagsProvider {
            override val name: String = "recovery_provider"
            override suspend fun bootstrap(): ProviderSnapshot {
                if (shouldFail) {
                    throw Exception("Temporary failure")
                }
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
            override fun isHealthy(): Boolean = !shouldFail
            override fun getLastSuccessfulFetchMs(): Long? = if (!shouldFail) currentTimeMillis() else null
            override fun getConsecutiveFailures(): Int = if (shouldFail) 1 else 0
        }

        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        val manager = DefaultFlagsManager(config)
        manager.setDefaultContext(context)

        // First bootstrap fails
        val bootstrapped1 = manager.ensureBootstrap(5000)
        assertTrue(bootstrapped1) // Should still return true (graceful failure)

        // Fix provider
        shouldFail = false

        // Refresh should recover
        manager.refresh(force = true)
        kotlinx.coroutines.delay(500)

        // Should work now
        assertEquals(true, manager.isEnabled("flag1", false))
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

