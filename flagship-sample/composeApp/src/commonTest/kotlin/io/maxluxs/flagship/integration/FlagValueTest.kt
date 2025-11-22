package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for typed flag values (Bool, Int, Double, String)
 */
class FlagValueTest {
    
    @BeforeTest
    fun setup() {
        Flags.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testBooleanFlag() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "enabled_flag" to FlagValue.Bool(true),
                "disabled_flag" to FlagValue.Bool(false)
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("enabled_flag"))
        assertFalse(manager.isEnabled("disabled_flag"))
        assertFalse(manager.isEnabled("nonexistent_flag", default = false))
        assertTrue(manager.isEnabled("nonexistent_flag", default = true))
    }
    
    @Test
    fun testIntFlag() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "max_retries" to FlagValue.Int(5),
                "timeout" to FlagValue.Int(30)
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertEquals(5, manager.value("max_retries", default = 0))
        assertEquals(30, manager.value("timeout", default = 0))
        assertEquals(100, manager.value("nonexistent_int", default = 100))
    }
    
    @Test
    fun testDoubleFlag() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "pi" to FlagValue.Double(3.14159),
                "threshold" to FlagValue.Double(0.95)
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertEquals(3.14159, manager.value("pi", default = 0.0), 0.00001)
        assertEquals(0.95, manager.value("threshold", default = 0.0), 0.001)
        assertEquals(1.0, manager.value("nonexistent_double", default = 1.0), 0.001)
    }
    
    @Test
    fun testStringFlag() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "welcome_message" to FlagValue.StringV("Welcome!"),
                "api_url" to FlagValue.StringV("https://api.example.com")
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertEquals("Welcome!", manager.value("welcome_message", default = ""))
        assertEquals("https://api.example.com", manager.value("api_url", default = ""))
        assertEquals("default", manager.value("nonexistent_string", default = "default"))
    }
    
    @Test
    fun testMixedFlagTypes() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "feature_enabled" to FlagValue.Bool(true),
                "max_items" to FlagValue.Int(100),
                "discount_rate" to FlagValue.Double(0.15),
                "theme_color" to FlagValue.StringV("blue")
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        assertTrue(manager.isEnabled("feature_enabled"))
        assertEquals(100, manager.value("max_items", default = 0))
        assertEquals(0.15, manager.value("discount_rate", default = 0.0), 0.001)
        assertEquals("blue", manager.value("theme_color", default = ""))
    }
    
    @Test
    fun testTypeCoercion() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "int_as_bool" to FlagValue.Int(1), // Should not be treated as bool
                "string_as_int" to FlagValue.StringV("42") // Should not be treated as int
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Type coercion should not happen - each type should be used correctly
        // Int(1) should not be treated as Bool(true)
        assertEquals(1, manager.value("int_as_bool", default = 0))
        assertFalse(manager.isEnabled("int_as_bool", default = false)) // Should use default, not coerce
        
        // String should not be treated as Int
        assertEquals("42", manager.value("string_as_int", default = ""))
        assertEquals(0, manager.value("string_as_int", default = 0)) // Should use default, not parse
    }
}

