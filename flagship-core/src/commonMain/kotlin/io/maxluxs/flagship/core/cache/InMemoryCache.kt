package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache with TTL validation.
 * 
 * Automatically invalidates expired snapshots based on TTL.
 * 
 * @property clock Clock for time tracking (default: SystemClock)
 */
class InMemoryCache(
    private val clock: Clock = SystemClock
) : FlagsCache {
    private val cache = mutableMapOf<String, ProviderSnapshot>()
    private val mutex = Mutex()

    override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        mutex.withLock {
            cache[providerName] = snapshot
        }
    }

    override suspend fun load(providerName: String): ProviderSnapshot? {
        return mutex.withLock {
            val snapshot = cache[providerName] ?: return null
            
            // Check TTL if present
            if (snapshot.ttlMs != null) {
                val age = clock.currentTimeMs() - snapshot.fetchedAtMs
                if (age > snapshot.ttlMs) {
                    // Expired, remove from cache
                    cache.remove(providerName)
                    return null
                }
            }
            
            snapshot
        }
    }

    override suspend fun clear(providerName: String) {
        mutex.withLock {
            cache.remove(providerName)
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * Remove all expired entries from cache.
     */
    suspend fun removeExpired() {
        mutex.withLock {
            val now = clock.currentTimeMs()
            val expiredKeys = cache.filter { (_, snapshot) ->
                snapshot.ttlMs != null && (now - snapshot.fetchedAtMs) > snapshot.ttlMs
            }.keys
            
            expiredKeys.forEach { cache.remove(it) }
        }
    }
}

