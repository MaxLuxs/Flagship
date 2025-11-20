package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryCache : FlagsCache {
    private val cache = mutableMapOf<String, ProviderSnapshot>()
    private val mutex = Mutex()

    override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        mutex.withLock {
            cache[providerName] = snapshot
        }
    }

    override suspend fun load(providerName: String): ProviderSnapshot? {
        return mutex.withLock {
            cache[providerName]
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
}

