package io.maxluxs.flagship.ui

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.helpers.TestHelpers
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI tests for override interactions
 * 
 * Note: Full Compose UI testing requires experimental APIs and platform-specific setup.
 * These tests verify the override functionality that OverridesScreen uses.
 */
class OverrideInteractionTest {

    @BeforeTest
    fun setup() {
        Flags.reset()
    }

    @AfterTest
    fun teardown() {
        Flags.reset()
    }

    @Test
    fun testOverridesEmptyState() = runTest {
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

        // Verify no overrides initially (OverridesScreen will show empty state)
        val overrides = manager.listOverrides()
        assertTrue(overrides.isEmpty())
    }

    @Test
    fun testOverridesSetAndList() = runTest {
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
        
        // Wait for override to be set
        delay(200) // Wait for async operation to complete

        // Verify override is available (OverridesScreen will display it)
        val overrides = manager.listOverrides()
        assertTrue(overrides.containsKey("test_flag"))
        assertEquals(FlagValue.Bool(false), overrides["test_flag"])
    }

    @Test
    fun testOverridesMultipleFlags() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "flag1" to FlagValue.Bool(true),
                "flag2" to FlagValue.Int(42)
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

        // Set multiple overrides
        manager.setOverride("flag1", FlagValue.Bool(false))
        manager.setOverride("flag2", FlagValue.Int(100))
        
        // Wait for overrides to be set
        delay(200)

        // Verify both overrides are available (OverridesScreen will list them)
        val overrides = manager.listOverrides()
        assertTrue(overrides.containsKey("flag1"))
        assertTrue(overrides.containsKey("flag2"))
        assertEquals(FlagValue.Bool(false), overrides["flag1"])
        assertEquals(FlagValue.Int(100), overrides["flag2"])
    }

    @Test
    fun testOverridesCount() = runTest {
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
        delay(200)

        // Verify override count (OverridesScreen will show this)
        val overrides = manager.listOverrides()
        assertEquals(1, overrides.size)
    }

    @Test
    fun testOverridesClear() = runTest {
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
        delay(200)

        // Clear override
        manager.clearOverride("test_flag")
        delay(200)

        // Verify override is cleared (OverridesScreen will show empty state)
        val overrides = manager.listOverrides()
        assertTrue(overrides.isEmpty())
    }

    @Test
    fun testOverridesDifferentValueTypes() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "bool_flag" to FlagValue.Bool(true),
                "int_flag" to FlagValue.Int(42),
                "string_flag" to FlagValue.StringV("test"),
                "double_flag" to FlagValue.Double(3.14)
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

        // Set overrides with different types
        manager.setOverride("bool_flag", FlagValue.Bool(false))
        manager.setOverride("int_flag", FlagValue.Int(100))
        manager.setOverride("string_flag", FlagValue.StringV("override"))
        manager.setOverride("double_flag", FlagValue.Double(2.71))
        
        delay(200)

        // Verify all types are stored correctly (OverridesScreen will display them)
        val overrides = manager.listOverrides()
        assertEquals(4, overrides.size)
        assertEquals(FlagValue.Bool(false), overrides["bool_flag"])
        assertEquals(FlagValue.Int(100), overrides["int_flag"])
        assertEquals(FlagValue.StringV("override"), overrides["string_flag"])
        assertEquals(FlagValue.Double(2.71), overrides["double_flag"])
    }
}

