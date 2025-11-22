package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for FlagsManager with multiple providers
 */
class FlagsManagerIntegrationTest {
    
    private lateinit var mockProvider1: TestProvider
    private lateinit var mockProvider2: TestProvider
    
    @BeforeTest
    fun setup() {
        mockProvider1 = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            name = "provider1"
        )
        mockProvider2 = TestProvider(
            flags = mapOf("flag2" to FlagValue.Bool(false)),
            name = "provider2"
        )
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testProviderPrecedence() = runTest {
        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(mockProvider1, mockProvider2),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
        
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // First provider should take precedence
        assertTrue(manager.isEnabled("flag1", false))
        // flag2 is in second provider, should be false
        assertFalse(manager.isEnabled("flag2", true))
    }
    
    @Test
    fun testMultipleProvidersWithSameFlag() = runTest {
        val provider1 = TestProvider(
            flags = mapOf("shared_flag" to FlagValue.Bool(true)),
            name = "provider1"
        )
        val provider2 = TestProvider(
            flags = mapOf("shared_flag" to FlagValue.Bool(false)),
            name = "provider2"
        )
        
        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(provider1, provider2),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
        
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // First provider should win
        assertTrue(manager.isEnabled("shared_flag", false))
    }
    
    @Test
    fun testBootstrapFlow() = runTest {
        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(mockProvider1),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
        
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Bootstrap should be called automatically or manually
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("flag1", false))
    }
}

