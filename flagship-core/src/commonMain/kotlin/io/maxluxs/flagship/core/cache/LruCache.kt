package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LRU (Least Recently Used) cache with TTL validation and metrics.
 * 
 * Automatically evicts least recently used entries when size limit is reached.
 * Also validates TTL and tracks cache hit/miss statistics.
 * 
 * @property maxSize Maximum number of entries to keep (default: 100)
 * @property clock Clock for time tracking (default: SystemClock)
 */
class LruCache(
    private val maxSize: Int = 100,
    private val clock: Clock = SystemClock
) : FlagsCache {
    private data class CacheEntry(
        val snapshot: ProviderSnapshot,
        var lastAccessed: Long
    )
    
    private val cache = LinkedHashMap<String, CacheEntry>()
    private val mutex = Mutex()
    
    // Metrics
    private var hits: Long = 0
    private var misses: Long = 0
    
    override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        mutex.withLock {
            // Remove oldest entry if at capacity
            if (cache.size >= maxSize && !cache.containsKey(providerName)) {
                val oldestKey = cache.keys.firstOrNull()
                if (oldestKey != null) {
                    cache.remove(oldestKey)
                }
            }
            
            cache[providerName] = CacheEntry(
                snapshot = snapshot,
                lastAccessed = clock.currentTimeMs()
            )
        }
    }

    override suspend fun load(providerName: String): ProviderSnapshot? {
        return mutex.withLock {
            val entry = cache[providerName] ?: run {
                misses++
                return null
            }
            
            // Check TTL if present
            if (entry.snapshot.ttlMs != null) {
                val age = clock.currentTimeMs() - entry.snapshot.fetchedAtMs
                if (age > entry.snapshot.ttlMs) {
                    // Expired, remove from cache
                    cache.remove(providerName)
                    misses++
                    return null
                }
            }
            
            // Update last accessed time (LinkedHashMap will move to end)
            entry.lastAccessed = clock.currentTimeMs()
            hits++
            entry.snapshot
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
            hits = 0
            misses = 0
        }
    }
    
    /**
     * Get cache statistics.
     */
    suspend fun getStats(): CacheStats {
        return mutex.withLock {
            val total = hits + misses
            val hitRate = if (total > 0) hits.toDouble() / total else 0.0
            
            CacheStats(
                size = cache.size,
                maxSize = maxSize,
                hits = hits,
                misses = misses,
                hitRate = hitRate
            )
        }
    }
    
    /**
     * Remove all expired entries from cache.
     */
    suspend fun removeExpired() {
        mutex.withLock {
            val now = clock.currentTimeMs()
            val expiredKeys = cache.filter { (_, entry) ->
                entry.snapshot.ttlMs != null && 
                (now - entry.snapshot.fetchedAtMs) > entry.snapshot.ttlMs
            }.keys.toList()
            
            expiredKeys.forEach { cache.remove(it) }
        }
    }
}

/**
 * Cache statistics.
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hits: Long,
    val misses: Long,
    val hitRate: Double
)

