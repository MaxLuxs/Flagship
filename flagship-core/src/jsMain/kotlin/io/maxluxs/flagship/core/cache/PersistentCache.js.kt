package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer

actual class PersistentCache actual constructor(
    private val serializer: FlagsSerializer
) : FlagsCache {
    // In-memory cache for Node.js (can be extended with filesystem/Redis in production)
    private val cache = js("typeof localStorage !== 'undefined'") as Boolean
        .let { useLocalStorage ->
            if (useLocalStorage) {
                // Browser environment - use localStorage
                BrowserCache()
            } else {
                // Node.js environment - use in-memory cache
                NodeCache()
            }
        }
    
    actual override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        val data = serializer.serialize(snapshot)
        cache.save("flagship_cache_$providerName", data)
    }

    actual override suspend fun load(providerName: String): ProviderSnapshot? {
        val data = cache.load("flagship_cache_$providerName") ?: return null
        return serializer.deserialize(data)
    }

    actual override suspend fun clear(providerName: String) {
        cache.clear("flagship_cache_$providerName")
    }

    actual override suspend fun clearAll() {
        cache.clearAll()
    }
    
    private interface CacheBackend {
        fun save(key: String, value: String)
        fun load(key: String): String?
        fun clear(key: String)
        fun clearAll()
    }
    
    private class BrowserCache : CacheBackend {
        @Suppress("UNUSED_VARIABLE")
        private val localStorage = js("localStorage")
        
        override fun save(key: String, value: String) {
            js("localStorage.setItem(key, value)")
        }
        
        override fun load(key: String): String? {
            val result = js("localStorage.getItem(key)")
            return if (result != null && result != js("undefined")) result as String else null
        }
        
        override fun clear(key: String) {
            js("localStorage.removeItem(key)")
        }
        
        override fun clearAll() {
            val keysToRemove = mutableListOf<String>()
            val length = js("localStorage.length") as Int
            for (i in 0 until length) {
                val key = js("localStorage.key(i)") as? String
                if (key != null && key.startsWith("flagship_cache_")) {
                    keysToRemove.add(key)
                }
            }
            keysToRemove.forEach { js("localStorage.removeItem(it)") }
        }
    }
    
    private class NodeCache : CacheBackend {
        private val storage = mutableMapOf<String, String>()
        
        override fun save(key: String, value: String) {
            storage[key] = value
        }
        
        override fun load(key: String): String? {
            return storage[key]
        }
        
        override fun clear(key: String) {
            storage.remove(key)
        }
        
        override fun clearAll() {
            storage.keys.filter { it.startsWith("flagship_cache_") }
                .forEach { storage.remove(it) }
        }
    }
}

