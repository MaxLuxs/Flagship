package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LruCacheTest {
    
    @Test
    fun testSaveAndLoad() = runTest {
        val cache = LruCache(maxSize = 10)
        val snapshot = ProviderSnapshot(
            flags = mapOf("test_flag" to FlagValue.Bool(true)),
            experiments = emptyMap(),
            revision = "rev1",
            fetchedAtMs = 1000L
        )
        
        cache.save("provider1", snapshot)
        val loaded = cache.load("provider1")
        
        assertEquals(snapshot, loaded)
    }
    
    @Test
    fun testLruEviction() = runTest {
        val cache = LruCache(maxSize = 2)
        
        val snapshot1 = ProviderSnapshot(flags = mapOf("flag1" to FlagValue.Bool(true)), experiments = emptyMap(), revision = "rev1", fetchedAtMs = 1000L)
        val snapshot2 = ProviderSnapshot(flags = mapOf("flag2" to FlagValue.Bool(true)), experiments = emptyMap(), revision = "rev2", fetchedAtMs = 2000L)
        val snapshot3 = ProviderSnapshot(flags = mapOf("flag3" to FlagValue.Bool(true)), experiments = emptyMap(), revision = "rev3", fetchedAtMs = 3000L)
        
        cache.save("provider1", snapshot1)
        cache.save("provider2", snapshot2)
        cache.save("provider3", snapshot3) // Should evict provider1
        
        assertNull(cache.load("provider1"))
        assertEquals(snapshot2, cache.load("provider2"))
        assertEquals(snapshot3, cache.load("provider3"))
    }
    
    @Test
    fun testTtlExpiration() = runTest {
        val cache = LruCache(maxSize = 10)
        val snapshot = ProviderSnapshot(
            flags = mapOf("test_flag" to FlagValue.Bool(true)),
            experiments = emptyMap(),
            revision = "rev1",
            fetchedAtMs = 1000L,
            ttlMs = 5000L
        )
        
        cache.save("provider1", snapshot)
        
        // Create expired snapshot
        val expiredSnapshot = snapshot.copy(fetchedAtMs = 0L)
        cache.save("provider1", expiredSnapshot)
        
        val loaded = cache.load("provider1")
        assertNull(loaded) // Should be null due to expiration
    }
    
    @Test
    fun testCacheStats() = runTest {
        val cache = LruCache(maxSize = 10)
        val snapshot = ProviderSnapshot(
            flags = mapOf("test_flag" to FlagValue.Bool(true)),
            experiments = emptyMap(),
            revision = "rev1",
            fetchedAtMs = 1000L
        )
        
        cache.save("provider1", snapshot)
        cache.load("provider1") // Hit
        cache.load("provider1") // Hit
        cache.load("provider2") // Miss
        
        val stats = cache.getStats()
        assertEquals(1, stats.size)
        assertEquals(2, stats.hits)
        assertEquals(1, stats.misses)
        assertTrue(stats.hitRate > 0.5)
    }
}

