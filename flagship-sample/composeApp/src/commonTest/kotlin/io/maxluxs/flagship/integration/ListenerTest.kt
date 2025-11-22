package io.maxluxs.flagship.integration
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.maxluxs.flagship.helpers.TestProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for listener functionality (notifications on flag changes)
 */
class ListenerTest {
    
    @BeforeTest
    fun setup() {
        Flags.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testListenerNotifiedOnSnapshotUpdate() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val snapshotUpdates = Channel<String>(Channel.UNLIMITED)
        val listener = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                snapshotUpdates.trySend(source)
            }
            
            override fun onOverrideChanged(key: String) {
                // Not testing this here
            }
        }
        
        manager.addListener(listener)
        manager.ensureBootstrap()
        
        // Should receive notification
        val source = snapshotUpdates.receive()
        assertTrue(source.isNotEmpty())
        
        manager.removeListener(listener)
    }
    
    @Test
    fun testListenerNotifiedOnOverride() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(false))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        manager.ensureBootstrap()
        
        val overrideChanges = Channel<String>(Channel.UNLIMITED)
        val listener = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                // Not testing this here
            }
            
            override fun onOverrideChanged(key: String) {
                overrideChanges.trySend(key)
            }
        }
        
        manager.addListener(listener)
        
        // Set override
        manager.setOverride("test_flag", FlagValue.Bool(true))
        
        // Should receive notification
        val key = overrideChanges.receive()
        assertEquals("test_flag", key)
        
        manager.removeListener(listener)
    }
    
    @Test
    fun testMultipleListeners() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val updates1 = Channel<String>(Channel.UNLIMITED)
        val updates2 = Channel<String>(Channel.UNLIMITED)
        
        val listener1 = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                updates1.trySend(source)
            }
            override fun onOverrideChanged(key: String) {}
        }
        
        val listener2 = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                updates2.trySend(source)
            }
            override fun onOverrideChanged(key: String) {}
        }
        
        manager.addListener(listener1)
        manager.addListener(listener2)
        manager.ensureBootstrap()
        
        // Both listeners should receive notification
        val source1 = updates1.receive()
        val source2 = updates2.receive()
        assertTrue(source1.isNotEmpty())
        assertTrue(source2.isNotEmpty())
        
        manager.removeListener(listener1)
        manager.removeListener(listener2)
    }
    
    @Test
    fun testRemoveListener() = runTest {
        val provider = TestProvider(
            flags = mapOf("test_flag" to FlagValue.Bool(true))
        )
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val updates = Channel<String>(Channel.UNLIMITED)
        val listener = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                updates.trySend(source)
            }
            override fun onOverrideChanged(key: String) {}
        }
        
        manager.addListener(listener)
        manager.ensureBootstrap()
        
        // Should receive initial notification
        updates.receive()
        
        // Remove listener
        manager.removeListener(listener)
        
        // Refresh should not notify removed listener
        manager.refresh()
        kotlinx.coroutines.delay(100)
        
        // Channel should be empty (no new notifications)
        assertTrue(updates.isEmpty)
    }
}

