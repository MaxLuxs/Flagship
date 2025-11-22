package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheTest {
    @Test
    fun testInMemoryCacheSaveAndLoad() = runTest {
        val cache = InMemoryCache()
        val snapshot = ProviderSnapshot(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            fetchedAtMs = currentTimeMillis()
        )

        cache.save("provider1", snapshot)
        val loaded = cache.load("provider1")

        assertEquals(snapshot, loaded)
    }

    @Test
    fun testInMemoryCacheClear() = runTest {
        val cache = InMemoryCache()
        val snapshot = ProviderSnapshot(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            fetchedAtMs = currentTimeMillis()
        )

        cache.save("provider1", snapshot)
        cache.clear("provider1")
        val loaded = cache.load("provider1")

        assertNull(loaded)
    }

    @Test
    fun testInMemoryCacheClearAll() = runTest {
        val cache = InMemoryCache()
        val snapshot1 = ProviderSnapshot(
            flags = mapOf("flag1" to FlagValue.Bool(true)),
            fetchedAtMs = currentTimeMillis()
        )
        val snapshot2 = ProviderSnapshot(
            flags = mapOf("flag2" to FlagValue.Bool(false)),
            fetchedAtMs = currentTimeMillis()
        )

        cache.save("provider1", snapshot1)
        cache.save("provider2", snapshot2)
        cache.clearAll()

        assertNull(cache.load("provider1"))
        assertNull(cache.load("provider2"))
    }
}

