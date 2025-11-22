package io.maxluxs.flagship.ui

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.helpers.TestHelpers
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * UI tests for FlagsDashboard component
 * 
 * Note: Full Compose UI testing requires experimental APIs and platform-specific setup.
 * These tests verify the manager integration and basic functionality that FlagsDashboard uses.
 */
class FlagsDashboardTest {

    @BeforeTest
    fun setup() {
        Flags.reset()
    }

    @AfterTest
    fun teardown() {
        Flags.reset()
    }

    @Test
    fun testDashboardManagerIntegration() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "test_flag" to FlagValue.Bool(true),
                "test_int" to FlagValue.Int(42)
            )
        )

        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()

        // Verify manager is ready for FlagsDashboard
        assertNotNull(manager)
        assertTrue(manager.isEnabled("test_flag", false))
    }

    @Test
    fun testDashboardManagerWithOverrides() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )

        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()

        // Set an override
        manager.setOverride("test_flag", FlagValue.Bool(false))
        kotlinx.coroutines.delay(100) // Wait for async operation

        // Verify override is set (FlagsDashboard will show this)
        val overrides = manager.listOverrides()
        assertTrue(overrides.containsKey("test_flag"))
        assertEquals(FlagValue.Bool(false), overrides["test_flag"])
    }

    @Test
    fun testDashboardManagerWithMultipleFlags() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "flag1" to FlagValue.Bool(true),
                "flag2" to FlagValue.Int(42),
                "flag3" to FlagValue.StringV("test")
            )
        )

        val config = FlagsConfig(
            appKey = "test-app",
            environment = "test",
            providers = listOf(provider),
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )

        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()

        // Verify all flags are available (FlagsDashboard will list them)
        val allFlags = manager.listAllFlags()
        assertTrue(allFlags.containsKey("flag1"))
        assertTrue(allFlags.containsKey("flag2"))
        assertTrue(allFlags.containsKey("flag3"))
    }
}

