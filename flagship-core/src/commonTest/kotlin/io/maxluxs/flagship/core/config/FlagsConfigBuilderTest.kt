package io.maxluxs.flagship.core.config

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlagsConfigBuilderTest {
    
    class TestProvider : FlagsProvider {
        override val name: String = "test"
        
        override suspend fun bootstrap(): io.maxluxs.flagship.core.model.ProviderSnapshot {
            return io.maxluxs.flagship.core.model.ProviderSnapshot(
                flags = mapOf("test_flag" to FlagValue.Bool(true)),
                experiments = emptyMap(),
                revision = "rev1",
                fetchedAtMs = System.currentTimeMillis()
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
    fun testBasicDSLConfiguration() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        
        Flagship.configure {
            appKey = "test-app"
            environment = "test"
            
            providers {
                + provider
            }
            
            cache = InMemoryCache()
            logger = DefaultLogger()
        }
        
        assertTrue(Flagship.isConfigured())
        val manager = Flagship.manager()
        assertNotNull(manager)
    }
    
    @Test
    fun testDSLWithMultipleProviders() = runTest {
        Flagship.reset()
        
        val provider1 = TestProvider()
        val provider2 = TestProvider()
        
        Flagship.configure {
            appKey = "test-app"
            environment = "production"
            
            providers {
                + provider1
                + provider2
            }
        }
        
        assertTrue(Flagship.isConfigured())
    }
    
    @Test
    fun testDSLWithAllOptions() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        
        Flagship.configure {
            appKey = "test-app"
            environment = "staging"
            defaultRefreshIntervalMs = 5 * 60_000L // 5 minutes
            
            providers {
                + provider
            }
            
            cache = InMemoryCache()
            logger = DefaultLogger()
            enableRealtime = true
            enablePerformanceProfiling = true
            maxSnapshots = 5
            maxSnapshotAgeMs = 12 * 60 * 60 * 1000L // 12 hours
        }
        
        assertTrue(Flagship.isConfigured())
    }
    
    @Test
    fun testDSLRequiresAppKey() = runTest {
        Flagship.reset()
        
        assertFailsWith<IllegalArgumentException> {
            Flagship.configure {
                // appKey not set
                environment = "test"
                providers {
                    + TestProvider()
                }
            }
        }
    }
    
    @Test
    fun testDSLWithEmptyAppKey() = runTest {
        Flagship.reset()
        
        assertFailsWith<IllegalArgumentException> {
            Flagship.configure {
                appKey = "" // Empty appKey
                environment = "test"
                providers {
                    + TestProvider()
                }
            }
        }
    }
    
    @Test
    fun testDSLDefaults() = runTest {
        Flagship.reset()
        
        val provider = TestProvider()
        
        Flagship.configure {
            appKey = "test-app"
            // environment defaults to "production"
            // defaultRefreshIntervalMs defaults to 15 minutes
            // cache defaults to InMemoryCache
            // logger defaults to NoopLogger
            
            providers {
                + provider
            }
        }
        
        assertTrue(Flagship.isConfigured())
        val manager = Flagship.manager()
        assertNotNull(manager)
    }
    
    @Test
    fun testDSLBuilderBuildMethod() {
        val provider = TestProvider()
        
        val config = FlagsConfigBuilder().apply {
            appKey = "test-app"
            environment = "test"
            providers {
                + provider
            }
            cache = InMemoryCache()
            logger = DefaultLogger()
        }.build()
        
        assertEquals("test-app", config.appKey)
        assertEquals("test", config.environment)
        assertEquals(1, config.providers.size)
        assertTrue(config.cache is InMemoryCache)
    }
    
    @Test
    fun testDSLBuilderWithCustomRefreshInterval() {
        val provider = TestProvider()
        
        val config = FlagsConfigBuilder().apply {
            appKey = "test-app"
            environment = "test"
            defaultRefreshIntervalMs = 30 * 60_000L // 30 minutes
            providers {
                + provider
            }
        }.build()
        
        assertEquals(30 * 60_000L, config.defaultRefreshIntervalMs)
    }
    
    @Test
    fun testDSLBuilderProviderOrder() {
        val provider1 = TestProvider()
        val provider2 = TestProvider()
        
        val config = FlagsConfigBuilder().apply {
            appKey = "test-app"
            environment = "test"
            providers {
                + provider1
                + provider2
            }
        }.build()
        
        assertEquals(2, config.providers.size)
        assertEquals(provider1, config.providers[0])
        assertEquals(provider2, config.providers[1])
    }
}

