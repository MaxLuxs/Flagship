package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for override functionality (local overrides for testing)
 */
class OverrideTest {
    
    @BeforeTest
    fun setup() {
        Flagship.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flagship.reset()
    }
    
    @Test
    fun testOverrideTakesPrecedence() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(false))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Original value should be false
        assertFalse(manager.isEnabled("test_flag"))
        
        // Use listener to wait for override completion
        var overrideCompleted = false
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {}
            override fun onOverrideChanged(key: String) {
                if (key == "test_flag") {
                    overrideCompleted = true
                }
            }
        })
        
        // Set override
        manager.setOverride("test_flag", FlagValue.Bool(true))
        
        // Wait for override to complete
        var attempts = 0
        while (!overrideCompleted && attempts < 50) {
            delay(50)
            attempts++
        }
        
        // Override should take precedence
        assertTrue(manager.isEnabled("test_flag"))
    }
    
    @Test
    fun testClearOverride() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(false))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Use listener to wait for override completion
        var overrideChanged = false
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {}
            override fun onOverrideChanged(key: String) {
                overrideChanged = true
            }
        })
        
        // Set override
        manager.setOverride("test_flag", FlagValue.Bool(true))
        var attempts = 0
        while (!overrideChanged && attempts < 50) {
            delay(50)
            attempts++
        }
        assertTrue(manager.isEnabled("test_flag"))
        
        // Clear override
        overrideChanged = false
        manager.clearOverride("test_flag")
        attempts = 0
        while (!overrideChanged && attempts < 50) {
            delay(50)
            attempts++
        }
        
        // Should return to original value
        assertFalse(manager.isEnabled("test_flag"))
    }
    
    @Test
    fun testOverrideDifferentTypes() = runTest {
        val provider = TestProvider(
            flags = mapOf(
                "bool_flag" to FlagValue.Bool(false),
                "int_flag" to FlagValue.Int(10),
                "string_flag" to FlagValue.StringV("original")
            )
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Use listener to wait for override completion
        var overrideCount = 0
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {}
            override fun onOverrideChanged(key: String) {
                overrideCount++
            }
        })
        
        // Override bool
        manager.setOverride("bool_flag", FlagValue.Bool(true))
        while (overrideCount < 1) delay(50)
        assertTrue(manager.isEnabled("bool_flag"))
        
        // Override int
        manager.setOverride("int_flag", FlagValue.Int(99))
        while (overrideCount < 2) delay(50)
        assertEquals(99, manager.value("int_flag", default = 0))
        
        // Override string
        manager.setOverride("string_flag", FlagValue.StringV("overridden"))
        while (overrideCount < 3) delay(50)
        assertEquals("overridden", manager.value("string_flag", default = ""))
    }
    
    @Test
    fun testListOverrides() = runTest {
        val provider = TestProvider(
            flags = mapOf("flag1" to FlagValue.Bool(false))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // No overrides initially
        val overrides1 = manager.listOverrides()
        assertTrue(overrides1.isEmpty())
        
        // Use listener to wait for override completion
        var overrideCount = 0
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {}
            override fun onOverrideChanged(key: String) {
                overrideCount++
            }
        })
        
        // Set some overrides
        manager.setOverride("flag1", FlagValue.Bool(true))
        manager.setOverride("flag2", FlagValue.Int(42))
        while (overrideCount < 2) delay(50)
        
        val overrides2 = manager.listOverrides()
        assertEquals(2, overrides2.size)
        assertTrue(overrides2.containsKey("flag1"))
        assertTrue(overrides2.containsKey("flag2"))
    }
    
    @Test
    fun testOverrideForNonExistentFlag() = runTest {
        val provider = TestProvider(
            flags = emptyMap()
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flagship.configure(config)
        val manager = Flagship.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        // Use listener to wait for override completion
        var overrideChanged = false
        manager.addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {}
            override fun onOverrideChanged(key: String) {
                overrideChanged = true
            }
        })
        
        // Override non-existent flag
        manager.setOverride("new_flag", FlagValue.Bool(true))
        var attempts = 0
        while (!overrideChanged && attempts < 50) {
            delay(50)
            attempts++
        }
        
        // Should work with override
        assertTrue(manager.isEnabled("new_flag"))
        
        // Clear override
        overrideChanged = false
        manager.clearOverride("new_flag")
        attempts = 0
        while (!overrideChanged && attempts < 50) {
            delay(50)
            attempts++
        }
        
        // Should return to default
        assertFalse(manager.isEnabled("new_flag", default = false))
    }
}

