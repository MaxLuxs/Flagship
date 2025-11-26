package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.model.ProviderSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe cache for provider snapshots.
 * 
 * Used by providers to cache last successful snapshot for fallback.
 * 
 * Example usage:
 * ```kotlin
 * val cache = SnapshotCache()
 * 
 * // Update cache after successful fetch
 * cache.update(snapshot)
 * 
 * // Get cached snapshot on error
 * val cached = cache.get() ?: throw Exception("No cached snapshot")
 * ```
 */
class SnapshotCache {
    private var lastSuccessfulSnapshot: ProviderSnapshot? = null
    private val mutex = Mutex()
    
    /**
     * Get cached snapshot.
     * 
     * @return Cached snapshot, or null if no snapshot was cached
     */
    suspend fun get(): ProviderSnapshot? {
        return mutex.withLock {
            lastSuccessfulSnapshot
        }
    }
    
    /**
     * Update cached snapshot.
     * 
     * @param snapshot The snapshot to cache
     */
    suspend fun update(snapshot: ProviderSnapshot) {
        mutex.withLock {
            lastSuccessfulSnapshot = snapshot
        }
    }
    
    /**
     * Clear cached snapshot.
     */
    suspend fun clear() {
        mutex.withLock {
            lastSuccessfulSnapshot = null
        }
    }
    
    /**
     * Check if cache has snapshot.
     * 
     * @return true if cache has a snapshot, false otherwise
     */
    suspend fun hasSnapshot(): Boolean {
        return mutex.withLock {
            lastSuccessfulSnapshot != null
        }
    }
}

