package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer

/**
 * Helper functions for cache implementations.
 * 
 * Provides common serialization/deserialization logic
 * that can be shared across platform-specific cache implementations.
 */
object CacheHelpers {
    /**
     * Serialize snapshot for storage.
     * 
     * @param snapshot Snapshot to serialize
     * @param serializer Serializer to use
     * @return Serialized string
     */
    fun serialize(snapshot: ProviderSnapshot, serializer: FlagsSerializer): String {
        return serializer.serialize(snapshot)
    }
    
    /**
     * Deserialize snapshot from storage.
     * 
     * @param data Serialized string
     * @param serializer Serializer to use
     * @return Deserialized snapshot or null if deserialization fails
     */
    fun deserialize(data: String, serializer: FlagsSerializer): ProviderSnapshot? {
        return try {
            serializer.deserialize(data)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate cache key for provider.
     * 
     * @param providerName Provider name
     * @return Cache key string
     */
    fun cacheKey(providerName: String): String {
        return "flagship_cache_$providerName"
    }
    
    /**
     * Check if a key is a Flagship cache key.
     * 
     * @param key Key to check
     * @return true if it's a Flagship cache key
     */
    fun isFlagshipCacheKey(key: String): Boolean {
        return key.startsWith("flagship_cache_")
    }
}

